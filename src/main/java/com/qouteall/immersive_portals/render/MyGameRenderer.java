package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.exposer.IEChunkRenderList;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.exposer.IEPlayerListEntry;
import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.IEOFWorldRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
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
import net.optifine.shaders.Shaders;
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
    
        if (CGlobal.isOptifinePresent) {
            /**{@link WorldRenderer#chunkInfos}*/
            //in vanilla it will create new chunkInfos object every frame
            //but with optifine it will always use one object
            //we need to switch chunkInfos correctly
            //if we do not put it a new object, it will clear the original chunkInfos
            ((IEOFWorldRenderer) newWorldRenderer).createNewRenderInfosNormal();
        }
        
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
        if (OFHelper.getIsUsingShader()) {
            Shaders.activeProgram = Shaders.ProgramNone;
            Shaders.beginRender(mc, mc.gameRenderer.getCamera(), partialTicks, 0);
        }
        ieGameRenderer.renderCenter_(partialTicks, getChunkUpdateFinishTime());
        if (OFHelper.getIsUsingShader()) {
            Shaders.activeProgram = Shaders.ProgramNone;
        }
        
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
        // oldChunkRenderList.setChunkRenderers(oldChunkRenderers);
        
    }
    
    public void endCulling() {
        GL11.glDisable(GL11.GL_CLIP_PLANE0);
    }
    
    public void startCulling() {
        //shaders does not compatible with glCullPlane
        //I have to modify shader code
        if (CGlobal.useFrontCulling && !OFHelper.getIsUsingShader()) {
            GL11.glEnable(GL11.GL_CLIP_PLANE0);
        }
    }
    
    //NOTE the actual culling plane is related to current model view matrix
    public void updateCullingPlane() {
        clipPlaneEquation = calcClipPlaneEquation();
        if (!OFHelper.getIsUsingShader()) {
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
    
        //glClipPlane is correlated with modelview transformation
        //but it's not in shaders
        Vec3d planeNormal = portal.getNormal().multiply(-1);
        
        Vec3d portalPos = portal.getPos().subtract(
            mc.gameRenderer.getCamera().getPos()
        );
        
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
        Vec3d oldLastTickPos = Helper.lastTickPosOf(player);
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
