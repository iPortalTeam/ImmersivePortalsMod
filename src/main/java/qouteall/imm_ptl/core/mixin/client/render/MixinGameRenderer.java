package qouteall.imm_ptl.core.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ProfilerCompat;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.portal.animation.ClientPortalAnimationManagement;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.CrossPortalViewRendering;
import qouteall.imm_ptl.core.render.GuiPortalRendering;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.q_misc_util.Helper;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private LightTexture lightTexture;
    
    @Unique
    private boolean ip_renderHand = true;
    @Shadow
    @Final
    @Mutable
    private Camera mainCamera;
    
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private FogRenderer fogRenderer;
    
    @Unique
    private boolean ip_panoramicMode = false;
    
    @Shadow
    @Final
    private PerspectiveProjectionMatrixBuffer levelProjectionMatrixBuffer;
    
    @Shadow
    protected abstract void bobView(PoseStack matrices, float f);
    
    @Shadow @Final private static Logger LOGGER;
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onFarBeforeRendering(
        DeltaTracker deltaTracker, boolean renderWorldIn, CallbackInfo ci
    ) {
        ProfilerCompat.push("ip_pre_total_render");
        IPGlobal.PRE_TOTAL_RENDER_TASK_LIST.processTasks();
        ProfilerCompat.pop();
        if (minecraft.level == null) {
            return;
        }
        if (!renderWorldIn) { // when respawning, it will runTick and execute rendering
            return;
        }
        ProfilerCompat.push("ip_pre_render");
        // Note do not use delta tick. use partial tick.
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        RenderStates.updatePreRenderInfo(partialTick);
        StableClientTimer.update(
            minecraft.level.getGameTime(), partialTick
        );
        ClientPortalAnimationManagement.update(); // must update before teleportation
        ClientTeleportationManager.manageTeleportation(false);
        IPGlobal.PRE_GAME_RENDER_EVENT.invoker().run();
        if (IPCGlobal.earlyRemoteUpload) {
            MyRenderHelper.earlyRemoteUpload();
        }
        ProfilerCompat.pop();
        
        RenderStates.frameIndex++;
    }
    
    //before rendering world (not triggered when rendering portal)
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void onBeforeRenderingCenter(
        DeltaTracker deltaTracker, boolean bl, CallbackInfo ci
    ) {
        PortalRenderer.switchToCorrectRenderer();
        
        IPCGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingCenter(
        DeltaTracker deltaTracker, boolean bl, CallbackInfo ci
    ) {
        IPCGlobal.renderer.finishRendering();
        
        RenderStates.onTotalRenderEnd();
        
        GuiPortalRendering._onGameRenderEnd();
        
        if (IPCGlobal.lateClientLightUpdate) {
            ProfilerCompat.push("ip_late_update_light");
            MyRenderHelper.lateUpdateLight();
            ProfilerCompat.pop();
        }
    }
    
    //special rendering in third person view
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void redirectRenderingWorld(
        GameRenderer gameRenderer, DeltaTracker deltaTracker
    ) {
        if (CrossPortalViewRendering.renderCrossPortalView()) {
            return;
        }
        
        gameRenderer.renderLevel(deltaTracker);
    }
    
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRenderCenterEnded(
        DeltaTracker deltaTracker, CallbackInfo ci
    ) {
        IPCGlobal.renderer.onHandRenderingEnded();
    }

    @Inject(method = "isPanoramicMode", at = @At("RETURN"), cancellable = true)
    private void ip_overridePanoramicMode(CallbackInfoReturnable<Boolean> cir) {
        if (ip_panoramicMode) {
            cir.setReturnValue(true);
        }
    }
    
    @WrapOperation(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"
        )
    )
    private void wrapRenderLevel(
        LevelRenderer instance,
        GraphicsResourceAllocator resourceAllocator,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        Matrix4f viewMatrix,
        Matrix4f projectionMatrix,
        Matrix4f projectionMatrixForCulling,
        GpuBufferSlice fogBuffer,
        Vector4f fogColor,
        boolean createWorldFog,
        Operation<Void> original
    ) {
        original.call(
            instance,
            resourceAllocator,
            deltaTracker,
            bl,
            camera,
            viewMatrix,
            projectionMatrix,
            projectionMatrixForCulling,
            fogBuffer,
            fogColor,
            createWorldFog
        );
        
        IPCGlobal.renderer.onBeforeHandRendering(viewMatrix);
    }
    
    //resize all world renderers when resizing window
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;resize(II)V", at = @At("RETURN"))
    private void onOnResized(int int_1, int int_2, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            ClientWorldLoader.WORLD_RENDERER_MAP.values().stream()
                .filter(
                    worldRenderer -> worldRenderer != minecraft.levelRenderer
                )
                .forEach(
                    worldRenderer -> worldRenderer.resize(int_1, int_2)
                );
        }
    }
    
    private static boolean portal_isRenderingHand = false;

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void onRenderHandBegins(float partialTick, boolean renderHand, Matrix4f matrix4f, CallbackInfo ci) {
        if (!renderHand || !ip_renderHand) {
            portal_isRenderingHand = false;
            ci.cancel();
            return;
        }
        portal_isRenderingHand = true;
    }
    
    @Inject(method = "renderItemInHand", at = @At("RETURN"))
    private void onRenderHandEnds(float partialTick, boolean renderHand, Matrix4f matrix4f, CallbackInfo ci) {
        portal_isRenderingHand = false;
    }
    
    // not using ModifyArgs because ModifyArgs seems broken on Forge
    @ModifyArg(
        method = "bobView",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 0
    )
    private float modifyBobViewTranslateX(float f) {
        if (portal_isRenderingHand) {
            return f;
        }
        else {
            return (float) (f * RenderStates.getViewBobbingOffsetMultiplier());
        }
    }
    
    @ModifyArg(
        method = "bobView",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 1
    )
    private float modifyBobViewTranslateY(float f) {
        if (portal_isRenderingHand) {
            return f;
        }
        else {
            return (float) (f * RenderStates.getViewBobbingOffsetMultiplier());
        }
    }
    
    @ModifyArg(
        method = "bobView",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 2
    )
    private float modifyBobViewTranslateZ(float f) {
        if (portal_isRenderingHand) {
            return f;
        }
        else {
            return (float) (f * RenderStates.getViewBobbingOffsetMultiplier());
        }
    }


