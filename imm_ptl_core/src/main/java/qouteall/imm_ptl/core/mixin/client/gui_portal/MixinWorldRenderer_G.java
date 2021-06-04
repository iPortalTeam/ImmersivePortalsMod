package qouteall.imm_ptl.core.mixin.client.gui_portal;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_G {
//    @Inject(method = "canDrawEntityOutlines", at = @At("HEAD"), cancellable = true)
//    private void onCanDrawEntityOutlines(CallbackInfoReturnable<Boolean> cir) {
//        if (GuiPortalRendering.isRendering()) {
//            cir.setReturnValue(false);
//        }
//    }
}
