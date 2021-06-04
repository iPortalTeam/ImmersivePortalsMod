package qouteall.imm_ptl.core.mixin.common.portal_generation;

import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(
        method = "useOnBlock",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onUseOnBlockEnded(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        CustomPortalGenManagement.onItemUse(context, cir.getReturnValue());
    }
}
