package qouteall.imm_ptl.core.mixin.client.interaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_B {
    
    //do not update target when rendering portal
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V", at = @At("HEAD"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            if (PortalRendering.isRendering()) {
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V", at = @At("RETURN"))
    private void onUpdateTargetedEntityFinish(float tickDelta, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            BlockManipulationClient.updatePointedBlock(tickDelta);
        }
    }
}
