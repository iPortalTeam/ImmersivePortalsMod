package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.lag_spike_fix.GLResourceCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GlStateManager.class, remap = false)
public abstract class MixinGlStateManager {
    
    @Shadow
    public static void _disableCull() {
        throw new RuntimeException();
    }
    
    @Inject(
        method = "_enableCull",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onEnableCull(CallbackInfo ci) {
        if (RenderStates.shouldForceDisableCull) {
            _disableCull();
            ci.cancel();
        }
    }
    
    @Inject(
        method = "_glGenBuffers",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGenBuffers(CallbackInfoReturnable<Integer> cir) {
        if (IPGlobal.cacheGlBuffer) {
            cir.setReturnValue(GLResourceCache.bufferCache.getNewResourceId());
            cir.cancel();
        }
    }
    
    @Inject(
        method = "_glGenVertexArrays",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGenVertexArrays(CallbackInfoReturnable<Integer> cir) {
        if (IPGlobal.cacheGlBuffer) {
            cir.setReturnValue(GLResourceCache.vertexArrayCache.getNewResourceId());
            cir.cancel();
        }
    }
}
