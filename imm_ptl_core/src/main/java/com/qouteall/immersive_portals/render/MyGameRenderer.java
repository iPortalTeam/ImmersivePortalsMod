package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.SodiumInterface;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import com.qouteall.immersive_portals.render.context_management.RenderingHierarchy;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class MyGameRenderer {
    public static MinecraftClient client = MinecraftClient.getInstance();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    // portal rendering and outer world rendering uses different buffer builder storages
    // theoretically every layer of portal rendering should have its own buffer builder storage
    // but it's more complex
    private static BufferBuilderStorage secondaryBufferBuilderStorage = new BufferBuilderStorage();
    
    public static void renderWorldNew(
        RenderingHierarchy renderingHierarchy,
        Consumer<Runnable> invokeWrapper
    ) {
        RenderingHierarchy.pushRenderInfo(renderingHierarchy);
        
        switchAndRenderTheWorld(
            renderingHierarchy.world,
            renderingHierarchy.cameraPos,
            renderingHierarchy.cameraPos,
            invokeWrapper,
            renderingHierarchy.renderDistance
        );
        
        RenderingHierarchy.popRenderInfo();
    }
    
    private static void switchAndRenderTheWorld(
        ClientWorld newWorld,
        Vec3d thisTickCameraPos,
        Vec3d lastTickCameraPos,
        Consumer<Runnable> invokeWrapper,
        int renderDistance
    ) {
        resetGlStates();
        
        Entity cameraEntity = client.cameraEntity;
        
        Vec3d oldEyePos = McHelper.getEyePos(cameraEntity);
        Vec3d oldLastTickEyePos = McHelper.getLastTickEyePos(cameraEntity);
        
        RegistryKey<World> oldEntityDimension = cameraEntity.world.getRegistryKey();
        ClientWorld oldEntityWorld = ((ClientWorld) cameraEntity.world);
        
        RegistryKey<World> newDimension = newWorld.getRegistryKey();
        
        //switch the camera entity pos
        McHelper.setEyePos(cameraEntity, thisTickCameraPos, lastTickCameraPos);
        cameraEntity.world = newWorld;
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(newDimension);
        
        CHelper.checkGlError();
        
        float tickDelta = RenderStates.tickDelta;
        
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ((IEWorldRenderer) worldRenderer).getBuiltChunkStorage().updateCameraPosition(
                cameraEntity.getX(),
                cameraEntity.getZ()
            );
        }
        
        if (Global.looseVisibleChunkIteration) {
            client.chunkCullingEnabled = false;
        }
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(
                RenderDimensionRedirect.getRedirectedDimension(newDimension)
            );
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        Camera newCamera = new Camera();
        
        //store old state
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        LightmapTextureManager oldLightmap = client.gameRenderer.getLightmapTextureManager();
        GameMode oldGameMode = playerListEntry.getGameMode();
        boolean oldNoClip = client.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(worldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
        HitResult oldCrosshairTarget = client.crosshairTarget;
        Camera oldCamera = client.gameRenderer.getCamera();
        ShaderEffect oldTransparencyShader =
            ((IEWorldRenderer) oldWorldRenderer).portal_getTransparencyShader();
        ShaderEffect newTransparencyShader = ((IEWorldRenderer) worldRenderer).portal_getTransparencyShader();
        BufferBuilderStorage oldBufferBuilder = ((IEWorldRenderer) worldRenderer).getBufferBuilderStorage();
        BufferBuilderStorage oldClientBufferBuilder = client.getBufferBuilders();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
        
        int oldRenderDistance = ((IEWorldRenderer) worldRenderer).portal_getRenderDistance();
        
        //switch
        ((IEMinecraftClient) client).setWorldRenderer(worldRenderer);
        client.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        
        BlockEntityRenderDispatcher.INSTANCE.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameMode.SPECTATOR);
        client.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(
            RenderDimensionRedirect.getRedirectedDimension(newDimension)
        );
        ((IEParticleManager) client.particleManager).mySetWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newDimension) {
            client.crosshairTarget = BlockManipulationClient.remoteHitResult;
        }
        ieGameRenderer.setCamera(newCamera);
        
        if (Global.useSecondaryEntityVertexConsumer) {
            ((IEWorldRenderer) worldRenderer).setBufferBuilderStorage(secondaryBufferBuilderStorage);
            ((IEMinecraftClient) client).setBufferBuilderStorage(secondaryBufferBuilderStorage);
        }
        
        Object newSodiumContext = SodiumInterface.createNewRenderingContext.apply(worldRenderer);
        Object oldSodiumContext = SodiumInterface.switchRenderingContext.apply(worldRenderer, newSodiumContext);
        
        ((IEWorldRenderer) oldWorldRenderer).portal_setTransparencyShader(null);
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(null);
        ((IEWorldRenderer) worldRenderer).portal_setRenderDistance(renderDistance);
        
        //update lightmap
        if (!RenderStates.isDimensionRendered(newDimension)) {
            helper.lightmapTexture.update(0);
        }
        
        //invoke rendering
        try {
            invokeWrapper.accept(() -> {
                client.getProfiler().push("render_portal_content");
                client.gameRenderer.renderWorld(
                    tickDelta,
                    Util.getMeasuringTimeNano(),
                    new MatrixStack()
                );
                client.getProfiler().pop();
            });
        }
        catch (Throwable e) {
            limitedLogger.invoke(e::printStackTrace);
        }
        
        //recover
        SodiumInterface.switchRenderingContext.apply(worldRenderer, oldSodiumContext);
        
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        client.world = oldEntityWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        BlockEntityRenderDispatcher.INSTANCE.world = oldEntityWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        client.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) client.particleManager).mySetWorld(oldEntityWorld);
        client.crosshairTarget = oldCrosshairTarget;
        ieGameRenderer.setCamera(oldCamera);
        
        ((IEWorldRenderer) oldWorldRenderer).portal_setTransparencyShader(oldTransparencyShader);
        ((IEWorldRenderer) worldRenderer).portal_setTransparencyShader(newTransparencyShader);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        
        ((IEWorldRenderer) worldRenderer).setBufferBuilderStorage(oldBufferBuilder);
        ((IEMinecraftClient) client).setBufferBuilderStorage(oldClientBufferBuilder);
        
        ((IEWorldRenderer) worldRenderer).portal_setRenderDistance(oldRenderDistance);
        
        if (Global.looseVisibleChunkIteration) {
            client.chunkCullingEnabled = true;
        }
        
        client.getEntityRenderDispatcher()
            .configure(
                client.world,
                oldCamera,
                client.targetedEntity
            );
        
        CHelper.checkGlError();
        
        //restore the camera entity pos
        cameraEntity.world = oldEntityWorld;
        McHelper.setEyePos(cameraEntity, oldEyePos, oldLastTickEyePos);
        
        resetGlStates();
    }
    
    /**
     * For example the Cull assumes that the culling is enabled before using it
     * {@link net.minecraft.client.render.RenderPhase.Cull}
     */
    public static void resetGlStates() {
        GlStateManager.disableAlphaTest();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        net.minecraft.client.render.DiffuseLighting.disable();
        MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().disable();
        client.gameRenderer.getOverlayTexture().teardownOverlayColor();
    }
    
    public static void renderPlayerItself(Runnable doRenderEntity) {
        EntityRenderDispatcher entityRenderDispatcher =
            ((IEWorldRenderer) client.worldRenderer).getEntityRenderDispatcher();
        PlayerListEntry playerListEntry = CHelper.getClientPlayerListEntry();
        GameMode originalGameMode = RenderStates.originalGameMode;
        
        Entity player = client.cameraEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPos();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameMode oldGameMode = playerListEntry.getGameMode();
        
        McHelper.setPosAndLastTickPos(
            player, RenderStates.originalPlayerPos, RenderStates.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        doRenderEntity.run();
        
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
    
    public static void resetFogState() {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            return;
        }
        
        if (OFInterface.isShaders.getAsBoolean()) {
            return;
        }
        
        forceResetFogState();
    }
    
    public static void forceResetFogState() {
        Camera camera = client.gameRenderer.getCamera();
        float g = client.gameRenderer.getViewDistance();
        
        Vec3d cameraPos = camera.getPos();
        double d = cameraPos.getX();
        double e = cameraPos.getY();
        double f = cameraPos.getZ();
        
        boolean bl2 = client.world.getSkyProperties().useThickFog(MathHelper.floor(d), MathHelper.floor(e)) ||
            client.inGameHud.getBossBarHud().shouldThickenFog();
        
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(g - 16.0F, 32.0F), bl2);
        BackgroundRenderer.setFogBlack();
    }
    
    public static void updateFogColor() {
        BackgroundRenderer.render(
            client.gameRenderer.getCamera(),
            RenderStates.tickDelta,
            client.world,
            client.options.viewDistance,
            client.gameRenderer.getSkyDarkness(RenderStates.tickDelta)
        );
    }
    
    public static void resetDiffuseLighting(MatrixStack matrixStack) {
        DiffuseLighting.enableForLevel(matrixStack.peek().getModel());
    }
    
    public static void pruneRenderList(ObjectList<?> visibleChunks) {
        if (PortalRendering.isRendering()) {
            if (Global.cullSectionsBehind) {
                // this thing has no optimization effect -_-
                
                PortalLike renderingPortal = PortalRendering.getRenderingPortal();
    
                if (renderingPortal instanceof Portal) {
                    int firstInsideOne = Helper.indexOf(
                        visibleChunks,
                        obj -> {
                            ChunkBuilder.BuiltChunk builtChunk =
                                ((IEWorldRendererChunkInfo) obj).getBuiltChunk();
                            Box boundingBox = builtChunk.boundingBox;
            
                            return FrustumCuller.isTouchingInsideContentArea(
                                ((Portal) renderingPortal), boundingBox
                            );
                        }
                    );
    
                    if (firstInsideOne != -1) {
                        visibleChunks.removeElements(0, firstInsideOne);
                    }
                    else {
                        visibleChunks.clear();
                    }
                }
            }
        }
    }
    
    public static void renderWorldInfoFramebuffer(
        RenderingHierarchy renderingHierarchy,
        Framebuffer framebuffer
    ) {
        CHelper.checkGlError();
        
        Framebuffer mcFb = client.getFramebuffer();
        
        Validate.isTrue(mcFb != framebuffer);
        
        ((IEMinecraftClient) client).setFrameBuffer(framebuffer);
        
        framebuffer.beginWrite(true);
        
        CGlobal.renderer.invokeWorldRendering(renderingHierarchy);
        
        ((IEMinecraftClient) client).setFrameBuffer(mcFb);
        
        mcFb.beginWrite(true);
        
        CHelper.checkGlError();
    }
    
}
