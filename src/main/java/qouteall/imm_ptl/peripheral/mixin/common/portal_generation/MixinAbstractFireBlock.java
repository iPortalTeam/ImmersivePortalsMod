package qouteall.imm_ptl.peripheral.mixin.common.portal_generation;

import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;
import qouteall.imm_ptl.core.IPGlobal;
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
            target = "Lnet/minecraft/world/dimension/AreaHelper;getNewPortal(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction$Axis;)Ljava/util/Optional;"
        )
    )
    Optional<AreaHelper> redirectCreateAreaHelper(WorldAccess worldAccess, BlockPos blockPos, Direction.Axis axis) {
        if (IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.disabled) {
            return Optional.empty();
        }
        
        if (IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.vanilla) {
            return AreaHelper.getNewPortal(worldAccess, blockPos, axis);
        }
        
        if (isNearObsidian(worldAccess, blockPos)) {
            IntrinsicPortalGeneration.onFireLitOnObsidian(
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
    
    // allow lighting fire on the side of obsidian
    // for lighting horizontal portals
    @Redirect(
        method = "shouldLightPortalAt",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;isPresent()Z"
        )
    )
    private static boolean redirectIsPresent(Optional optional) {
        if (IPGlobal.netherPortalMode != IPGlobal.NetherPortalMode.vanilla) {
            return true;
        }
        else {
            return optional.isPresent();
        }
    }
}
