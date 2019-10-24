package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.portal.BreakableMirror;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGenerator;
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
        ItemUsageContext context,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        IWorld world = context.getWorld();
        if (!world.isClient()) {
            BlockPos blockPos_1 = context.getBlockPos();
            BlockPos firePos = blockPos_1.offset(context.getSide());
            NetherPortalGenerator.NetherPortalGeneratedInformation info =
                NetherPortalGenerator.onFireLit(((ServerWorld) world), firePos);
            if (info == null) {
                BreakableMirror mirror = BreakableMirror.createMirror(
                    ((ServerWorld) world), context.getBlockPos(), context.getSide()
                );
                if (mirror != null) {
                    context.getStack().damage(1, context.getPlayer(),
                        playerEntity_1x -> playerEntity_1x.sendToolBreakStatus(context.getHand())
                    );
                }
            }
        }
    }
}
