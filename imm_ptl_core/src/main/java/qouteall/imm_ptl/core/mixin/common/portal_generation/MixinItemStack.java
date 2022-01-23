package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManagement;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(
        method = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onUseOnBlockEnded(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        CustomPortalGenManagement.onItemUse(context, cir.getReturnValue());
    }
}
