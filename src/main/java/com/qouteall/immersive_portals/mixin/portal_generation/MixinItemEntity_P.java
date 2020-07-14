package com.qouteall.immersive_portals.mixin.portal_generation;

import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity_P {
    @Shadow public abstract ItemStack getStack();
    
    @Inject(
        method = "tick",
        at = @At("RETURN")
    )
    private void onItemTickEnded(CallbackInfo ci) {
        CustomPortalGenManagement.onItemTick((ItemEntity) (Object) this);
    }
}
