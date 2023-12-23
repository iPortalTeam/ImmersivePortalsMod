package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManager;

@Mixin(ItemStack.class)
public class MixinItemStack {
    @Inject(
        method = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At("RETURN")
    )
    private void onUseOnBlockEnded(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level world = context.getLevel();
        
        if (!world.isClientSide()) {
            CustomPortalGenManager customPortalGenManager =
                IPPerServerInfo.of(world.getServer()).customPortalGenManager;
            
            if (customPortalGenManager != null) {
                customPortalGenManager.onItemUse(context, cir.getReturnValue());
            }
        }
    }
}
