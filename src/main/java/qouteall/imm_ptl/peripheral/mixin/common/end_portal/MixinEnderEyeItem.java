package qouteall.imm_ptl.peripheral.mixin.common.end_portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.EndPortalEntity;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

@Mixin(EnderEyeItem.class)
public class MixinEnderEyeItem {
    @Inject(method = "Lnet/minecraft/world/item/EnderEyeItem;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
    private void onUseOnBlock(
        UseOnContext itemUsageContext_1,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (IPGlobal.endPortalMode != IPGlobal.EndPortalMode.vanilla) {
            cir.setReturnValue(myUseOnBlock(itemUsageContext_1));
            cir.cancel();
        }
    }
    
    private InteractionResult myUseOnBlock(UseOnContext itemUsageContext) {
        Level world = itemUsageContext.getLevel();
        BlockPos blockPos = itemUsageContext.getClickedPos();
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.getBlock() == Blocks.END_PORTAL_FRAME &&
            !blockState.getValue(EndPortalFrameBlock.HAS_EYE)) {
            if (world.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            else {
                BlockState blockState_2 = (BlockState) blockState.setValue(
                    EndPortalFrameBlock.HAS_EYE,
                    true
                );
                Block.pushEntitiesUp(blockState, blockState_2, world, blockPos);
                world.setBlock(blockPos, blockState_2, 2);
                world.updateNeighbourForOutputSignal(blockPos, Blocks.END_PORTAL_FRAME);
                itemUsageContext.getItemInHand().shrink(1);
                world.levelEvent(1503, blockPos, 0);
                BlockPattern.BlockPatternMatch pattern =
                    EndPortalFrameBlock.getOrCreatePortalShape().find(world, blockPos);
                if (pattern != null) {
                    BlockPos blockPos_2 = pattern.getFrontTopLeft().offset(-3, 0, -3);
                    
                    for (int dx = 0; dx < 3; ++dx) {
                        for (int dz = 0; dz < 3; ++dz) {
                            world.setBlock(
                                blockPos_2.offset(dx, 0, dz),
                                PortalPlaceholderBlock.instance.defaultBlockState().setValue(
                                    PortalPlaceholderBlock.AXIS, Direction.Axis.Y
                                ),
                                2
                            );
                        }
                    }
                    
                    world.globalLevelEvent(1038, blockPos_2.offset(1, 0, 1), 0);
                    
                    EndPortalEntity.onEndPortalComplete(((ServerLevel) world), Vec3.atLowerCornerOf(pattern.getFrontTopLeft()).add(-1.5, 0.5, -1.5));
                }
                
                return InteractionResult.SUCCESS;
            }
        }
        else {
            return InteractionResult.PASS;
        }
    }
}
