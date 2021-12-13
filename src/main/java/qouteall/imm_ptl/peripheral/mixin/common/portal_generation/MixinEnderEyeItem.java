package qouteall.imm_ptl.peripheral.mixin.common.portal_generation;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndPortalFrameBlock;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

@Mixin(EnderEyeItem.class)
public class MixinEnderEyeItem {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void onUseOnBlock(
        ItemUsageContext itemUsageContext_1,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (IPGlobal.endPortalMode != IPGlobal.EndPortalMode.vanilla) {
            cir.setReturnValue(myUseOnBlock(itemUsageContext_1));
            cir.cancel();
        }
    }
    
    private ActionResult myUseOnBlock(ItemUsageContext itemUsageContext) {
        World world = itemUsageContext.getWorld();
        BlockPos blockPos = itemUsageContext.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.getBlock() == Blocks.END_PORTAL_FRAME &&
            !blockState.get(EndPortalFrameBlock.EYE)) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            else {
                BlockState blockState_2 = (BlockState) blockState.with(
                    EndPortalFrameBlock.EYE,
                    true
                );
                Block.pushEntitiesUpBeforeBlockChange(blockState, blockState_2, world, blockPos);
                world.setBlockState(blockPos, blockState_2, 2);
                world.updateComparators(blockPos, Blocks.END_PORTAL_FRAME);
                itemUsageContext.getStack().decrement(1);
                world.syncWorldEvent(1503, blockPos, 0);
                BlockPattern.Result pattern =
                    EndPortalFrameBlock.getCompletedFramePattern().searchAround(world, blockPos);
                if (pattern != null) {
                    BlockPos blockPos_2 = pattern.getFrontTopLeft().add(-3, 0, -3);
                    
                    for (int dx = 0; dx < 3; ++dx) {
                        for (int dz = 0; dz < 3; ++dz) {
                            world.setBlockState(
                                blockPos_2.add(dx, 0, dz),
                                PortalPlaceholderBlock.instance.getDefaultState().with(
                                    PortalPlaceholderBlock.AXIS, Direction.Axis.Y
                                ),
                                2
                            );
                        }
                    }
                    
                    world.syncGlobalEvent(1038, blockPos_2.add(1, 0, 1), 0);
                    
                    EndPortalEntity.onEndPortalComplete(((ServerWorld) world), Vec3d.of(pattern.getFrontTopLeft()).add(-1.5, 0.5, -1.5));
                }
                
                return ActionResult.SUCCESS;
            }
        }
        else {
            return ActionResult.PASS;
        }
    }
}
