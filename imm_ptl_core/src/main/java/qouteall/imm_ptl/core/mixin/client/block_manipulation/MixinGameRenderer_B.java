package qouteall.imm_ptl.core.mixin.client.block_manipulation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationClient;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

@Mixin(GameRenderer.class)
public class MixinGameRenderer_B {
    
    //do not update target when rendering portal
    @Inject(method = "updateTargetedEntity", at = @At("HEAD"), cancellable = true)
    private void onUpdateTargetedEntity(float tickDelta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null) {
            if (PortalRendering.isRendering()) {
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "updateTargetedEntity", at = @At("RETURN"))
    private void onUpdateTargetedEntityFinish(float tickDelta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null) {
            BlockManipulationClient.updatePointedBlock(tickDelta);
        }
    }
}
