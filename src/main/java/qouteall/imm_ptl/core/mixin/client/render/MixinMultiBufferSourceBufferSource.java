package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

@Mixin(MultiBufferSource.BufferSource.class)
public class MixinMultiBufferSourceBufferSource {
    @Inject(
        method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("HEAD")
    )
    private void onBeginDraw(RenderType layer, CallbackInfo ci) {
        if (PortalRendering.isRenderingOddNumberOfMirrors()) {
            RenderStates.shouldForceDisableCull = true;
            GlStateManager._disableCull();
        }
    }
    
    @Inject(
        method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("RETURN")
    )
    private void onEndDraw(RenderType layer, CallbackInfo ci) {
        if (RenderStates.shouldForceDisableCull) {
            RenderStates.shouldForceDisableCull = false;
            GlStateManager._enableCull();
        }
    }
}
