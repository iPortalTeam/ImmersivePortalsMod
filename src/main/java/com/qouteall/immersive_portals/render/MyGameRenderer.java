package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.ducks.IEChunkRenderList;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class MyGameRenderer {
    private MinecraftClient mc = MinecraftClient.getInstance();
    private double[] clipPlaneEquation;
    
    public MyGameRenderer() {
    
    }
    
    public void renderWorld(
        float partialTicks,
        WorldRenderer newWorldRenderer,
        ClientWorld newWorld,
        Vec3d oldCameraPos
    ) {
        ChunkRenderDispatcher chunkRenderDispatcher =
            ((IEWorldRenderer) newWorldRenderer).getChunkRenderDispatcher();
        chunkRenderDispatcher.updateCameraPosition(
            mc.player.x, mc.player.z
        );
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) mc.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(newWorld.dimension.getType());
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        Camera newCamera = new Camera();
    
        //store old state
        WorldRenderer oldWorldRenderer = mc.worldRenderer;
        ClientWorld oldWorld = mc.world;
        LightmapTextureManager oldLightmap = ieGameRenderer.getLightmapTextureManager();
        BackgroundRenderer oldFogRenderer = ieGameRenderer.getBackgroundRenderer();
        GameMode oldGameMode = playerListEntry.getGameMode();
        boolean oldNoClip = mc.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        List oldChunkInfos = ((IEWorldRenderer) mc.worldRenderer).getChunkInfos();
        IEChunkRenderList oldChunkRenderList =
            (IEChunkRenderList) ((IEWorldRenderer) oldWorldRenderer).getChunkRenderList();
        //List<ChunkRenderer> oldChunkRenderers = oldChunkRenderList.getChunkRenderers();
    
    
        OFInterface.createNewRenderInfosNormal.accept((IEOFWorldRenderer) newWorldRenderer);
    
        //switch
        mc.worldRenderer = newWorldRenderer;
        mc.world = newWorld;
        ieGameRenderer.setBackgroundRenderer(helper.fogRenderer);
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.update(0);
        helper.lightmapTexture.enable();
        BlockEntityRenderDispatcher.INSTANCE.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameMode.SPECTATOR);
        mc.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
    
        CGlobal.renderInfoNumMap.put(
            newWorld.dimension.getType(),
            ((IEWorldRenderer) mc.worldRenderer).getChunkInfos().size()
        );
    
        updateCullingPlane();
        
        //this is important
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GuiLighting.disable();
        ((GameRenderer) ieGameRenderer).disableLightmap();
        
        mc.getProfiler().push("render_portal_content");
    
        CGlobal.switchedFogRenderer = ieGameRenderer.getBackgroundRenderer();
        
        //invoke it!
        OFInterface.beforeRenderCenter.accept(partialTicks);
        ieGameRenderer.renderCenter_(partialTicks, getChunkUpdateFinishTime());
        OFInterface.afterRenderCenter.run();
        
        mc.getProfiler().pop();
    
        //recover
        mc.worldRenderer = oldWorldRenderer;
        mc.world = oldWorld;
        ieGameRenderer.setBackgroundRenderer(oldFogRenderer);
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        mc.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        ((IEWorldRenderer) mc.worldRenderer).setChunkInfos(oldChunkInfos);
    
    
        oldChunkRenderList.setCameraPos(oldCameraPos.x, oldCameraPos.y, oldCameraPos.z);
        
    }
    
    public void endCulling() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
    }
    
    public void startCulling() {
        //shaders does not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !OFInterface.isShaders.getAsBoolean()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public void updateCullingPlane() {
        clipPlaneEquation = calcClipPlaneEquation();
        if (!OFInterface.isShaders.getAsBoolean()) {
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0, clipPlaneEquation);
        }
    }
    
    private long getChunkUpdateFinishTime() {
        return 0;
    }
    
    //invoke this before rendering portal
    //its result depends on camra pos
    private double[] calcClipPlaneEquation() {
        Portal portal = CGlobal.renderer.getRenderingPortal();
    
        Vec3d planeNormal = portal.getNormal().multiply(-1);
    
        Vec3d portalPos = portal.getPos()
            .subtract(portal.getNormal().multiply(-0.01))//avoid z fighting
            .subtract(mc.gameRenderer.getCamera().getPos());
    
        if (OFInterface.isShaders.getAsBoolean() && portal instanceof Mirror) {
            planeNormal = planeNormal.multiply(-1);
        }
        
        //equation: planeNormal * p + c > 0
        //-planeNormal * portalCenter = c
        double c = planeNormal.multiply(-1).dotProduct(portalPos);
        
        return new double[]{
            planeNormal.x,
            planeNormal.y,
            planeNormal.z,
            c
        };
    }
    
    public double[] getClipPlaneEquation() {
        return clipPlaneEquation;
    }
    
    public void renderPlayerItselfIfNecessary() {
        if (CGlobal.renderer.shouldRenderPlayerItself()) {
            renderPlayerItself(
                RenderHelper.originalPlayerPos,
                RenderHelper.originalPlayerLastTickPos,
                RenderHelper.partialTicks
            );
        }
    }
    
    private void renderPlayerItself(Vec3d playerPos, Vec3d playerLastTickPos, float patialTicks) {
        EntityRenderDispatcher entityRenderDispatcher =
            ((IEWorldRenderer) mc.worldRenderer).getEntityRenderDispatcher();
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        GameMode originalGameMode = RenderHelper.originalGameMode;
        
        Entity player = mc.cameraEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPos();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameMode oldGameMode = playerListEntry.getGameMode();
        
        Helper.setPosAndLastTickPos(
            player, playerPos, playerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        entityRenderDispatcher.render(player, patialTicks, false);
        
        Helper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
}
