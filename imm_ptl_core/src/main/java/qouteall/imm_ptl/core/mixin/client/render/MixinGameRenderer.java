package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.portal.animation.ClientPortalAnimationManagement;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.CrossPortalViewRendering;
import qouteall.imm_ptl.core.render.GuiPortalRendering;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private LightTexture lightTexture;
    
    @Shadow
    private boolean renderHand;
    @Shadow
    @Final
    @Mutable
    private Camera mainCamera;
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Shadow
    private int tick;
    
    @Shadow
    private boolean panoramicMode;
    
    @Shadow
    public abstract void resetProjectionMatrix(Matrix4f matrix4f);
    
    @Shadow
    protected abstract void bobView(PoseStack matrices, float f);
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V", at = @At("HEAD"))
    private void onFarBeforeRendering(
        float tickDelta,
        long nanoTime,
        boolean renderWorldIn,
        CallbackInfo ci
    ) {
        minecraft.getProfiler().push("ip_pre_total_render");
        IPGlobal.preTotalRenderTaskList.processTasks();
        minecraft.getProfiler().pop();
        if (minecraft.level == null) {
            return;
        }
        if (!renderWorldIn) { // when respawning, it will runTick and execute rendering
            return;
        }
        minecraft.getProfiler().push("ip_pre_render");
        RenderStates.updatePreRenderInfo(tickDelta);
        StableClientTimer.update(minecraft.level.getGameTime(), tickDelta);
        ClientPortalAnimationManagement.update(); // must update before teleportation
        IPCGlobal.clientTeleportationManager.manageTeleportation(false);
        IPGlobal.preGameRenderSignal.emit();
        if (IPCGlobal.earlyRemoteUpload) {
            MyRenderHelper.earlyRemoteUpload();
        }
        minecraft.getProfiler().pop();
        
        RenderStates.frameIndex++;
    }
    
    //before rendering world (not triggered when rendering portal)
    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"
        )
    )
    private void onBeforeRenderingCenter(
        float float_1,
        long long_1,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        PortalRenderer.switchToCorrectRenderer();
        
        IPCGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterRenderingCenter(
        float float_1,
        long long_1,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.finishRendering();
        
        RenderStates.onTotalRenderEnd();
        
        GuiPortalRendering.onGameRenderEnd();
    
        if (IPCGlobal.lateClientLightUpdate) {
            minecraft.getProfiler().push("ip_late_update_light");
            MyRenderHelper.lateUpdateLight();
            minecraft.getProfiler().pop();
        }
    }
    
    //special rendering in third person view
    @Redirect(
        method = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"
        )
    )
    private void redirectRenderingWorld(
        GameRenderer gameRenderer, float tickDelta, long limitTime, PoseStack matrix
    ) {
        if (CrossPortalViewRendering.renderCrossPortalView()) {
            return;
        }
        
        gameRenderer.renderLevel(tickDelta, limitTime, matrix);
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
    private void onRenderCenterEnded(
        float float_1,
        long long_1,
        PoseStack matrixStack_1,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.onHandRenderingEnded(matrixStack_1);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRightBeforeHandRendering(float tickDelta, long limitTime, PoseStack matrix, CallbackInfo ci) {
        IPCGlobal.renderer.onBeforeHandRendering(matrix);
    }
    
    //resize all world renderers when resizing window
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;resize(II)V", at = @At("RETURN"))
    private void onOnResized(int int_1, int int_2, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            ClientWorldLoader.worldRendererMap.values().stream()
                .filter(
                    worldRenderer -> worldRenderer != minecraft.levelRenderer
                )
                .forEach(
                    worldRenderer -> worldRenderer.resize(int_1, int_2)
                );
        }
    }
    
    private static boolean portal_isRenderingHand = false;
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V", at = @At("HEAD"))
    private void onRenderHandBegins(PoseStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        portal_isRenderingHand = true;
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V", at = @At("RETURN"))
    private void onRenderHandEnds(PoseStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        portal_isRenderingHand = false;
    }
    
    // do not translate
    @Redirect(
        method = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
        )
    )
    private void redirectBobViewTranslate(PoseStack matrixStack, float x, float y, float z) {
        if (portal_isRenderingHand) {
            matrixStack.translate(x, y, z);
        }
        else {
            double multiplier = RenderStates.getViewBobbingOffsetMultiplier();
            matrixStack.translate(
                x * multiplier, y * multiplier, z * multiplier
            );
        }
    }
    
    // make sure that the portal rendering basic projection matrix is right
    // the basic projection matrix does not contain view bobbing
    @Redirect(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lorg/joml/Matrix4f;",
            ordinal = 0
        )
    )
    private Matrix4f redirectGetBasicProjectionMatrix(GameRenderer instance, double fov) {
        if (PortalRendering.isRendering()) {
            if (RenderStates.basicProjectionMatrix != null) {
                // replace the basic projection matrix
                return RenderStates.basicProjectionMatrix;
            }
            else {
                Helper.err("projection matrix state abnormal");
            }
        }
        
        Matrix4f result = instance.getProjectionMatrix(fov);
        RenderStates.basicProjectionMatrix = result;
        
        return result;
    }
    
    @Override
    public void setLightmapTextureManager(LightTexture manager) {
        lightTexture = manager;
    }
    
    @Override
    public boolean getDoRenderHand() {
        return renderHand;
    }
    
    @Override
    public void setCamera(Camera camera_) {
        mainCamera = camera_;
    }
    
    @Override
    public void setIsRenderingPanorama(boolean cond) {
        panoramicMode = cond;
    }
    
    @Override
    public void portal_bobView(PoseStack matrixStack, float tickDelta) {
        bobView(matrixStack, tickDelta);
    }
}
