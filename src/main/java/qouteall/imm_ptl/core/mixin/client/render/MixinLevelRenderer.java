package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.textures.GpuSampler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.IrisCompat;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.render.CrossPortalEntityRenderer;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.ImmPtlViewArea;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.VisibleSectionDiscovery;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;

@SuppressWarnings("JavadocReference")
@Mixin(value = LevelRenderer.class)
public abstract class MixinLevelRenderer implements IEWorldRenderer {
    private static final ThreadLocal<Entity> ip$currentRenderedEntity = new ThreadLocal<>();
    @Unique
    private Matrix4f ip$layerRenderModelView;
    @Unique
    private boolean ip$transparencyChainOverrideSet;
    @Unique
    private PostChain ip$transparencyChainOverride;
    
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
    @Final
    private LevelRenderState levelRenderState;

    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Invoker("getTransparencyChain")
    protected abstract PostChain ip$getTransparencyChain();
    
    @Mutable
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    
    @Shadow
    private int lastViewDistance;
    
    @Shadow
    public abstract RenderTarget getTranslucentTarget();
    
    @Invoker("applyFrustum")
    protected abstract void ip$applyFrustum(Frustum frustum);

    @Shadow
    public abstract Frustum getCapturedFrustum();

    @Shadow
    public abstract void killFrustum();
    
    @Shadow
    public abstract void close();
    
    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;
    
