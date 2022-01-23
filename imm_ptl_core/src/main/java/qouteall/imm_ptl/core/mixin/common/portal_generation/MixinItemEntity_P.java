package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManagement;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity_P {
    @Shadow
    public abstract ItemStack getItem();
    
    @Inject(
        method = "Lnet/minecraft/world/entity/item/ItemEntity;tick()V",
        at = @At("TAIL")
    )
    private void onItemTickEnded(CallbackInfo ci) {
        ItemEntity this_ = (ItemEntity) (Object) this;
        if (this_.isRemoved()) {
            return;
        }
        
        if (this_.level.isClientSide()) {
            return;
        }
        
        this_.level.getProfiler().push("imm_ptl_item_tick");
        CustomPortalGenManagement.onItemTick(this_);
        this_.level.getProfiler().pop();
    }
}
