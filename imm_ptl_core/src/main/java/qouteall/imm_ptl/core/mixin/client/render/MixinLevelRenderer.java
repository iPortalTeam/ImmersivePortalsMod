package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyBuiltChunkStorage;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

@Mixin(value = LevelRenderer.class)
public abstract class MixinLevelRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientLevel level;
    
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Shadow
    private ViewArea viewArea;
    
    @Shadow
    protected abstract void renderEntity(
        Entity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        PoseStack matrixStack_1,
        MultiBufferSource vertexConsumerProvider_1
    );
    
    @Shadow
    private ChunkRenderDispatcher chunkRenderDispatcher;
    
    @Shadow
    private PostChain transparencyChain;
    
    @Mutable
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    
    @Shadow
    private int lastViewDistance;
    
    @Shadow
    @Nullable
    private RenderTarget translucentTarget;
    
    @Shadow
    private Frustum cullingFrustum;
    
    @Shadow
    @Nullable
    private VertexBuffer starBuffer;
    
    @Shadow
    @Nullable
    private VertexBuffer skyBuffer;
    
    @Shadow
    @Nullable
    private VertexBuffer darkBuffer;
    
    @Shadow
    @Nullable
    private VertexBuffer cloudBuffer;
    
    @Mutable
    @Shadow
    @Final
    private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;
    
    @Shadow
    protected abstract void deinitTransparency();
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z"
        )
    )
    private void onAfterCutoutRendering(
        PoseStack matrices, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightTexture lightmapTextureManager, Matrix4f matrix4f,
        CallbackInfo ci
    ) {
//        IPCGlobal.renderer.onBeforeTranslucentRendering(matrices);
        
        CrossPortalEntityRenderer.onBeginRenderingEntities(matrices);
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private void onMyBeforeTranslucentRendering(
        PoseStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.onBeforeTranslucentRendering(matrices);
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
        
        MyGameRenderer.resetDiffuseLighting(matrices);
        
        FrontClipping.disableClipping();
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endLastBatch()V"
        )
    )
    private void onEndRenderingEntities(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        CrossPortalEntityRenderer.onEndRenderingEntities(poseStack);
    }
    
    @Inject(
        method = "renderLevel",
        at = @At("RETURN")
    )
    private void onAfterTranslucentRendering(
        PoseStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        
        IPCGlobal.renderer.onAfterTranslucentRendering(matrices);
        
        //make hand rendering normal
        Lighting.setupLevel(matrices.last().pose());
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V"
        )
    )
    private void onBeforeRenderingLayer(
        PoseStack matrices, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightTexture lightmapTextureManager, Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getActiveClippingPlane(),
                matrices.last().pose(),
                -FrontClipping.ADJUSTMENT
                // move the clipping plane a little back, to make world wrapping portal not z-fight
            );
            
            if (PortalRendering.isRenderingOddNumberOfMirrors()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
            
            if (IPGlobal.enableDepthClampForPortalRendering) {
                CHelper.enableDepthClamp();
            }
        }
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack;DDDLorg/joml/Matrix4f;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingLayer(
        PoseStack matrices, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightTexture lightmapTextureManager, Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
            MyRenderHelper.recoverFaceCulling();
            
            if (IPGlobal.enableDepthClampForPortalRendering) {
                CHelper.disableDepthClamp();
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetupTerrainBegin(
        Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator,
        CallbackInfo ci
    ) {
        if (WorldRenderInfo.isRendering()) {
            if (level.dimension() != RenderStates.originalPlayerDimension) {
                chunkRenderDispatcher.setCamera(camera.getPosition());
            }
        }
        
        if (ip_allowOverrideTerrainSetup()) {
            if (WorldRenderInfo.isRendering()) {
                level.getProfiler().push("ip_terrain_setup");
                VisibleSectionDiscovery.discoverVisibleSections(
                    level, ((MyBuiltChunkStorage) viewArea),
                    camera,
                    new Frustum(frustum).offsetToFullyIncludeCameraCube(8),
                    renderChunksInFrustum
                );
                level.getProfiler().pop();
                
                ci.cancel();
            }
        }
    }
    
    private boolean ip_allowOverrideTerrainSetup() {
        return !SodiumInterface.invoker.isSodiumPresent()
            && !IrisInterface.invoker.isRenderingShadowMap();
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetupTerrainEnd(
        Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator,
        CallbackInfo ci
    ) {
        if (!WorldRenderInfo.isRendering()) {
            if (ip_allowOverrideTerrainSetup()) {
                if (MyGameRenderer.vanillaTerrainSetupOverride > 0) {
                    MyGameRenderer.vanillaTerrainSetupOverride--;
                    
                    level.getProfiler().push("ip_terrain_setup");
                    VisibleSectionDiscovery.discoverVisibleSections(
                        level, ((MyBuiltChunkStorage) viewArea),
                        camera,
                        new Frustum(frustum).offsetToFullyIncludeCameraCube(8),
                        renderChunksInFrustum
                    );
                    level.getProfiler().pop();
                }
                else if (IPGlobal.alwaysOverrideTerrainSetup) {
                    // debug
                    level.getProfiler().push("ip_terrain_setup_debug");
                    VisibleSectionDiscovery.discoverVisibleSections(
                        level, ((MyBuiltChunkStorage) viewArea),
                        camera,
                        new Frustum(frustum).offsetToFullyIncludeCameraCube(8),
                        renderChunksInFrustum
                    );
                    level.getProfiler().pop();
                }
            }
        }
    }
    
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V",
            remap = false
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!IPCGlobal.renderer.replaceFrameBufferClearing()) {
            RenderSystem.clear(int_1, boolean_1);
        }
    }
    
    @Redirect(
        method = "allChanged",
        at = @At(
            value = "NEW",
            target = "net/minecraft/client/renderer/ViewArea"
        )
    )
    private ViewArea redirectConstructingBuildChunkStorage(
        ChunkRenderDispatcher chunkBuilder_1,
        Level world_1,
        int int_1,
        LevelRenderer worldRenderer_1
    ) {
        if (IPCGlobal.useHackedChunkRenderDispatcher) {
            return new MyBuiltChunkStorage(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
        else {
            return new ViewArea(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
    }
    
    // @Inject does not allow getting the entity reference
    // maybe needs Mixin Extra
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"
        )
    )
    private void redirectRenderEntity(
        LevelRenderer worldRenderer,
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
        renderEntity(
            entity,
            cameraX, cameraY, cameraZ,
            tickDelta,
            matrixStack, vertexConsumerProvider
        );
        CrossPortalEntityRenderer.afterRenderingEntity(entity);
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"
        )
    )
    private void beforeRenderingWeather(
        PoseStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getActiveClippingPlane(),
                matrices.last().pose(), 0
            );
            RenderStates.isRenderingPortalWeather = true;
        }
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V",
            shift = At.Shift.AFTER
        )
    )
    private void afterRenderingWeather(
        PoseStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
            RenderStates.isRenderingPortalWeather = false;
        }
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean redirectGlowing(Minecraft client, Entity entity) {
        if (WorldRenderInfo.isRendering()) {
            return false;
        }
        return client.shouldEntityAppearGlowing(entity);
    }
    
    // sometimes we change renderDistance but we don't want to reload it
    @Inject(method = "allChanged", at = @At("HEAD"), cancellable = true)
    private void onReloadStarted(CallbackInfo ci) {
        if (WorldRenderInfo.isRendering()) {
            Helper.log("world renderer reloading cancelled during portal rendering");
            ci.cancel();
        }
    }
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "allChanged", at = @At("TAIL"))
    private void onReloadFinished(CallbackInfo ci) {
        LevelRenderer this_ = (LevelRenderer) (Object) this;
        
        if (ClientWorldLoader.getIsCreatingClientWorld()) {
            return;
        }
        
        Validate.isTrue(Minecraft.getInstance().levelRenderer == this_);
        
        ClientWorldLoader._onWorldRendererReloaded();
    }
    
    @Inject(
        method = "renderSky", at = @At("HEAD"), cancellable = true
    )
    private void onRenderSkyBegin(
        PoseStack poseStack, Matrix4f matrix4f, float partialTick, Camera camera,
        boolean isFoggy, Runnable runnable, CallbackInfo ci
    ) {
        if (WorldRenderInfo.isRendering()) {
            if (!WorldRenderInfo.getTopRenderInfo().doRenderSky) {
                if (!IrisInterface.invoker.isShaders()) {
                    ci.cancel();
                }
            }
        }
        
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
    }
    
    @Inject(
        method = "renderSky",
        at = @At("RETURN")
    )
    private void onRenderSkyEnd(PoseStack poseStack, Matrix4f matrix4f, float f, Camera camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        MyRenderHelper.recoverFaceCulling();
    }
    
    // correct the eye position for sky rendering
    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 redirectGetEyePositionInSkyRendering(LocalPlayer player, float partialTicks) {
        if (WorldRenderInfo.isRendering()) {
            return WorldRenderInfo.getCameraPos();
        }
        return player.getEyePosition(partialTicks);
    }
    
    @Inject(
        method = "prepareCullFrustum", at = @At("HEAD")
    )
    private void onSetupFrustum(PoseStack matrices, Vec3 pos, Matrix4f projectionMatrix, CallbackInfo ci) {
        TransformationManager.processTransformation(minecraft.gameRenderer.getMainCamera(), matrices);
    }
    
    // vanilla clears translucentFramebuffer even when transparencyShader is null
    // it makes the framebuffer to be wrongly bound in fabulous mode
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;translucentTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    private RenderTarget redirectTranslucentFramebuffer(LevelRenderer this_) {
        if (PortalRendering.isRendering()) {
            return null;
        }
        else {
            return translucentTarget;
        }
    }
    
    // if not in spectator mode, when the camera is in block chunk culling will cull chunks wrongly
    @ModifyVariable(
        method = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1
    )
    private boolean modifyIsSpectator(boolean value) {
        if (WorldRenderInfo.isRendering()) {
            return true;
        }
        return value;
    }
    
    // the captured lambda uses the net handler's world field
    // so switch that correctly
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;pollLightUpdates()V"
        )
    )
    private void redirectRunQueuedChunkUpdates(ClientLevel world) {
        ClientWorldLoader.withSwitchedWorld(
            world, world::pollLightUpdates
        );
    }
    
    /**
     * when rendering portal, it won't call {@link ViewArea#repositionCamera(double, double)}
     * So {@link ViewArea#getRenderChunkAt(BlockPos)} will return incorrect result
     */
    @Inject(
        method = "isChunkCompiled",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsChunkCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (PortalRendering.isRendering()) {
            if (!SodiumInterface.invoker.isSodiumPresent()) {
                if (viewArea instanceof MyBuiltChunkStorage myBuiltChunkStorage) {
                    cir.setReturnValue(ip_isChunkCompiled(myBuiltChunkStorage, blockPos));
                }
            }
        }
    }
    
    private boolean ip_isChunkCompiled(MyBuiltChunkStorage myBuiltChunkStorage, BlockPos blockPos) {
        SectionPos sectionPos = SectionPos.of(blockPos);
        ChunkRenderDispatcher.RenderChunk renderChunk = myBuiltChunkStorage.rawGet(
            sectionPos.x(), sectionPos.y(), sectionPos.z()
        );
        
        return renderChunk != null
            && renderChunk.compiled.get() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
    }
    
    @Override
    public EntityRenderDispatcher ip_getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }
    
    @Override
    public ViewArea ip_getBuiltChunkStorage() {
        return viewArea;
    }
    
    @Override
    public ChunkRenderDispatcher getChunkBuilder() {
        return chunkRenderDispatcher;
    }
    
    @Override
    public void ip_myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        renderEntity(
            entity, cameraX, cameraY, cameraZ, tickDelta, matrixStack, vertexConsumerProvider
        );
    }
    
    @Override
    public PostChain portal_getTransparencyShader() {
        return transparencyChain;
    }
    
    @Override
    public void portal_setTransparencyShader(PostChain arg) {
        transparencyChain = arg;
    }
    
    @Override
    public RenderBuffers ip_getRenderBuffers() {
        return renderBuffers;
    }
    
    @Override
    public void ip_setRenderBuffers(RenderBuffers arg) {
        renderBuffers = arg;
    }
    
    @Override
    public Frustum portal_getFrustum() {
        return cullingFrustum;
    }
    
    @Override
    public void portal_setFrustum(Frustum arg) {
        cullingFrustum = arg;
    }
    
    @Override
    public void portal_fullyDispose() {
        deinitTransparency();
        
        if (starBuffer != null) {
            starBuffer.close();
        }
        if (skyBuffer != null) {
            skyBuffer.close();
        }
        if (darkBuffer != null) {
            darkBuffer.close();
        }
        if (cloudBuffer != null) {
            cloudBuffer.close();
        }
        
        level = null;
    }
    
    @Override
    public void portal_setChunkInfoList(ObjectArrayList<LevelRenderer.RenderChunkInfo> arg) {
        renderChunksInFrustum = arg;
    }
    
    @Override
    public ObjectArrayList<LevelRenderer.RenderChunkInfo> portal_getChunkInfoList() {
        return renderChunksInFrustum;
    }
}
