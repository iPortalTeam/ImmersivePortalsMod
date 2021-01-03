package com.qouteall.imm_ptl_peripheral.mixin.common.portal_generation;

import com.qouteall.imm_ptl_peripheral.PeripheralModMain;
import com.qouteall.imm_ptl_peripheral.portal_generation.IntrinsicPortalGeneration;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.portal.BreakableMirror;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class MixinFlintAndSteelItem {
    //TODO make it possible to ignite on horizontal obsidian face

//    @Inject(
//        method = "canIgnite",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private static void onCanIgnite(
//        BlockState block,
//        WorldAccess world,
//        BlockPos pos,
//        CallbackInfoReturnable<Boolean> cir
//    ) {
//        for (Direction direction : Direction.values()) {
//            if (O_O.isObsidian(world, pos.offset(direction))) {
//                if (block.isAir()) {
//                    cir.setReturnValue(true);
//                    cir.cancel();
//                }
//            }
//        }
//    }
    
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onUseFlintAndSteel(
        ItemUsageContext context,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (Global.netherPortalMode == Global.NetherPortalMode.vanilla) {
            return;
        }
        
        WorldAccess world = context.getWorld();
        if (!world.isClient()) {
            BlockPos targetPos = context.getBlockPos();
            Direction side = context.getSide();
            BlockPos firePos = targetPos.offset(side);
            BlockState targetBlockState = world.getBlockState(targetPos);
            Block targetBlock = targetBlockState.getBlock();
            if (BreakableMirror.isGlass(((World) world), targetPos)) {
                BreakableMirror mirror = BreakableMirror.createMirror(
                    ((ServerWorld) world), targetPos, side
                );
                cir.setReturnValue(ActionResult.SUCCESS);
            }
            else if (targetBlock == PeripheralModMain.portalHelperBlock) {
                boolean result = IntrinsicPortalGeneration.activatePortalHelper(
                    ((ServerWorld) world),
                    firePos
                );
            }
        }
    }
}
