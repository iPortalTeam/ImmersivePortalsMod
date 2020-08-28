package com.qouteall.immersive_portals;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class ClippedBlockView implements BlockView {
    public BlockView delegate;
    public Vec3d clipPos;
    public Vec3d contentDirection;
    public Entity raytraceDefaultEntity;
    
    public ClippedBlockView(BlockView delegate, Vec3d clipPos, Vec3d contentDirection, Entity raytraceDefaultEntity) {
        this.delegate = delegate;
        this.clipPos = clipPos;
        this.contentDirection = contentDirection;
        this.raytraceDefaultEntity = raytraceDefaultEntity;
    }
    
    public boolean isClipped(BlockPos pos) {
        return Vec3d.ofCenter(pos).subtract(clipPos).dotProduct(contentDirection) < 0;
    }
    
    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return delegate.getBlockEntity(pos);
    }
    
    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (isClipped(pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return delegate.getBlockState(pos);
    }
    
    @Override
    public FluidState getFluidState(BlockPos pos) {
        if (isClipped(pos)) {
            return Fluids.EMPTY.getDefaultState();
        }
        
        return delegate.getFluidState(pos);
    }
    
    @Override
    public int getLuminance(BlockPos pos) {
        return delegate.getLuminance(pos);
    }
    
    @Override
    public int getMaxLightLevel() {
        return delegate.getMaxLightLevel();
    }
    
    @Override
    public int getHeight() {
        return delegate.getHeight();
    }
    
    @Override
    public Stream<BlockState> method_29546(Box box) {
        return delegate.method_29546(box);
    }
    
    @Override
    public BlockHitResult raycast(RaycastContext context) {
        Vec3d delta = context.getEnd().subtract(context.getStart());
        double t = Helper.getCollidingT(
            clipPos,
            contentDirection,
            context.getStart(),
            delta
        );
        Vec3d startPos = context.getStart().add(delta.multiply(t));
    
        return raycast(new RaycastContext(
            startPos,
            context.getEnd(),
            RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE,
            raytraceDefaultEntity
        ));
    }
    
    @Nullable
    @Override
    public BlockHitResult raycastBlock(Vec3d start, Vec3d end, BlockPos pos, VoxelShape shape, BlockState state) {
        return delegate.raycastBlock(start, end, pos, shape, state);
    }
    
}
