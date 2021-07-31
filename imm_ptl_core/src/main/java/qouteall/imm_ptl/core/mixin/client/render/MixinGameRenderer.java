package qouteall.imm_ptl.core.mixin.client.render;

import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPModMainClient;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.render.CrossPortalViewRendering;
import qouteall.imm_ptl.core.render.GuiPortalRendering;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IEGameRenderer {
    @Shadow
    @Final
    @Mutable
    private LightmapTextureManager lightmapTextureManager;
    
    @Shadow
    private boolean renderHand;
    @Shadow
    @Final
    @Mutable
    private Camera camera;
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    @Shadow
    private int ticks;
    
    @Shadow
    private boolean renderingPanorama;
    
    @Shadow
    public abstract void loadProjectionMatrix(Matrix4f matrix4f);
    
    @Shadow
    protected abstract void bobView(MatrixStack matrices, float f);
    
    @Inject(method = "render", at = @At("HEAD"))
    private void onFarBeforeRendering(
        float tickDelta,
        long nanoTime,
        boolean renderWorldIn,
        CallbackInfo ci
    ) {
        IPGlobal.preTotalRenderTaskList.processTasks();
        if (client.world == null) {
            return;
        }
        RenderStates.updatePreRenderInfo(tickDelta);
        IPCGlobal.clientTeleportationManager.manageTeleportation(RenderStates.tickDelta);
        IPGlobal.preGameRenderSignal.emit();
        if (IPCGlobal.earlyClientLightUpdate) {
            MyRenderHelper.earlyUpdateLight();
        }
        
        RenderStates.frameIndex++;
    }
    
    //before rendering world (not triggered when rendering portal)
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"
        )
    )
    private void onBeforeRenderingCenter(
        float float_1,
        long long_1,
        boolean boolean_1,
        CallbackInfo ci
    ) {
        IPModMainClient.switchToCorrectRenderer();
        
        IPCGlobal.renderer.prepareRendering();
    }
    
    //after rendering world (not triggered when rendering portal)
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V",
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
    }
    
    //special rendering in third person view
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(FJLnet/minecraft/client/util/math/MatrixStack;)V"
        )
    )
    private void redirectRenderingWorld(
        GameRenderer gameRenderer, float tickDelta, long limitTime, MatrixStack matrix
    ) {
        if (CrossPortalViewRendering.renderCrossPortalView()) {
            return;
        }
        
        gameRenderer.renderWorld(tickDelta, limitTime, matrix);
    }
    
    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void onRenderCenterEnded(
        float float_1,
        long long_1,
        MatrixStack matrixStack_1,
        CallbackInfo ci
    ) {
        IPCGlobal.renderer.onHandRenderingEnded(matrixStack_1);
    }
    
    @Inject(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lnet/minecraft/util/math/Matrix4f;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRightBeforeHandRendering(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        IPCGlobal.renderer.onBeforeHandRendering(matrix);
    }
    
    //resize all world renderers when resizing window
    @Inject(method = "onResized", at = @At("RETURN"))
    private void onOnResized(int int_1, int int_2, CallbackInfo ci) {
        if (ClientWorldLoader.getIsInitialized()) {
            ClientWorldLoader.worldRendererMap.values().stream()
                .filter(
                    worldRenderer -> worldRenderer != client.worldRenderer
                )
                .forEach(
                    worldRenderer -> worldRenderer.onResized(int_1, int_2)
                );
        }
    }
    
    private static boolean portal_isRenderingHand = false;
    
    @Inject(method = "renderHand", at = @At("HEAD"))
    private void onRenderHandBegins(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        portal_isRenderingHand = true;
    }
    
    @Inject(method = "renderHand", at = @At("RETURN"))
    private void onRenderHandEnds(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        portal_isRenderingHand = false;
    }
    
    // do not translate
    @Redirect(
        method = "bobView",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V"
        )
    )
    private void redirectBobViewTranslate(MatrixStack matrixStack, double x, double y, double z) {
        if (portal_isRenderingHand) {
            matrixStack.translate(x, y, z);
        }
        else if (TransformationManager.isCalculatingViewBobbingOffset) {
            matrixStack.translate(
                x * RenderStates.viewBobFactor,
                y * RenderStates.viewBobFactor,
                z * RenderStates.viewBobFactor
            );
        }

//        double viewBobFactor = portal_isRenderingHand ? 1 : RenderStates.viewBobFactor;
//
    }
    
    @Redirect(
        method = "renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;loadProjectionMatrix(Lnet/minecraft/util/math/Matrix4f;)V"
        )
    )
    private void redirectLoadProjectionMatrix(GameRenderer gameRenderer, Matrix4f matrix4f) {
        if (PortalRendering.isRendering()) {
            //load recorded projection matrix
            loadProjectionMatrix(RenderStates.projectionMatrix);
        }
        else {
            //load projection matrix normally
            loadProjectionMatrix(matrix4f);
            
            //record projection matrix
            if (RenderStates.projectionMatrix == null) {
                RenderStates.projectionMatrix = matrix4f;
            }
        }
    }
    
    @Override
    public void setLightmapTextureManager(LightmapTextureManager manager) {
        lightmapTextureManager = manager;
    }
    
    @Override
    public boolean getDoRenderHand() {
        return renderHand;
    }
    
    @Override
    public void setCamera(Camera camera_) {
        camera = camera_;
    }
    
    @Override
    public void setIsRenderingPanorama(boolean cond) {
        renderingPanorama = cond;
    }
    
    @Override
    public void portal_bobView(MatrixStack matrixStack, float tickDelta) {
        bobView(matrixStack, tickDelta);
    }
}
