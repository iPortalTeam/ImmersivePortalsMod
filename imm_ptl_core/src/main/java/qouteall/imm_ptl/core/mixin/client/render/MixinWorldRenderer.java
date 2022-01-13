package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.network.IPCommonNetworkClient;
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

@Mixin(value = WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    @Shadow
    private BuiltChunkStorage chunks;
    
    @Shadow
    protected abstract void renderEntity(
        Entity entity_1,
        double double_1,
        double double_2,
        double double_3,
        float float_1,
        MatrixStack matrixStack_1,
        VertexConsumerProvider vertexConsumerProvider_1
    );
    
    @Shadow
    private ChunkBuilder chunkBuilder;
    
    @Shadow
    private ShaderEffect transparencyShader;
    
    @Mutable
    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;
    
    @Shadow
    private int viewDistance;
    
    @Shadow
    @Nullable
    private Framebuffer translucentFramebuffer;
    
    @Shadow
    private Frustum frustum;
    
    @Shadow
    @Nullable
    private VertexBuffer starsBuffer;
    
    @Shadow
    @Nullable
    private VertexBuffer lightSkyBuffer;
    
    @Shadow
    @Nullable
    private VertexBuffer darkSkyBuffer;
    
    @Shadow
    @Nullable
    private VertexBuffer cloudsBuffer;
    
    @Mutable
    @Shadow
    @Final
    private ObjectArrayList<WorldRenderer.ChunkInfo> chunkInfos;
    
    // important rendering hooks
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void onBeforeTranslucentRendering(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        // draw the entity vertices before rendering portal
        // because there is only one additional buffer builder for portal rendering
        /**{@link MyGameRenderer#secondaryBufferBuilderStorage}*/
        if (WorldRenderInfo.isRendering()) {
            client.getBufferBuilders().getEntityVertexConsumers().draw();
        }
        
        IPCGlobal.renderer.onBeforeTranslucentRendering(matrices);
        
        MyGameRenderer.updateFogColor();
        MyGameRenderer.resetFogState();
        
        //is it necessary?
        MyGameRenderer.resetDiffuseLighting(matrices);
        
        FrontClipping.disableClipping();
        
        CrossPortalEntityRenderer.onEndRenderingEntities(matrices);
    }
    
    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void onAfterTranslucentRendering(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        
        IPCGlobal.renderer.onAfterTranslucentRendering(matrices);
        
        //make hand rendering normal
        DiffuseLighting.disableForLevel(matrices.peek().getPositionMatrix());
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLnet/minecraft/util/math/Matrix4f;)V"
        )
    )
    private void onBeforeRenderingLayer(
        MatrixStack matrices, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getRenderingPortal(),
                true,
                matrices
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
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLnet/minecraft/util/math/Matrix4f;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingLayer(
        MatrixStack matrices, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f,
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
        method = "setupTerrain",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetupTerrainBegin(
        Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator,
        CallbackInfo ci
    ) {
        if (WorldRenderInfo.isRendering()) {
            if (world.getRegistryKey() != RenderStates.originalPlayerDimension) {
                chunkBuilder.setCameraPosition(camera.getPos());
            }
        }
        
        if (ip_allowOverrideTerrainSetup()) {
            if (WorldRenderInfo.isRendering()) {
                world.getProfiler().push("ip_terrain_setup");
                VisibleSectionDiscovery.discoverVisibleSections(
                    world, ((MyBuiltChunkStorage) chunks),
                    camera,
                    new Frustum(frustum).method_38557(8),
                    chunkInfos
                );
                world.getProfiler().pop();
                
                ci.cancel();
            }
        }
    }
    
    private boolean ip_allowOverrideTerrainSetup() {
        return !SodiumInterface.invoker.isSodiumPresent()
            && !IrisInterface.invoker.isRenderingShadowMap();
    }
    
    @Inject(
        method = "setupTerrain",
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
                    
                    world.getProfiler().push("ip_terrain_setup");
                    VisibleSectionDiscovery.discoverVisibleSections(
                        world, ((MyBuiltChunkStorage) chunks),
                        camera,
                        new Frustum(frustum).method_38557(8),
                        chunkInfos
                    );
                    world.getProfiler().pop();
                }
                else if (IPGlobal.alwaysOverrideTerrainSetup) {
                    // debug
                    world.getProfiler().push("ip_terrain_setup_debug");
                    VisibleSectionDiscovery.discoverVisibleSections(
                        world, ((MyBuiltChunkStorage) chunks),
                        camera,
                        new Frustum(frustum).method_38557(8),
                        chunkInfos
                    );
                    world.getProfiler().pop();
                }
            }
        }
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/DimensionEffects;isDarkened()Z"
        )
    )
    private void onAfterCutoutRendering(
        MatrixStack matrices, float tickDelta, long limitTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f,
        CallbackInfo ci
    ) {
//        IPCGlobal.renderer.onBeforeTranslucentRendering(matrices);
        
        CrossPortalEntityRenderer.onBeginRenderingEntities(matrices);
    }
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!IPCGlobal.renderer.replaceFrameBufferClearing()) {
            RenderSystem.clear(int_1, boolean_1);
        }
    }
    
    @Redirect(
        method = "reload()V",
        at = @At(
            value = "NEW",
            target = "net/minecraft/client/render/BuiltChunkStorage"
        )
    )
    private BuiltChunkStorage redirectConstructingBuildChunkStorage(
        ChunkBuilder chunkBuilder_1,
        World world_1,
        int int_1,
        WorldRenderer worldRenderer_1
    ) {
        if (IPCGlobal.useHackedChunkRenderDispatcher) {
            return new MyBuiltChunkStorage(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
        else {
            return new BuiltChunkStorage(
                chunkBuilder_1, world_1, int_1, worldRenderer_1
            );
        }
    }
    
    //render player itself when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V"
        )
    )
    private void redirectRenderEntity(
        WorldRenderer worldRenderer,
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider
    ) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        if (entity == camera.getFocusedEntity() && WorldRenderInfo.isRendering()) { //player
            if (CrossPortalEntityRenderer.shouldRenderEntityNow(entity)) {
                MyGameRenderer.renderPlayerItself(() -> {
                    if (CrossPortalEntityRenderer.shouldRenderPlayerNormally(entity)) {
                        CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
                        renderEntity(
                            entity,
                            cameraX, cameraY, cameraZ,
                            tickDelta,
                            matrixStack, vertexConsumerProvider
                        );
                        CrossPortalEntityRenderer.afterRenderingEntity(entity);
                    }
                });
                return;
            }
        }
        
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
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V"
        )
    )
    private void beforeRenderingWeather(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f matrix4f,
        CallbackInfo ci
    ) {
        if (PortalRendering.isRendering()) {
            FrontClipping.setupInnerClipping(
                PortalRendering.getRenderingPortal(), true, matrices
            );
            RenderStates.isRenderingPortalWeather = true;
        }
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderWeather(Lnet/minecraft/client/render/LightmapTextureManager;FDDD)V",
            shift = At.Shift.AFTER
        )
    )
    private void afterRenderingWeather(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
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
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"
        )
    )
    private boolean redirectGlowing(MinecraftClient client, Entity entity) {
        if (WorldRenderInfo.isRendering()) {
            return false;
        }
        return client.hasOutline(entity);
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    // sometimes we change renderDistance but we don't want to reload it
    @Inject(method = "reload()V", at = @At("HEAD"), cancellable = true)
    private void onReloadStarted(CallbackInfo ci) {
        if (WorldRenderInfo.isRendering()) {
            Helper.log("world renderer reloading cancelled during portal rendering");
            ci.cancel();
        }
    }
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "reload()V", at = @At("TAIL"))
    private void onReloadFinished(CallbackInfo ci) {
        WorldRenderer this_ = (WorldRenderer) (Object) this;
        
        if (world != null) {
            Helper.log("WorldRenderer reloaded " + world.getRegistryKey().getValue());
        }
        
        if (isReloadingOtherWorldRenderers) {
            return;
        }
        if (PortalRendering.isRendering()) {
            return;
        }
        if (ClientWorldLoader.getIsCreatingClientWorld()) {
            return;
        }
        if (this_ != MinecraftClient.getInstance().worldRenderer) {
            return;
        }
        
        isReloadingOtherWorldRenderers = true;
        
        for (WorldRenderer worldRenderer : ClientWorldLoader.worldRendererMap.values()) {
            if (worldRenderer != this_) {
                worldRenderer.reload();
            }
        }
        isReloadingOtherWorldRenderers = false;
    }
    
    @Inject(
        method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V",
        at = @At("HEAD"), cancellable = true
    )
    private void onRenderSkyBegin(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            if (PortalRendering.getRenderingPortal().isFuseView()) {
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
        method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V",
        at = @At("RETURN")
    )
    private void onRenderSkyEnd(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable, CallbackInfo ci) {
        MyRenderHelper.recoverFaceCulling();
    }
    
    @Inject(
        method = "setupFrustum", at = @At("HEAD")
    )
    private void onSetupFrustum(MatrixStack matrices, Vec3d pos, Matrix4f projectionMatrix, CallbackInfo ci) {
        TransformationManager.processTransformation(client.gameRenderer.getCamera(), matrices);
    }
    
    // vanilla clears translucentFramebuffer even when transparencyShader is null
    // it makes the framebuffer to be wrongly bound in fabulous mode
    @Redirect(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/WorldRenderer;translucentFramebuffer:Lnet/minecraft/client/gl/Framebuffer;"
        )
    )
    private Framebuffer redirectTranslucentFramebuffer(WorldRenderer this_) {
        if (PortalRendering.isRendering()) {
            return null;
        }
        else {
            return translucentFramebuffer;
        }
    }
    
    // if not in spectator mode, when the camera is in block chunk culling will cull chunks wrongly
    @ModifyVariable(
        method = "setupTerrain",
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
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;runQueuedChunkUpdates()V"
        )
    )
    private void redirectRunQueuedChunkUpdates(ClientWorld world) {
        IPCommonNetworkClient.withSwitchedWorld(
            world, world::runQueuedChunkUpdates
        );
    }
    
    @Override
    public EntityRenderDispatcher ip_getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }
    
    @Override
    public BuiltChunkStorage ip_getBuiltChunkStorage() {
        return chunks;
    }
    
    @Override
    public ChunkBuilder getChunkBuilder() {
        return chunkBuilder;
    }
    
    @Override
    public void ip_myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider vertexConsumerProvider
    ) {
        renderEntity(
            entity, cameraX, cameraY, cameraZ, tickDelta, matrixStack, vertexConsumerProvider
        );
    }
    
    @Override
    public ShaderEffect portal_getTransparencyShader() {
        return transparencyShader;
    }
    
    @Override
    public void portal_setTransparencyShader(ShaderEffect arg) {
        transparencyShader = arg;
    }
    
    @Override
    public BufferBuilderStorage ip_getBufferBuilderStorage() {
        return bufferBuilders;
    }
    
    @Override
    public void ip_setBufferBuilderStorage(BufferBuilderStorage arg) {
        bufferBuilders = arg;
    }
    
    @Override
    public Frustum portal_getFrustum() {
        return frustum;
    }
    
    @Override
    public void portal_setFrustum(Frustum arg) {
        frustum = arg;
    }
    
    @Override
    public void portal_fullyDispose() {
        if (starsBuffer != null) {
            starsBuffer.close();
        }
        if (lightSkyBuffer != null) {
            lightSkyBuffer.close();
        }
        if (darkSkyBuffer != null) {
            darkSkyBuffer.close();
        }
        if (cloudsBuffer != null) {
            cloudsBuffer.close();
        }
        
        world = null;
        
    }
    
    @Override
    public void portal_setChunkInfoList(ObjectArrayList<WorldRenderer.ChunkInfo> arg) {
        chunkInfos = arg;
    }
    
    @Override
    public ObjectArrayList<WorldRenderer.ChunkInfo> portal_getChunkInfoList() {
        return chunkInfos;
    }
}
