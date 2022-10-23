package qouteall.imm_ptl.core.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IEMinecraftClient {
    @Final
    @Shadow
    @Mutable
    private RenderTarget mainRenderTarget;
    
    @Shadow
    public Screen screen;
    
    @Mutable
    @Shadow
    @Final
    public LevelRenderer levelRenderer;
    
    @Shadow
    private static int fps;
    
    @Shadow
    public abstract ProfilerFiller getProfiler();
    
    @Shadow
    @Nullable
    public ClientLevel level;
    
    @Mutable
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    
    // this happens after ticking client world and entities
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;tick(Ljava/util/function/BooleanSupplier;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterClientTick(CallbackInfo ci) {
        getProfiler().push("imm_ptl_tick_signal");
        IPGlobal.postClientTickSignal.emit();
        getProfiler().pop();
    
        // immediately after ticking
        RenderStates.tickDelta = 0;
        StableClientTimer.update(level.getGameTime(), RenderStates.tickDelta);
        IPCGlobal.clientTeleportationManager.manageTeleportation();
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;runTick(Z)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;fps:I",
            shift = At.Shift.AFTER
        )
    )
    private void onSnooperUpdate(boolean tick, CallbackInfo ci) {
        ClientPerformanceMonitor.updateEverySecond(fps);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/Minecraft;updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V",
        at = @At("HEAD")
    )
    private void onSetWorld(ClientLevel clientWorld_1, CallbackInfo ci) {
        IPGlobal.clientCleanupSignal.emit();
    }
    
    //avoid messing up rendering states in fabulous
    @Inject(method = "Lnet/minecraft/client/Minecraft;useShaderTransparency()Z", at = @At("HEAD"), cancellable = true)
    private static void onIsFabulousGraphicsOrBetter(CallbackInfoReturnable<Boolean> cir) {
        if (WorldRenderInfo.isRendering()) {
            cir.setReturnValue(false);
        }
    }
    
    @Override
    public void setFrameBuffer(RenderTarget buffer) {
        mainRenderTarget = buffer;
    }
    
    @Override
    public Screen getCurrentScreen() {
        return screen;
    }
    
    @Override
    public void setWorldRenderer(LevelRenderer r) {
        levelRenderer = r;
    }
    
    @Override
    public void ip_setRenderBuffers(RenderBuffers arg) {
        renderBuffers = arg;
    }
}
