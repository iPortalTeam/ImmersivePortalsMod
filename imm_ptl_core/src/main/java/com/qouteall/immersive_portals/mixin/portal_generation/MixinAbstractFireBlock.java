package com.qouteall.immersive_portals.mixin.portal_generation;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.portal.nether_portal.NetherPortalGeneration;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.AreaHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(AbstractFireBlock.class)
public class MixinAbstractFireBlock {
    @Redirect(
        method = "onBlockAdded",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/AreaHelper;method_30485(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction$Axis;)Ljava/util/Optional;"
        )
    )
    Optional<AreaHelper> redirectCreateAreaHelper(WorldAccess worldAccess, BlockPos blockPos, Direction.Axis axis) {
        if (isNearObsidian(worldAccess, blockPos)) {
            NetherPortalGeneration.onFireLitOnObsidian(
                ((ServerWorld) worldAccess),
                blockPos
            );
        }
        
        return Optional.empty();
    }
    
    private static boolean isNearObsidian(WorldAccess access, BlockPos blockPos) {
        for (Direction value : Direction.values()) {
            if (O_O.isObsidian(access.getBlockState(blockPos.offset(value)))) {
                return true;
            }
        }
        return false;
    }
}
