package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalLayers;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PortalRenderer {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    //this WILL be called when rendering portal
    public abstract void onBeforeTranslucentRendering(MatrixStack matrixStack);
    
    //this WILL be called when rendering portal
    public abstract void onAfterTranslucentRendering(MatrixStack matrixStack);
    
    //this WILL be called when rendering portal
    public abstract void onRenderCenterEnded(MatrixStack matrixStack);
    
    //this will NOT be called when rendering portal
    public abstract void prepareRendering();
    
    //this will NOT be called when rendering portal
    public abstract void finishRendering();
    
    //this will be called when rendering portal entities
    public abstract void renderPortalInEntityRenderer(Portal portal);
    
    public abstract boolean shouldSkipClearing();
    
    protected void renderPortals(MatrixStack matrixStack) {
        assert client.cameraEntity.world == client.world;
        assert client.cameraEntity.dimension == client.world.dimension.getType();
        
        List<Portal> portalsNearbySorted = getPortalsNearbySorted();
        
        if (portalsNearbySorted.isEmpty()) {
            return;
        }
        
        Frustum frustum = null;
        if (CGlobal.earlyFrustumCullingPortal) {
            frustum = new Frustum(
                matrixStack.peek().getModel(),
                RenderStates.projectionMatrix
            );
            
            Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
            frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        
        for (Portal portal : portalsNearbySorted) {
            renderPortalIfRoughCheckPassed(portal, matrixStack, frustum);
        }
    }
    
    private void renderPortalIfRoughCheckPassed(
        Portal portal,
        MatrixStack matrixStack,
        Frustum frustum
    ) {
        if (!portal.isPortalValid()) {
            Helper.err("rendering invalid portal " + portal);
            return;
        }
        
        if (RenderStates.getRenderedPortalNum() >= Global.portalRenderLimit) {
            return;
        }
        
        Vec3d thisTickEyePos = client.gameRenderer.getCamera().getPos();
        
        if (!portal.isInFrontOfPortal(thisTickEyePos)) {
            return;
        }
        
        if (PortalLayers.isRendering()) {
            Portal outerPortal = PortalLayers.getRenderingPortal();
            if (Portal.isParallelPortal(portal, outerPortal)) {
                return;
            }
        }
        
        if (isOutOfDistance(portal)) {
            return;
        }
        
        if (CGlobal.earlyFrustumCullingPortal) {
            if (!frustum.isVisible(portal.getBoundingBox())) {
                return;
            }
        }
        
        doRenderPortal(portal, matrixStack);
    }
    
    protected final double getRenderRange() {
        double range = client.options.viewDistance * 16;
        if (PortalLayers.getPortalLayer() > 1) {
            //do not render deep layers of mirror when far away
            range /= (PortalLayers.getPortalLayer());
        }
        if (RenderStates.isLaggy) {
            range = 16;
        }
        return range;
    }
    
    private List<Portal> getPortalsNearbySorted() {
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        return CHelper.getClientNearbyPortals(getRenderRange())
            .sorted(
                Comparator.comparing(portalEntity ->
                    portalEntity.getDistanceToNearestPointInPortal(cameraPos)
                )
            ).collect(Collectors.toList());
    }
    
    protected abstract void doRenderPortal(
        Portal portal,
        MatrixStack matrixStack
    );
    
    protected final void manageCameraAndRenderPortalContent(
        Portal portal
    ) {
        if (PortalLayers.getPortalLayer() > PortalLayers.getMaxPortalLayer()) {
            return;
        }
        
        testNewRenderWorld1(portal);
    }
    
    private void testNewRenderWorld1(Portal portal) {
        Entity cameraEntity = client.cameraEntity;
        
        PortalLayers.onBeginPortalWorldRendering();
        
        Vec3d oldEyePos = McHelper.getEyePos(cameraEntity);
        Vec3d oldLastTickEyePos = McHelper.getLastTickEyePos(cameraEntity);
        
        Vec3d newEyePos = portal.transformPoint(oldEyePos);
        Vec3d newLastTickEyePos = portal.transformPoint(oldLastTickEyePos);
        DimensionType newDimension = portal.dimensionTo;
        ClientWorld newWorld =
            CGlobal.clientWorldLoader.getWorld(newDimension);
        
        Camera camera = client.gameRenderer.getCamera();
        
        assert cameraEntity.world == client.world;
        
        
        DimensionType oldDimension = cameraEntity.dimension;
        ClientWorld oldWorld = ((ClientWorld) cameraEntity.world);
        
        Vec3d oldCameraPos = camera.getPos();
        
        McHelper.setEyePos(cameraEntity, newEyePos, newLastTickEyePos);
        cameraEntity.dimension = newWorld.dimension.getType();
        cameraEntity.world = newWorld;
        client.world = newWorld;
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
        ClientWorld destClientWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        CHelper.checkGlError();
        
        float tickDelta = RenderStates.tickDelta;
        
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ((IEWorldRenderer) worldRenderer).getBuiltChunkStorage().updateCameraPosition(
                MyGameRenderer.client.cameraEntity.getX(),
                MyGameRenderer.client.cameraEntity.getZ()
            );
        }
        
        if (Global.looseVisibleChunkIteration) {
            MyGameRenderer.client.chunkCullingEnabled = false;
        }
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) MyGameRenderer.client.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(
                RenderDimensionRedirect.getRedirectedDimension(destClientWorld.dimension.getType())
            );
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        Camera newCamera = new Camera();
        
        //store old state
        WorldRenderer oldWorldRenderer = MyGameRenderer.client.worldRenderer;
        LightmapTextureManager oldLightmap = MyGameRenderer.client.gameRenderer.getLightmapTextureManager();
        GameMode oldGameMode = playerListEntry.getGameMode();
        boolean oldNoClip = MyGameRenderer.client.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(worldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
        HitResult oldCrosshairTarget = MyGameRenderer.client.crosshairTarget;
        Camera oldCamera = MyGameRenderer.client.gameRenderer.getCamera();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
        
        //switch
        ((IEMinecraftClient) MyGameRenderer.client).setWorldRenderer(worldRenderer);
        MyGameRenderer.client.world = destClientWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        
        BlockEntityRenderDispatcher.INSTANCE.world = destClientWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameMode.SPECTATOR);
        MyGameRenderer.client.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(
            RenderDimensionRedirect.getRedirectedDimension(destClientWorld.dimension.getType())
        );
        ((IEParticleManager) MyGameRenderer.client.particleManager).mySetWorld(destClientWorld);
        if (BlockManipulationClient.remotePointedDim == destClientWorld.dimension.getType()) {
            MyGameRenderer.client.crosshairTarget = BlockManipulationClient.remoteHitResult;
        }
        ieGameRenderer.setCamera(newCamera);
        
        //update lightmap
        if (!RenderStates.isDimensionRendered(destClientWorld.dimension.getType())) {
            helper.lightmapTexture.update(0);
        }
        helper.lightmapTexture.enable();
        
        MyGameRenderer.client.getProfiler().push("render_portal_content");
        
        //invoke rendering
        MyGameRenderer.client.gameRenderer.renderWorld(
            tickDelta,
            Util.getMeasuringTimeNano(),
            new MatrixStack()
        );
        
        MyGameRenderer.client.getProfiler().pop();
        
        //recover
        ((IEMinecraftClient) MyGameRenderer.client).setWorldRenderer(oldWorldRenderer);
        MyGameRenderer.client.world = oldWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        MyGameRenderer.client.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) MyGameRenderer.client.particleManager).mySetWorld(oldWorld);
        MyGameRenderer.client.crosshairTarget = oldCrosshairTarget;
        ieGameRenderer.setCamera(oldCamera);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        
        if (Global.looseVisibleChunkIteration) {
            MyGameRenderer.client.chunkCullingEnabled = true;
        }
        
        MyGameRenderer.client.getEntityRenderManager()
            .configure(
                MyGameRenderer.client.world,
                oldCamera,
                MyGameRenderer.client.targetedEntity
            );
        
        CHelper.checkGlError();
        
        //restore the position
        cameraEntity.dimension = oldDimension;
        cameraEntity.world = oldWorld;
        client.world = oldWorld;
        McHelper.setEyePos(cameraEntity, oldEyePos, oldLastTickEyePos);
        
        GlStateManager.enableDepthTest();
        GlStateManager.disableBlend();
        MyRenderHelper.restoreViewPort();
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
    }
    
    private boolean isOutOfDistance(Portal portal) {
        
        return false;
//        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
//        if (portal.getDistanceToNearestPointInPortal(cameraPos) > getRenderRange()) {
//            return true;
//        }
//
//        if (getPortalLayer() >= 1 &&
//            portal.getDistanceToNearestPointInPortal(cameraPos) >
//                (16 * maxPortalLayer.get())
//        ) {
//            return true;
//        }
//        return false;
    }
    
    protected void renderPortalContentWithContextSwitched(
        Portal portal, Vec3d oldCameraPos, ClientWorld oldWorld
    ) {
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(portal.dimensionTo);
        ClientWorld destClientWorld = CGlobal.clientWorldLoader.getWorld(portal.dimensionTo);
        
        CHelper.checkGlError();
        
        MyGameRenderer.renderWorld(
            worldRenderer, destClientWorld, oldCameraPos, oldWorld
        );
        
        CHelper.checkGlError();
        
    }
    
}
