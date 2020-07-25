package com.qouteall.immersive_portals.mixin_client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.MyGameRenderer;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import com.qouteall.immersive_portals.render.PixelCuller;
import com.qouteall.immersive_portals.render.TransformationManager;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Set;

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
    
    @Shadow
    private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;
    
    @Shadow
    private ShaderEffect transparencyShader;
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V",
            ordinal = 1,
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
        CGlobal.renderer.onBeforeTranslucentRendering(matrices);
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
        CrossPortalEntityRenderer.onEndRenderingEntities(matrices);
        CGlobal.renderer.onAfterTranslucentRendering(matrices);
        
        //make hand rendering normal
        DiffuseLighting.method_27869(matrices.peek().getModel());
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V"
        )
    )
    private void onBeforeRenderingLayer(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            PixelCuller.updateCullingPlaneInner(
                matrices,
                PortalRendering.getRenderingPortal(),
                true
            );
            PixelCuller.startCulling();
            if (PortalRendering.isRenderingOddNumberOfMirrors()) {
                MyRenderHelper.applyMirrorFaceCulling();
            }
        }
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDD)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingLayer(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            PixelCuller.endCulling();
            MyRenderHelper.recoverFaceCulling();
        }
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getSolid()Lnet/minecraft/client/render/RenderLayer;"
        )
    )
    private void onBeginRenderingSolid(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        MyGameRenderer.doPruneVisibleChunks(this.visibleChunks);
    }
    
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/SkyProperties;isDarkened()Z"
        )
    )
    private void onAfterCutoutRendering(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        CrossPortalEntityRenderer.onBeginRenderingEnties(matrices);
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
    
    //to let the player be rendered when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/Camera;isThirdPerson()Z"
        )
    )
    private boolean redirectIsThirdPerson(Camera camera) {
        if (CrossPortalEntityRenderer.shouldRenderPlayerItself()) {
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
            if (CrossPortalEntityRenderer.shouldRenderPlayerItself()) {
                MyGameRenderer.renderPlayerItself(() -> {
                    double distanceToCamera =
                        entity.getCameraPosVec(RenderStates.tickDelta)
                            .distanceTo(client.gameRenderer.getCamera().getPos());
                    //avoid rendering player too near and block view except mirror
                    if (distanceToCamera > 1 || PortalRendering.isRenderingOddNumberOfMirrors()) {
                        CrossPortalEntityRenderer.beforeRenderingEntity(entity, matrixStack);
                        renderEntity(
                            entity,
                            cameraX, cameraY, cameraZ,
                            tickDelta,
                            matrixStack, vertexConsumerProvider
                        );
                        CrossPortalEntityRenderer.afterRenderingEntity(entity);
                    }
                    else {
//                        Helper.log("dis");
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
            PixelCuller.updateCullingPlaneInner(
                matrices,
                PortalRendering.getRenderingPortal(),
                true
            );
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
        if (PortalRendering.isRendering()) {
            PixelCuller.endCulling();
        }
    }
    
    //avoid render glowing entities when rendering portal
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;method_27022(Lnet/minecraft/entity/Entity;)Z"
        )
    )
    private boolean redirectGlowing(MinecraftClient client, Entity entity) {
        if (PortalRendering.isRendering()) {
            return false;
        }
        return client.method_27022(entity);
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
        if (PortalRendering.isRendering()) {
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
    
   
    
    @Inject(method = "renderSky", at = @At("HEAD"))
    private void onRenderSkyBegin(MatrixStack matrixStack_1, float float_1, CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            //reset gl states
            RenderLayer.getBlockLayers().get(0).startDrawing();
            RenderLayer.getBlockLayers().get(0).endDrawing();
            
            //fix sky abnormal with optifine and fog disabled
            if (OFInterface.isFogDisabled.getAsBoolean()) {
                GL11.glEnable(GL11.GL_FOG);
            }
        }
        
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
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

//        if (client.world.dimension instanceof AlternateDimension) {
//            AlternateSkyRenderer.renderAlternateSky(matrixStack_1, float_1);
//        }
        
        if (PortalRendering.isRendering()) {
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
    
    //reduce lag spike
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;updateChunks(J)V"
        )
    )
    private void redirectUpdateChunks(WorldRenderer worldRenderer, long limitTime) {
        if (PortalRendering.isRendering() && (!OFInterface.isOptifinePresent)) {
            portal_updateChunks();
        }
        else {
            updateChunks(limitTime);
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
        RenderStates.shouldForceDisableCull = PortalRendering.isRenderingOddNumberOfMirrors();
        immediate.draw(layer);
        RenderStates.shouldForceDisableCull = false;
    }
    
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V"
        )
    )
    private void redirectVertexDraw1(VertexConsumerProvider.Immediate immediate) {
        RenderStates.shouldForceDisableCull = PortalRendering.isRenderingOddNumberOfMirrors();
        immediate.draw();
        RenderStates.shouldForceDisableCull = false;
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
        if (Global.edgelessSky) {
            if (PortalRendering.isRendering()) {
                if (PortalRendering.getRenderingPortal() instanceof GlobalTrackedPortal) {
                    MyGameRenderer.renderSkyFor(
                        RenderDimensionRedirect.getRedirectedDimension(RenderStates.originalPlayerDimension),
                        matrixStack, f
                    );
                    return;
                }
            }
        }
        
        if (OFInterface.isShaders.getAsBoolean()) {
            RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
            RegistryKey<World> redirectedDimension = RenderDimensionRedirect.getRedirectedDimension(
                dim);
            
            MyGameRenderer.renderSkyFor(redirectedDimension, matrixStack, f);
            return;
        }
        
        worldRenderer.renderSky(matrixStack, f);
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
            MyGameRenderer.forceResetFogState();
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
    
    @Override
    public ShaderEffect portal_getTransparencyShader() {
        return transparencyShader;
    }
    
    @Override
    public void portal_setTransparencyShader(ShaderEffect arg) {
        transparencyShader = arg;
    }
    
    private void portal_updateChunks() {
        
        ChunkBuilder chunkBuilder = this.chunkBuilder;
        boolean uploaded = chunkBuilder.upload();
        this.needsTerrainUpdate |= uploaded;//no short circuit
        
        int limit = 1;
//        if (CGlobal.renderer.getPortalLayer() > 1) {
//            limit = 1;
//        }
        
        int num = 0;
        for (Iterator<ChunkBuilder.BuiltChunk> iterator = chunksToRebuild.iterator(); iterator.hasNext(); ) {
            ChunkBuilder.BuiltChunk builtChunk = iterator.next();
            
            //vanilla's updateChunks() does not check shouldRebuild()
            //so it may create many rebuild tasks and cancelling it which creates performance cost
            if (builtChunk.shouldBuild()) {
                builtChunk.scheduleRebuild(chunkBuilder);
                builtChunk.cancelRebuild();
                
                iterator.remove();
                
                num++;
                
                if (num >= limit) {
                    break;
                }
            }
        }
        
    }
    
}
