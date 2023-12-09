package qouteall.imm_ptl.core.mixin.common.collision;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.portal.PortalPlaceholderBlock;

@Mixin(ThrowableProjectile.class)
public class MixinThrowableProjectile {
    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private BlockState wrapGetBlockState(
        Level level, BlockPos blockPos, Operation<BlockState> original,
        @Share("immptl_shouldCancelHit") LocalBooleanRef shouldCancelHit
    ) {
        BlockState blockState = original.call(level, blockPos);
        
        if (blockState.getBlock() == PortalPlaceholderBlock.instance) {
            shouldCancelHit.set(true);
        }
        else {
            shouldCancelHit.set(false);
        }
        
        return blockState;
    }
    
    @WrapWithCondition(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"
        )
    )
    private boolean wrapOnHit(
        ThrowableProjectile throwableProjectile, HitResult hitResult,
        @Share("immptl_shouldCancelHit") LocalBooleanRef shouldCancelHit
    ) {
        return !shouldCancelHit.get();
    }
}