    @Shadow
    @Final
    @Mutable
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Shadow
    protected abstract EntityRenderState extractEntity(Entity entity, float partialTick);
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z"
        ),
        require = 0,
        expect = 0
    )
    private void onAfterCutoutRendering(
        GraphicsResourceAllocator graphicsResourceAllocator,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        Matrix4f modelView,
        Matrix4f matrix4f2,
        Matrix4f matrix4f3,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean bl2,
        CallbackInfo ci
    ) {
//        IPCGlobal.renderer.onBeforeTranslucentRendering(matrices);
        
        CrossPortalEntityRenderer.onBeginRenderingEntitiesAndBlockEntities(modelView);
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/rendertype/RenderType;"
        ),
        require = 0,
        expect = 0
    )
    private void onMyBeforeTranslucentRendering(
        GraphicsResourceAllocator graphicsResourceAllocator,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        Matrix4f modelView,
        Matrix4f matrix4f2,
        Matrix4f matrix4f3,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean bl2,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.onBeforeTranslucentRendering(modelView);
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
        
        MyGameRenderer.resetDiffuseLighting();
        
        FrontClipping.disableClipping();
    }

    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getTransparencyChain()Lnet/minecraft/client/renderer/PostChain;"
        )
    )
    private PostChain ip$redirectTransparencyChain(LevelRenderer instance) {
        if (ip$transparencyChainOverrideSet) {
            return ip$transparencyChainOverride;
        }
        return ip$getTransparencyChain();
    }
    
    @IPVanillaCopy
    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void onEndRenderingEntities(
        PoseStack poseStack,
        LevelRenderState levelRenderState,
        SubmitNodeStorage submitNodeStorage,
        CallbackInfo ci
    ) {
        CrossPortalEntityRenderer.onEndRenderingEntitiesAndBlockEntities(poseStack);
    }
    
    @Inject(
        method = "renderLevel",
        at = @At("RETURN")
    )
    private void onAfterTranslucentRendering(
        GraphicsResourceAllocator graphicsResourceAllocator,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        Matrix4f modelView,
        Matrix4f matrix4f2,
        Matrix4f matrix4f3,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean bl2,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.onAfterTranslucentRendering(modelView);
        
        // make hand rendering normal
        minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
    }
    
    @Inject(
        method = "method_62214",
        at = @At("HEAD")
    )
    private void ip$cacheLayerRenderModelView(
        GpuBufferSlice fogBuffer,
        LevelRenderState levelRenderState,
        net.minecraft.util.profiling.ProfilerFiller profilerFiller,
        Matrix4f modelView,
        com.mojang.blaze3d.resource.ResourceHandle<?> mainTarget,
        com.mojang.blaze3d.resource.ResourceHandle<?> translucentTarget,
        boolean renderBlockOutline,
        com.mojang.blaze3d.resource.ResourceHandle<?> entityOutlineTarget,
        com.mojang.blaze3d.resource.ResourceHandle<?> entityOutlineDepth,
        CallbackInfo ci
    ) {
        ip$layerRenderModelView = modelView;
    }

    @Inject(
        method = "method_62214",
        at = @At("RETURN")
    )
    private void ip$clearLayerRenderModelView(
        GpuBufferSlice fogBuffer,
        LevelRenderState levelRenderState,
        net.minecraft.util.profiling.ProfilerFiller profilerFiller,
        Matrix4f modelView,
        com.mojang.blaze3d.resource.ResourceHandle<?> mainTarget,
        com.mojang.blaze3d.resource.ResourceHandle<?> translucentTarget,
        boolean renderBlockOutline,
        com.mojang.blaze3d.resource.ResourceHandle<?> entityOutlineTarget,
        com.mojang.blaze3d.resource.ResourceHandle<?> entityOutlineDepth,
        CallbackInfo ci
    ) {
        ip$layerRenderModelView = null;
    }

    @WrapOperation(
        method = "method_62214",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V"
        )
    )
    private void ip$wrapRenderGroup(
        ChunkSectionsToRender sectionsToRender,
        ChunkSectionLayerGroup group,
        GpuSampler sampler,
        Operation<Void> original
    ) {
        if (PortalRendering.isRendering()) {
            Matrix4f modelView = ip$layerRenderModelView;
            if (modelView != null) {
                FrontClipping.setupInnerClipping(
                    PortalRendering.getActiveClippingPlane(),
                    modelView,
                    -FrontClipping.ADJUSTMENT
                    // move the clipping plane a little back, to make world wrapping portal not z-fight
                );
            }
            
            if (PortalRendering.isRenderingOddNumberOfMirrors()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
            
            if (IPGlobal.enableDepthClampForPortalRendering) {
                CHelper.enableDepthClamp();
            }
        }

        original.call(sectionsToRender, group, sampler);

        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
            MyRenderHelper.recoverFaceCulling();
            
            if (IPGlobal.enableDepthClampForPortalRendering) {
                CHelper.disableDepthClamp();
            }
        }
    }
    
    @Inject(
        method = "cullTerrain",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetupTerrainBegin(
        Camera camera, Frustum frustum, boolean spectator,
        CallbackInfo ci
    ) {
        if (WorldRenderInfo.isRendering()) {
            if (level.dimension() != RenderStates.originalPlayerDimension) {
                sectionRenderDispatcher.setCameraPosition(camera.position());
            }
        }
        
        if (ip_allowOverrideTerrainSetup()) {
            if (WorldRenderInfo.isRendering()) {
                Profiler.get().push("ip_terrain_setup");
                VisibleSectionDiscovery.discoverVisibleSections(
                    level, ((ImmPtlViewArea) viewArea),
                    camera,
                    new Frustum(frustum).offsetToFullyIncludeCameraCube(8),
                    visibleSections
                );
                Profiler.get().pop();
                
                ci.cancel();
            }
        }
    }
    
    private boolean ip_allowOverrideTerrainSetup() {
        return !SodiumInterface.invoker.isSodiumPresent()
            && !IrisCompat.isRenderingShadowMap();
    }
    
    @Inject(
        method = "cullTerrain",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetupTerrainEnd(
        Camera camera, Frustum frustum, boolean spectator,
        CallbackInfo ci
    ) {
        if (!WorldRenderInfo.isRendering()) {
            if (ip_allowOverrideTerrainSetup()) {
                if (MyGameRenderer.vanillaTerrainSetupOverride > 0) {
                    MyGameRenderer.vanillaTerrainSetupOverride--;
                    
                    Profiler.get().push("ip_terrain_setup");
                    VisibleSectionDiscovery.discoverVisibleSections(
                        level, ((ImmPtlViewArea) viewArea),
                        camera,
                        new Frustum(frustum).offsetToFullyIncludeCameraCube(8),
                        visibleSections
                    );
                    Profiler.get().pop();
                }
                else if (IPGlobal.alwaysOverrideTerrainSetup) {
                    // debug
                    Profiler.get().push("ip_terrain_setup_debug");
                    VisibleSectionDiscovery.discoverVisibleSections(
                        level, ((ImmPtlViewArea) viewArea),
                        camera,
                        new Frustum(frustum).offsetToFullyIncludeCameraCube(8),
                        visibleSections
                    );
                    Profiler.get().pop();
                }
            }
        }
    }
    
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelTargetBundle;clear()V"
        )
    )
    private void redirectClearing(LevelTargetBundle targets) {
        if (!IPCGlobal.renderer.replaceFrameBufferClearing()) {
            targets.clear();
        }
    }
    
    @Redirect(
        method = "allChanged",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;Lnet/minecraft/world/level/Level;ILnet/minecraft/client/renderer/LevelRenderer;)Lnet/minecraft/client/renderer/ViewArea;"
        )
    )
    private ViewArea redirectConstructingBuildChunkStorage(
        SectionRenderDispatcher chunkBuilder_1,
        Level world_1,
        int int_1,
        LevelRenderer worldRenderer_1
    ) {
        if (IPCGlobal.useHackedChunkRenderDispatcher) {
            return new ImmPtlViewArea(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
        else {
            return new ViewArea(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
    }
    
    @Redirect(
        method = "extractVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;extractEntity(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"
        )
    )
    private EntityRenderState redirectExtractEntity(
        LevelRenderer renderer,
        Entity entity,
        float partialTick
    ) {
        ip$currentRenderedEntity.set(entity);
        return extractEntity(entity, partialTick);
    }

    @Redirect(
        method = "submitEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
        )
    )
    private void redirectSubmitEntity(
        EntityRenderDispatcher dispatcher,
        EntityRenderState renderState,
        CameraRenderState cameraRenderState,
        double cameraX,
        double cameraY,
        double cameraZ,
        PoseStack matrixStack,
        SubmitNodeCollector collector
    ) {
        Entity entity = ip$currentRenderedEntity.get();
        if (entity != null) {
            CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
        }
        dispatcher.submit(
            renderState,
            cameraRenderState,
            cameraX,
            cameraY,
            cameraZ,
            matrixStack,
            collector
        );
        if (entity != null) {
            CrossPortalEntityRenderer.afterRenderingEntity(entity);
        }
        ip$currentRenderedEntity.remove();
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_761;method_62203(Lnet/minecraft/class_9909;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
        )
    )
    private void beforeRenderingWeather(
        GraphicsResourceAllocator graphicsResourceAllocator,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        Matrix4f modelView,
        Matrix4f matrix4f2,
        Matrix4f matrix4f3,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean bl2,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getActiveClippingPlane(),
                modelView, 0
            );
            RenderStates.isRenderingPortalWeather = true;
        }
    }
    
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_761;method_62203(Lnet/minecraft/class_9909;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            shift = At.Shift.AFTER
        )
    )
    private void afterRenderingWeather(
        GraphicsResourceAllocator graphicsResourceAllocator,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        Matrix4f matrix4f,
        Matrix4f matrix4f2,
        Matrix4f matrix4f3,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean bl2,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
            RenderStates.isRenderingPortalWeather = false;
        }
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "extractVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;appearsGlowing()Z"
        )
    )
    private boolean redirectGlowing(EntityRenderState renderState) {
        if (WorldRenderInfo.isRendering()) {
            return false;
        }
        return renderState.appearsGlowing();
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
        method = "addSkyPass", at = @At("HEAD"), cancellable = true
    )
    private void onRenderSkyBegin(
        FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogBuffer, CallbackInfo ci
    ) {
        if (WorldRenderInfo.isRendering()) {
            if (!WorldRenderInfo.getTopRenderInfo().doRenderSky) {
                if (!IrisCompat.isShaders()) {
                    ci.cancel();
                    return;
                }
            }
        }
        
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
    }
    
    @Inject(
        method = "addSkyPass",
        at = @At("RETURN")
    )
    private void onRenderSkyEnd(
        FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogBuffer, CallbackInfo ci
    ) {
        MyRenderHelper.recoverFaceCulling();
    }
    
    // if not in spectator mode, when the camera is in block chunk culling will cull chunks wrongly
    @ModifyVariable(
        method = "cullTerrain",
        at = @At("HEAD"),
        argsOnly = true,
        index = 3
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
     * So {@link ViewArea#getRenderSectionAt} will return incorrect result
     */
    @Inject(
        method = "isSectionCompiledAndVisible",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIsChunkCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (PortalRendering.isRendering()) {
            if (!SodiumInterface.invoker.isSodiumPresent()) {
                if (viewArea instanceof ImmPtlViewArea immPtlViewArea) {
                    cir.setReturnValue(ip_isChunkCompiled(immPtlViewArea, blockPos));
                }
            }
        }
    }
    
    private boolean ip_isChunkCompiled(ImmPtlViewArea immPtlViewArea, BlockPos blockPos) {
        SectionPos sectionPos = SectionPos.of(blockPos);
        var renderChunk = immPtlViewArea.rawGet(
            sectionPos.x(), sectionPos.y(), sectionPos.z()
        );
        
        return renderChunk != null
            && renderChunk.getSectionMesh() != CompiledSectionMesh.UNCOMPILED;
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
    public void ip_myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float partialTick,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        EntityRenderState renderState = entityRenderDispatcher.extractEntity(entity, partialTick);
        SubmitNodeCollector collector = featureRenderDispatcher.getSubmitNodeStorage();
        CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
        entityRenderDispatcher.submit(
            renderState,
            levelRenderState.cameraRenderState,
            cameraX,
            cameraY,
            cameraZ,
            matrixStack,
            collector
        );
        CrossPortalEntityRenderer.afterRenderingEntity(entity);
        featureRenderDispatcher.renderAllFeatures();
        featureRenderDispatcher.endFrame();
    }
    
    @Override
    public PostChain portal_getTransparencyShader() {
        if (ip$transparencyChainOverrideSet) {
            return ip$transparencyChainOverride;
        }
        return ip$getTransparencyChain();
    }
    
    @Override
    public void portal_setTransparencyShader(PostChain arg) {
        if (arg == null) {
            ip$transparencyChainOverrideSet = true;
            ip$transparencyChainOverride = null;
            return;
        }
        PostChain current = ip$getTransparencyChain();
        if (arg == current) {
            ip$transparencyChainOverrideSet = false;
            ip$transparencyChainOverride = null;
        }
        else {
            ip$transparencyChainOverrideSet = true;
            ip$transparencyChainOverride = arg;
        }
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
        return getCapturedFrustum();
    }
    
    @Override
    public void portal_setFrustum(Frustum arg) {
        if (arg == null) {
            killFrustum();
        }
        else {
            ip$applyFrustum(arg);
        }
    }
    
    @Override
    public void portal_fullyDispose() {
        close();
        
        level = null;
    }
    
    @Override
    public void portal_setChunkInfoList(ObjectArrayList<SectionRenderDispatcher.RenderSection> arg) {
        visibleSections = arg;
    }
    
    @Override
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> portal_getChunkInfoList() {
        return visibleSections;
    }
}
