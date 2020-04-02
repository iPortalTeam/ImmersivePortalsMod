package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.alternate_dimension.AlternateSky;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.far_scenery.FarSceneryRenderer;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PixelCuller;
import com.qouteall.immersive_portals.render.TransformationManager;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer implements IEWorldRenderer {
    
    @Shadow
    private ClientWorld world;
    
    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    
    @Shadow
    @Final
    public MinecraftClient client;
    
    @Shadow
    private double lastTranslucentSortX;
    
    @Shadow
    private double lastTranslucentSortY;
    
    @Shadow
    private double lastTranslucentSortZ;
    
    @Shadow
    private BuiltChunkStorage chunks;
    
    @Shadow
    protected abstract void renderLayer(
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3
    );
    
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
    
    @Mutable
    @Shadow
    @Final
    private ObjectList<?> visibleChunks;
    
    @Shadow
    private int renderDistance;
    
    @Shadow
    private boolean needsTerrainUpdate;
    
    
    @Shadow
    private VertexBuffer lightSkyBuffer;
    
    @Shadow
    @Final
    private VertexFormat skyVertexFormat;
    
    @Shadow
    @Final
    private TextureManager textureManager;
    
    @Shadow
    private VertexBuffer darkSkyBuffer;
    
    @Shadow
    private ChunkBuilder chunkBuilder;
    
    @Shadow
    protected abstract void updateChunks(long limitTime);
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V"
        )
    )
    private void onRenderBeforeRenderLayer(
        WorldRenderer worldRenderer,
        RenderLayer renderLayer,
        MatrixStack matrices,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        boolean isTranslucent = renderLayer == RenderLayer.getTranslucent();
        if (isTranslucent) {
            CrossPortalEntityRenderer.onEndRenderingEntities(matrices);
            CGlobal.renderer.onBeforeTranslucentRendering(matrices);
            FarSceneryRenderer.onBeforeTranslucentRendering(matrices);
        }
        renderLayer(
            renderLayer, matrices,
            cameraX, cameraY, cameraZ
        );
        if (isTranslucent) {
            CGlobal.renderer.onAfterTranslucentRendering(matrices);
        }
        
        if (renderLayer == RenderLayer.getCutout()) {
            CrossPortalEntityRenderer.onBeginRenderingEnties(matrices);
        }
    }
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"
        )
    )
    private void redirectClearing(int int_1, boolean boolean_1) {
        if (!CGlobal.renderer.shouldSkipClearing()) {
            RenderSystem.clear(int_1, boolean_1);
        }
    }
    
    @Redirect(
        method = "reload",
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
        if (CGlobal.useHackedChunkRenderDispatcher) {
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
    
    //apply culling and apply optimization
    @Inject(
        method = "renderLayer",
        at = @At("HEAD")
    )
    private void onStartRenderLayer(
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfo ci
    ) {
        ObjectList<?> visibleChunks = this.visibleChunks;
        if (renderLayer_1 == RenderLayer.getSolid()) {
            MyGameRenderer.doPruneVisibleChunks(visibleChunks);
        }
        
        if (CGlobal.renderer.isRendering()) {
            PixelCuller.updateCullingPlaneInner(
                matrixStack_1,
                CGlobal.renderer.getRenderingPortal()
            );
            PixelCuller.startCulling();
            if (MyRenderHelper.isRenderingOddNumberOfMirrors()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
        }
    }
    
    @Inject(
        method = "renderLayer",
        at = @At("TAIL")
    )
    private void onStopRenderLayer(
        RenderLayer renderLayer_1,
        MatrixStack matrixStack_1,
        double double_1,
        double double_2,
        double double_3,
        CallbackInfo ci
    ) {
        if (CGlobal.renderer.isRendering()) {
            PixelCuller.endCulling();
            MyRenderHelper.recoverFaceCulling();
        }
    }
    
    //to let the player be rendered when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z"
        )
    )
    private boolean redirectIsThirdPerson(Camera camera) {
        if (CGlobal.renderer.shouldRenderPlayerItself()) {
            return true;
        }
        return camera.isThirdPerson();
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
        if (entity == camera.getFocusedEntity()) {
            if (CGlobal.renderer.shouldRenderPlayerItself()) {
                MyGameRenderer.renderPlayerItself(() -> {
                    CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
                    renderEntity(
                        entity,
                        cameraX, cameraY, cameraZ,
                        tickDelta,
                        matrixStack, vertexConsumerProvider
                    );
                    CrossPortalEntityRenderer.afterRenderingEntity(entity);
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
        if (CGlobal.renderer.isRendering()) {
            PixelCuller.updateCullingPlaneInner(matrices, CGlobal.renderer.getRenderingPortal());
            PixelCuller.startCulling();
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
        if (CGlobal.renderer.isRendering()) {
            PixelCuller.endCulling();
        }
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;isGlowing()Z"
        )
    )
    private boolean redirectGlowing(Entity entity) {
        if (CGlobal.renderer.isRendering()) {
            return false;
        }
        return entity.isGlowing();
    }
    
    private static boolean isReloadingOtherWorldRenderers = false;
    
    //reload other world renderers when the main world renderer is reloaded
    @Inject(method = "reload", at = @At("TAIL"))
    private void onReload(CallbackInfo ci) {
        ClientWorldLoader clientWorldLoader = CGlobal.clientWorldLoader;
        WorldRenderer this_ = (WorldRenderer) (Object) this;
        if (isReloadingOtherWorldRenderers) {
            return;
        }
        if (CGlobal.renderer.isRendering()) {
            return;
        }
        if (clientWorldLoader.getIsLoadingFakedWorld()) {
            return;
        }
        if (this_ != MinecraftClient.getInstance().worldRenderer) {
            return;
        }
        
        isReloadingOtherWorldRenderers = true;
        
        for (WorldRenderer worldRenderer : clientWorldLoader.worldRendererMap.values()) {
            if (worldRenderer != this_) {
                worldRenderer.reload();
            }
        }
        isReloadingOtherWorldRenderers = false;
    }
    
    //avoid translucent sort while rendering portal
    @Redirect(
        method = "renderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getTranslucent()Lnet/minecraft/client/render/RenderLayer;"
        )
    )
    private RenderLayer redirectGetTranslucent() {
        if (CGlobal.renderer.isRendering()) {
            return null;
        }
        return RenderLayer.getTranslucent();
    }
    
    @Inject(method = "renderSky", at = @At("HEAD"))
    private void onRenderSkyBegin(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        if (CGlobal.renderer.isRendering()) {
            //reset gl states
            RenderLayer.getBlockLayers().get(0).startDrawing();
            RenderLayer.getBlockLayers().get(0).endDrawing();
            
            //fix sky abnormal with optifine and fog disabled
            if (OFInterface.isFogDisabled.getAsBoolean()) {
                GL11.glEnable(GL11.GL_FOG);
            }
        }
        
        if (MyRenderHelper.isRenderingOddNumberOfMirrors()) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
    }
    
    //fix sun abnormal with optifine and fog disabled
    @Inject(
        method = "renderSky",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/WorldRenderer;SUN:Lnet/minecraft/util/Identifier;"
        )
    )
    private void onStartRenderingSun(MatrixStack matrixStack, float f, CallbackInfo ci) {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            GL11.glDisable(GL11.GL_FOG);
        }
    }
    
    @Inject(method = "renderSky", at = @At("RETURN"))
    private void onRenderSkyEnd(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        
        if (client.world.dimension instanceof AlternateDimension) {
            AlternateSky.renderAlternateSky(matrixStack_1, float_1);
        }
        
        if (CGlobal.renderer.isRendering()) {
            //fix sky abnormal with optifine and fog disabled
            GL11.glDisable(GL11.GL_FOG);
            RenderSystem.enableFog();
            RenderSystem.disableFog();
        }
        
        MyRenderHelper.recoverFaceCulling();
    }
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onBeforeRender(
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
        TransformationManager.processTransformation(camera, matrices);
    }
    
    //update builtChunkStorage every frame
    //update terrain when rendering portal
    @Inject(
        method = "setupTerrain",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/chunk/ChunkBuilder;setCameraPosition(Lnet/minecraft/util/math/Vec3d;)V"
        )
    )
    private void onBeforeChunkBuilderSetCameraPosition(
        Camera camera_1,
        Frustum frustum_1,
        boolean boolean_1,
        int int_1,
        boolean boolean_2,
        CallbackInfo ci
    ) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            this.chunks.updateCameraPosition(this.client.player.getX(), this.client.player.getZ());
        }
        
        if (CGlobal.renderer.isRendering()) {
            needsTerrainUpdate = true;
        }
    }
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;updateChunks(J)V"
        )
    )
    private void redirectUpdateChunks(WorldRenderer worldRenderer, long limitTime) {
        if (CGlobal.renderer.isRendering()) {
            updateChunks(0);
        }
        else {
            updateChunks(limitTime);
        }
    }
    
    //rebuild less chunk in render thread while rendering portal to reduce lag spike
    //minecraft has two places rebuilding chunks in render thread
    //one in updateChunks() one in setupTerrain()
    @ModifyConstant(
        method = "setupTerrain",
        constant = @Constant(doubleValue = 768.0D)
    )
    private double modifyRebuildRange(double original) {
        if (CGlobal.renderer.isRendering()) {
            return 256.0;
        }
        else {
            return original;
        }
    }
    
    //disable cull when rendering mirror
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;)V"
        )
    )
    private void redirectVertexDraw(VertexConsumerProvider.Immediate immediate, RenderLayer layer) {
        MyRenderHelper.shouldForceDisableCull = MyRenderHelper.isRenderingOddNumberOfMirrors();
        immediate.draw(layer);
        MyRenderHelper.shouldForceDisableCull = false;
    }
    
    //redirect sky rendering dimension
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderSky(Lnet/minecraft/client/util/math/MatrixStack;F)V"
        )
    )
    private void redirectRenderSky(WorldRenderer worldRenderer, MatrixStack matrixStack, float f) {
        if (OFInterface.isShaders.getAsBoolean()) {
            MyGameRenderer.renderSkyFor(
                RenderDimensionRedirect.getRedirectedDimension(
                    MinecraftClient.getInstance().world.dimension.getType()
                ),
                matrixStack,
                f
            );
        }
        else {
            worldRenderer.renderSky(matrixStack, f);
        }
    }
    
    //fix cloud fog abnormal with OptiFine and fog disabled
    @Inject(
        method = "renderClouds(Lnet/minecraft/client/util/math/MatrixStack;FDDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableFog()V",
            shift = At.Shift.AFTER
        )
    )
    private void onEnableFogInRenderClouds(
        MatrixStack matrices,
        float tickDelta,
        double cameraX,
        double cameraY,
        double cameraZ,
        CallbackInfo ci
    ) {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            MyGameRenderer.forceResetFog();
            GL11.glEnable(GL11.GL_FOG);
        }
    }
    
    @Override
    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }
    
    @Override
    public BuiltChunkStorage getBuiltChunkStorage() {
        return chunks;
    }
    
    @Override
    public ObjectList getVisibleChunks() {
        return visibleChunks;
    }
    
    @Override
    public void setVisibleChunks(ObjectList l) {
        visibleChunks = l;
    }
    
    @Override
    public ChunkBuilder getChunkBuilder() {
        return chunkBuilder;
    }
    
    @Override
    public void myRenderEntity(
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
}
