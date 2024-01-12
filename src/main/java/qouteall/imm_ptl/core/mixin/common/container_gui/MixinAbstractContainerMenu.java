package qouteall.imm_ptl.core.mixin.common.container_gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;

@Mixin(AbstractContainerMenu.class)
public class MixinAbstractContainerMenu {
    @WrapOperation(
        method = "method_17696",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;distanceToSqr(DDD)D"
        )
    )
    private static double wrapDistanceToSqr(
        Player player, double x, double y, double z,
        Operation<Double> operation,
        @Local(argsOnly = true) Level world, @Local(argsOnly = true) BlockPos blockPos
    ) {
        double dist = operation.call(player, x, y, z);
        if (dist < 64) {
            return dist;
        }
        
        if (BlockManipulationServer.validateReach(player, world, blockPos)) {
            return 0;
        }
        else {
            return dist;
        }
    }
}
