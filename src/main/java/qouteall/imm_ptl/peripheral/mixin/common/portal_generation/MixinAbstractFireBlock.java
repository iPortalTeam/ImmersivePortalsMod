package qouteall.imm_ptl.peripheral.mixin.common.portal_generation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;

import java.util.Optional;

@Mixin(BaseFireBlock.class)
public class MixinAbstractFireBlock {
    @Redirect(
        method = "Lnet/minecraft/world/level/block/BaseFireBlock;onPlace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/portal/PortalShape;findEmptyPortalShape(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction$Axis;)Ljava/util/Optional;"
        )
    )
    Optional<PortalShape> redirectCreateAreaHelper(LevelAccessor worldAccess, BlockPos blockPos, Direction.Axis axis) {
        if (IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.disabled) {
            return Optional.empty();
        }
        
        if (IPGlobal.netherPortalMode == IPGlobal.NetherPortalMode.vanilla) {
            return PortalShape.findEmptyPortalShape(worldAccess, blockPos, axis);
        }
        
        if (isNearObsidian(worldAccess, blockPos)) {
            IntrinsicPortalGeneration.onFireLitOnObsidian(
                ((ServerLevel) worldAccess),
                blockPos,
                null
            );
        }
        
        return Optional.empty();
    }
    
    private static boolean isNearObsidian(LevelAccessor access, BlockPos blockPos) {
        for (Direction value : Direction.values()) {
            if (O_O.isObsidian(access.getBlockState(blockPos.relative(value)))) {
                return true;
            }
        }
        return false;
    }
    
    // allow lighting fire on the side of obsidian
    // for lighting horizontal portals
    @Redirect(
        method = "Lnet/minecraft/world/level/block/BaseFireBlock;isPortal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z",
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
