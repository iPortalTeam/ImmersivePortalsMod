package qouteall.imm_ptl.core.platform_specific.mixin.client;

import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ClientWorldLoader;

@Mixin(InvalidateRenderStateCallback.class)
public interface MixinFabricInvalidateRenderStateCallback {
    @Inject(
        method = "lambda$static$0",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onInvokeEvent(InvalidateRenderStateCallback[] event, CallbackInfo ci) {
        if (ClientWorldLoader.getIsCreatingClientWorld()) {
            ci.cancel();
        }
    }
}