//    @Redirect(
//        method = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
//        at = @At(
//            value = "INVOKE",
//            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
//        )
//    )
//    private void redirectBobViewTranslate(PoseStack matrixStack, float x, float y, float z) {
//        if (portal_isRenderingHand) {
//            matrixStack.translate(x, y, z);
//        }
//        else {
//            double multiplier = RenderStates.getViewBobbingOffsetMultiplier();
//            matrixStack.translate(
//                x * multiplier, y * multiplier, z * multiplier
//            );
//        }
//    }
    
    // make sure that the portal rendering basic projection matrix is right
    // the basic projection matrix does not contain view bobbing
    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;",
            ordinal = 0
        )
    )
    private Matrix4f redirectGetBasicProjectionMatrix(GameRenderer instance, float fov) {
        if (PortalRendering.isRendering()) {
            if (RenderStates.basicProjectionMatrix != null) {
                // replace the basic projection matrix
                // copy to avoid unwanted modification
                return new Matrix4f(RenderStates.basicProjectionMatrix);
            }
            else {
                LOGGER.error("[iPortal] Projection matrix state abnormal");
            }
        }
        
        Matrix4f result = instance.getProjectionMatrix(fov);
        // copy to avoid unwanted modification
        RenderStates.basicProjectionMatrix = new Matrix4f(result);
        
        return result;
    }
    
    @WrapOperation(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4f;rotation(Lorg/joml/Quaternionfc;)Lorg/joml/Matrix4f;",
            remap = false
        )
    )
    private Matrix4f wrapCameraTransformation(
        Matrix4f instance, Quaternionfc quat, Operation<Matrix4f> original
    ) {
        Matrix4f r = original.call(instance, quat);
        return TransformationManager.processTransformation(mainCamera, r);
    }
    
    @Override
    public void ip_setLightmapTextureManager(LightTexture manager) {
        lightTexture = manager;
    }
    
    @Override
    public boolean ip_getDoRenderHand() {
        return ip_renderHand;
    }

    @Override
    public void ip_setDoRenderHand(boolean doRenderHand) {
        ip_renderHand = doRenderHand;
    }
    
    @Override
    public void ip_setCamera(Camera camera_) {
        mainCamera = camera_;
    }
    
    @Override
    public void ip_setIsRenderingPanorama(boolean cond) {
        ip_panoramicMode = cond;
    }

    @Override
    public void ip_resetProjectionMatrix(Matrix4f matrix) {
        RenderSystem.setProjectionMatrix(
            levelProjectionMatrixBuffer.getBuffer(matrix),
            ProjectionType.PERSPECTIVE
        );
    }

    @Override
    public FogRenderer ip_getFogRenderer() {
        return fogRenderer;
    }

}
