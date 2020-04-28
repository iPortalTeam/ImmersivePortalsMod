package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.ducks.omgmixinsuxx;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RayTraceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockView.class)
public interface MixinBlockView extends BlockView {
    /**
     * @author LoganDark
     * @reason Mixin doesn't let me inject.
     */
    @Overwrite
    default BlockHitResult rayTrace(RayTraceContext context) {
        return BlockView.rayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = this.getBlockState(blockPos);
            FluidState fluidState = this.getFluidState(blockPos);
            Vec3d vec3d = rayTraceContext.getStart();
            Vec3d vec3d2 = rayTraceContext.getEnd();
            VoxelShape voxelShape = rayTraceContext.getBlockShape(blockState, this, blockPos);
            BlockHitResult blockHitResult = omgmixinsuxx.onRayTraceBlock(this, vec3d, vec3d2, blockPos, voxelShape, blockState);
            VoxelShape voxelShape2 = rayTraceContext.getFluidShape(fluidState, this, blockPos);
            BlockHitResult blockHitResult2 = voxelShape2.rayTrace(vec3d, vec3d2, blockPos);
            double d = blockHitResult == null ? Double.MAX_VALUE : rayTraceContext.getStart().squaredDistanceTo(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : rayTraceContext.getStart().squaredDistanceTo(blockHitResult2.getPos());
            return d <= e ? blockHitResult : blockHitResult2;
        }, (rayTraceContext) -> {
            Vec3d vec3d = rayTraceContext.getStart().subtract(rayTraceContext.getEnd());
            return BlockHitResult.createMissed(rayTraceContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), new BlockPos(rayTraceContext.getEnd()));
        });
    }
}
