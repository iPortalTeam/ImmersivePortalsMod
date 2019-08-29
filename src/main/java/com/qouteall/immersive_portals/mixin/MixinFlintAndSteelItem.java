package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.portal.NetherPortalGenerator;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class MixinFlintAndSteelItem {
    @Inject(method = "useOnBlock", at = @At("HEAD"))
    private void onUseFlintAndSteel(
        ItemUsageContext itemUsageContext_1,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        IWorld world = itemUsageContext_1.getWorld();
        if (!world.isClient()) {
            BlockPos blockPos_1 = itemUsageContext_1.getBlockPos();
            BlockPos firePos = blockPos_1.offset(itemUsageContext_1.getSide());
            NetherPortalGenerator.onFireLit(
                ((ServerWorld) world),
                firePos
            );
        }
    }
}
