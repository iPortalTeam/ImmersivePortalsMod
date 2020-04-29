package com.qouteall.immersive_portals.my_util;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntBox {
    
    public final BlockPos l;
    public final BlockPos h;
    
    public IntBox(BlockPos l, BlockPos h) {
        this.l = l.toImmutable();
        this.h = h.toImmutable();
    }
    
    public static IntBox getBoxByBasePointAndSize(
        BlockPos areaSize,
        BlockPos blockPos
    ) {
        return new IntBox(
            blockPos,
            blockPos.add(areaSize).add(-1, -1, -1)
        );
    }
    
    public IntBox getSorted() {
        return new IntBox(
            Helper.min(l, h),
            Helper.max(l, h)
        );
    }
    
    public boolean isSorted() {
        return l.getX() <= h.getX() &&
            l.getY() <= h.getY() &&
            l.getZ() <= h.getZ();
    }
    
    public IntBox expandOrShrink(Vec3i offset) {
        assert isSorted();
        
        return new IntBox(
            l.subtract(offset),
            h.add(offset)
        );
    }
    
    public IntBox getExpanded(Direction.Axis axis, int n) {
        assert isSorted();
        
        return expandOrShrink(
            Helper.scale(
                Direction.get(
                    Direction.AxisDirection.POSITIVE, axis
                ).getVector(),
                n
            )
        );
    }
    
    public IntBox getExpanded(Direction direction, int n) {
        if (direction.getDirection() == Direction.AxisDirection.POSITIVE) {
            return new IntBox(
                l,
                h.add(Helper.scale(direction.getVector(), n))
            );
        }
        else {
            return new IntBox(
                l.add(Helper.scale(direction.getVector(), n)),
                h
            );
        }
    }
    
    public Stream<BlockPos> stream() {
        assert isSorted();
        
        return IntStream.range(l.getX(), h.getX() + 1).boxed().flatMap(
            x -> IntStream.range(l.getY(), h.getY() + 1).boxed().flatMap(
                y -> IntStream.range(l.getZ(), h.getZ() + 1).boxed().map(
                    z -> new BlockPos(x, y, z)
                )
            )
        );
    }
    
    //it will get only one mutable block pos object
    //don't store its reference. store its copy
    public Stream<BlockPos> fastStream() {
        return BlockPos.stream(l, h);
    }
    
    public BlockPos getSize() {
        assert isSorted();
        
        return h.add(1, 1, 1).subtract(l);
    }
    
    public IntBox getSurfaceLayer(
        Direction.Axis axis,
        Direction.AxisDirection axisDirection
    ) {
        assert isSorted();
        
        if (axisDirection == Direction.AxisDirection.NEGATIVE) {
            IntBox result = new IntBox(
                l,
                new BlockPos(
                    (axis == Direction.Axis.X ? l : h).getX(),
                    (axis == Direction.Axis.Y ? l : h).getY(),
                    (axis == Direction.Axis.Z ? l : h).getZ()
                )
            );
            assert result.isSorted();
            return result;
        }
        else {
            IntBox result = new IntBox(
                new BlockPos(
                    (axis == Direction.Axis.X ? h : l).getX(),
                    (axis == Direction.Axis.Y ? h : l).getY(),
                    (axis == Direction.Axis.Z ? h : l).getZ()
                ),
                h
            );
            assert result.isSorted();
            return result;
        }
    }
    
    public IntBox getSurfaceLayer(
        Direction facing
    ) {
        return getSurfaceLayer(
            facing.getAxis(),
            facing.getDirection()
        );
    }
    
    //@Nullable
    public static IntBox getIntersect(
        IntBox a,
        IntBox b
    ) {
        assert a.isSorted();
        assert b.isSorted();
        
        IntBox intersected = new IntBox(
            Helper.max(a.l, b.l),
            Helper.min(a.h, b.h)
        );
        
        if (!intersected.isSorted()) {
            return null;
        }
        else {
            return intersected;
        }
    }
    
    public IntBox map(
        Function<BlockPos, BlockPos> func1,
        Function<BlockPos, BlockPos> func2
    ) {
        return new IntBox(
            func1.apply(l),
            func2.apply(h)
        );
    }
    
    public BlockPos getCenter() {
        return Helper.divide(l.add(h), 2);
    }
    
    public Vec3d getCenterVec() {
        return new Vec3d(
            (l.getX() + h.getX() + 1) / 2.0,
            (l.getY() + h.getY() + 1) / 2.0,
            (l.getZ() + h.getZ() + 1) / 2.0
        );
    }
    
    public IntBox getAdjusted(
        int dxa, int dya, int dza,
        int dxb, int dyb, int dzb
    ) {
        return new IntBox(
            l.add(dxa, dya, dza),
            h.add(dxb, dyb, dzb)
        );
    }
    
    public Stream<BlockPos> forSixSurfaces(
        Function<Stream<IntBox>, Stream<IntBox>> mapper
    ) {
        assert isSorted();
        
        IntBox[] array = {
            getSurfaceLayer(Direction.DOWN),
            getSurfaceLayer(Direction.NORTH).getAdjusted(
                0, 1, 0,
                0, 0, 0
            ),
            getSurfaceLayer(Direction.SOUTH).getAdjusted(
                0, 1, 0,
                0, 0, 0
            ),
            getSurfaceLayer(Direction.WEST).getAdjusted(
                0, 1, 1,
                0, 0, -1
            ),
            getSurfaceLayer(Direction.EAST).getAdjusted(
                0, 1, 1,
                0, 0, -1
            ),
            getSurfaceLayer(Direction.UP).getAdjusted(
                1, 0, 1,
                -1, 0, -1
            )
        };
        
        return mapper.apply(
            Arrays.stream(array).filter(IntBox::isSorted)
        ).flatMap(
            IntBox::stream
        );
    }
    
    public IntBox getMoved(Vec3i offset) {
        return new IntBox(
            l.add(offset),
            h.add(offset)
        );
    }
    
    public static IntBox getContainingBox(
        IntBox box1,
        IntBox box2
    ) {
        assert box1.isSorted();
        assert box2.isSorted();
        
        return new IntBox(
            Helper.min(
                box1.l,
                box2.l
            ),
            Helper.max(
                box1.h,
                box2.h
            )
        );
    }
    
    public IntBox getSubBoxInCenter(BlockPos subBoxSize) {
        BlockPos thisSize = getSize();
        assert thisSize.getX() >= subBoxSize.getX();
        assert thisSize.getY() >= subBoxSize.getY();
        assert thisSize.getZ() >= subBoxSize.getZ();
        return getBoxByBasePointAndSize(
            subBoxSize,
            Helper.divide(thisSize.subtract(subBoxSize), 2).add(l)
        );
    }
    
    public BlockPos[] getEightVertices() {
        return new BlockPos[]{
            new BlockPos(l.getX(), l.getY(), l.getZ()),
            new BlockPos(l.getX(), l.getY(), h.getZ()),
            new BlockPos(l.getX(), h.getY(), l.getZ()),
            new BlockPos(l.getX(), h.getY(), h.getZ()),
            new BlockPos(h.getX(), l.getY(), l.getZ()),
            new BlockPos(h.getX(), l.getY(), h.getZ()),
            new BlockPos(h.getX(), h.getY(), l.getZ()),
            new BlockPos(h.getX(), h.getY(), h.getZ())
        };
    }
    
    public Box toRealNumberBox() {
        assert isSorted();
        return new Box(
            l.getX(),
            l.getY(),
            l.getZ(),
            h.getX() + 1,
            h.getY() + 1,
            h.getZ() + 1
        );
    }
    
    public IntBox getExpanded(BlockPos newPoint) {
        return new IntBox(
            Helper.min(
                l,
                newPoint
            ),
            Helper.max(
                h,
                newPoint
            )
        );
    }
    
    public boolean contains(BlockPos pos) {
        assert isSorted();
    
        return pos.getX() >= l.getX() &&
            pos.getX() <= h.getX() &&
            pos.getY() >= l.getY() &&
            pos.getY() <= h.getY() &&
            pos.getZ() >= l.getZ() &&
            pos.getZ() <= h.getZ();
    }
}
