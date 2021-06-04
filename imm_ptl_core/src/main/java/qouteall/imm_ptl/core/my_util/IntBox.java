package qouteall.imm_ptl.core.my_util;

import qouteall.imm_ptl.core.Helper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntBox {
    
    public final BlockPos l;
    public final BlockPos h;
    
    public IntBox(BlockPos l, BlockPos h) {
        this.l = Helper.min(l, h);
        this.h = Helper.max(l, h);
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
    
    public IntBox expandOrShrink(Vec3i offset) {
        return new IntBox(
            l.subtract(offset),
            h.add(offset)
        );
    }
    
    public IntBox getExpanded(Direction.Axis axis, int n) {
        
        
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
        
        
        return h.add(1, 1, 1).subtract(l);
    }
    
    public IntBox getSurfaceLayer(
        Direction.Axis axis,
        Direction.AxisDirection axisDirection
    ) {
        
        
        if (axisDirection == Direction.AxisDirection.NEGATIVE) {
            IntBox result = new IntBox(
                l,
                new BlockPos(
                    (axis == Direction.Axis.X ? l : h).getX(),
                    (axis == Direction.Axis.Y ? l : h).getY(),
                    (axis == Direction.Axis.Z ? l : h).getZ()
                )
            );
            
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
    
    @Nullable
    public static IntBox getIntersect(
        IntBox a,
        IntBox b
    ) {
        BlockPos l = Helper.max(a.l, b.l);
        BlockPos h = Helper.min(a.h, b.h);
    
        if (l.getX() > h.getX()) {
            return null;
        }
        if (l.getY() > h.getY()) {
            return null;
        }
        if (l.getZ() > h.getZ()) {
            return null;
        }
    
        return new IntBox(l, h);
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
            Arrays.stream(array).filter(intBox -> true)
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
        Validate.isTrue(thisSize.getX() >= subBoxSize.getX());
        Validate.isTrue(thisSize.getY() >= subBoxSize.getY());
        Validate.isTrue(thisSize.getZ() >= subBoxSize.getZ());
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
        
        
        return pos.getX() >= l.getX() &&
            pos.getX() <= h.getX() &&
            pos.getY() >= l.getY() &&
            pos.getY() <= h.getY() &&
            pos.getZ() >= l.getZ() &&
            pos.getZ() <= h.getZ();
    }
    
    @Override
    public String toString() {
        return String.format(
            "(%d %d %d)-(%d %d %d)",
            l.getX(), l.getY(), l.getZ(),
            h.getX(), h.getY(), h.getZ()
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntBox intBox = (IntBox) o;
        return l.equals(intBox.l) &&
            h.equals(intBox.h);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(l, h);
    }
}
