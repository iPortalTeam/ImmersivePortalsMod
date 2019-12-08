package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.ducks.*;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
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
        Camera camera_1 = this.camera;
        this.viewDistance = (float) (this.client.options.viewDistance * 16);
        MatrixStack matrixStack_2 = new MatrixStack();
        matrixStack_2.peek().getModel().multiply(this.method_22973(camera_1, float_1, true));
        this.bobViewWhenHurt(matrixStack_2, float_1);
        if (this.client.options.bobView) {
            this.bobView(matrixStack_2, float_1);
        }
    
        float float_2 = MathHelper.lerp(
            float_1,
            this.client.player.lastNauseaStrength,
            this.client.player.nextNauseaStrength
        );
        if (float_2 > 0.0F) {
            int int_1 = 20;
            if (this.client.player.hasStatusEffect(StatusEffects.NAUSEA)) {
                int_1 = 7;
            }
        
            float float_3 = 5.0F / (float_2 * float_2 + 5.0F) - float_2 * 0.04F;
            float_3 *= float_3;
            Vector3f vector3f_1 = new Vector3f(
                0.0F,
                MathHelper.SQUARE_ROOT_OF_TWO / 2.0F,
                MathHelper.SQUARE_ROOT_OF_TWO / 2.0F
            );
            matrixStack_2.multiply(vector3f_1.getDegreesQuaternion(((float) this.ticks + float_1) * (float) int_1));
            matrixStack_2.scale(1.0F / float_3, 1.0F, 1.0F);
            float float_4 = -((float) this.ticks + float_1) * (float) int_1;
            matrixStack_2.multiply(vector3f_1.getDegreesQuaternion(float_4));
        }
    
        Matrix4f matrix4f_1 = matrixStack_2.peek().getModel();
        this.method_22709(matrix4f_1);
        camera_1.update(
            this.client.world,
            (Entity) (this.client.getCameraEntity() == null ? this.client.player : this.client.getCameraEntity()),
            this.client.options.perspective > 0,
            this.client.options.perspective == 2,
            float_1
        );
        matrixStack_1.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(camera_1.getPitch()));
        matrixStack_1.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(camera_1.getYaw() + 180.0F));
    
    
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
        GameMode oldGameMode = playerListEntry.getGameMode();
        boolean oldNoClip = mc.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        List oldChunkInfos = ((IEWorldRenderer) mc.worldRenderer).getChunkInfos();
        IEChunkRenderList oldChunkRenderList =
            (IEChunkRenderList) ((IEWorldRenderer) oldWorldRenderer).getChunkRenderList();
    
    
        OFInterface.createNewRenderInfosNormal.accept((IEOFWorldRenderer) newWorldRenderer);
    
        //switch
        ((IEMinecraftClient) mc).setWorldRenderer(newWorldRenderer);
        mc.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        helper.lightmapTexture.update(0);
        helper.lightmapTexture.enable();
        BlockEntityRenderDispatcher.INSTANCE.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameMode.SPECTATOR);
        mc.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(newWorld.dimension.getType());
    
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
        newWorldRenderer.render(
            matrixStack,
            partialTicks,
            getChunkUpdateFinishTime(),
            false,//should render block outline
            mc.gameRenderer.getCamera(),
            helper.lightmapTexture,
        
            );
        OFInterface.afterRenderCenter.run();
        
        mc.getProfiler().pop();
    
        //recover
        ((IEMinecraftClient) mc).setWorldRenderer(oldWorldRenderer);
        mc.world = oldWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        mc.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        GlStateManager.enableBlend();
        ((IEWorldRenderer) mc.worldRenderer).setChunkInfos(oldChunkInfos);
        FogRendererContext.swappingManager.popSwapping();
    
    
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
    
    
    public void renderPlayerItself(Runnable doRenderEntity) {
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
            player, RenderHelper.originalPlayerPos, RenderHelper.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        doRenderEntity.run();
        
        Helper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
}
