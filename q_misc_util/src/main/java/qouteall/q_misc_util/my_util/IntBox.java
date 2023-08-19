package qouteall.q_misc_util.my_util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

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
    
    // the name is misleading.
    @Deprecated
    public static IntBox getBoxByBasePointAndSize(
        BlockPos areaSize,
        BlockPos blockPos
    ) {
        return fromBasePointAndSize(blockPos, areaSize);
    }
    
    public static IntBox fromBasePointAndSize(
        BlockPos blockPos,
        BlockPos areaSize
    ) {
        return new IntBox(
            blockPos,
            blockPos.offset(areaSize).offset(-1, -1, -1)
        );
    }
    
    public IntBox expandOrShrink(Vec3i offset) {
        return new IntBox(
            l.subtract(offset),
            h.offset(offset)
        );
    }
    
    public IntBox getExpanded(Direction.Axis axis, int n) {
        return expandOrShrink(
            Helper.scale(
                Direction.get(
                    Direction.AxisDirection.POSITIVE, axis
                ).getNormal(),
                n
            )
        );
    }
    
    public IntBox getExpanded(Direction direction, int n) {
        if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            return new IntBox(
                l,
                h.offset(Helper.scale(direction.getNormal(), n))
            );
        }
        else {
            return new IntBox(
                l.offset(Helper.scale(direction.getNormal(), n)),
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
        return BlockPos.betweenClosedStream(l, h);
    }
    
    public BlockPos getSize() {
        return h.offset(1, 1, 1).subtract(l);
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
            facing.getAxisDirection()
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
        return Helper.divide(l.offset(h), 2);
    }
    
    public Vec3 getCenterVec() {
        return new Vec3(
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
            l.offset(dxa, dya, dza),
            h.offset(dxb, dyb, dzb)
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
            l.offset(offset),
            h.offset(offset)
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
        return fromBasePointAndSize(Helper.divide(thisSize.subtract(subBoxSize), 2).offset(l), subBoxSize);
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
    
    public AABB toRealNumberBox() {
        
        return new AABB(
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
    
    public BlockPos selectCoordinateFromBox(boolean high) {
        return high ? h : l;
    }
    
    public IntBox[] get12Edges() {
        return new IntBox[]{
            new IntBox(
                selectCoordinateFromBox(false, false, false),
                selectCoordinateFromBox(false, false, true)
            ),
            new IntBox(
                selectCoordinateFromBox(false, true, false),
                selectCoordinateFromBox(false, true, true)
            ),
            new IntBox(
                selectCoordinateFromBox(true, false, false),
                selectCoordinateFromBox(true, false, true)
            ),
            new IntBox(
                selectCoordinateFromBox(true, true, false),
                selectCoordinateFromBox(true, true, true)
            ),
            
            new IntBox(
                selectCoordinateFromBox(false, false, false),
                selectCoordinateFromBox(false, true, false)
            ),
            new IntBox(
                selectCoordinateFromBox(false, false, true),
                selectCoordinateFromBox(false, true, true)
            ),
            new IntBox(
                selectCoordinateFromBox(true, false, false),
                selectCoordinateFromBox(true, true, false)
            ),
            new IntBox(
                selectCoordinateFromBox(true, false, true),
                selectCoordinateFromBox(true, true, true)
            ),
            
            new IntBox(
                selectCoordinateFromBox(false, false, false),
                selectCoordinateFromBox(true, false, false)
            ),
            new IntBox(
                selectCoordinateFromBox(false, false, true),
                selectCoordinateFromBox(true, false, true)
            ),
            new IntBox(
                selectCoordinateFromBox(false, true, false),
                selectCoordinateFromBox(true, true, false)
            ),
            new IntBox(
                selectCoordinateFromBox(false, true, true),
                selectCoordinateFromBox(true, true, true)
            )
        };
    }
    
    public BlockPos selectCoordinateFromBox(boolean xUp, boolean yUp, boolean zUp) {
        return new BlockPos(
            selectCoordinateFromBox(xUp).getX(),
            selectCoordinateFromBox(yUp).getY(),
            selectCoordinateFromBox(zUp).getZ()
        );
    }
    
    public static IntBox getBoxByPosAndSignedSize(
        BlockPos basePos,
        BlockPos signedSize
    ) {
        return new IntBox(
            basePos,
            new BlockPos(
                getEndCoordWithSignedSize(basePos.getX(), signedSize.getX()),
                getEndCoordWithSignedSize(basePos.getY(), signedSize.getY()),
                getEndCoordWithSignedSize(basePos.getZ(), signedSize.getZ())
            )
        );
    }
    
    private static int getEndCoordWithSignedSize(int base, int signedSize) {
        if (signedSize > 0) {
            return base + signedSize - 1;
        }
        else if (signedSize < 0) {
            return base + signedSize + 1;
        }
        else {
            throw new IllegalArgumentException("Signed size cannot be zero");
        }
    }
    
    public boolean isOnSurface(BlockPos pos) {
        boolean xOnEnd = pos.getX() == l.getX() || pos.getX() == h.getX();
        boolean yOnEnd = pos.getY() == l.getY() || pos.getY() == h.getY();
        boolean zOnEnd = pos.getZ() == l.getZ() || pos.getZ() == h.getZ();
        
        return xOnEnd || yOnEnd || zOnEnd;
    }
    
    public boolean isOnEdge(BlockPos pos) {
        boolean xOnEnd = pos.getX() == l.getX() || pos.getX() == h.getX();
        boolean yOnEnd = pos.getY() == l.getY() || pos.getY() == h.getY();
        boolean zOnEnd = pos.getZ() == l.getZ() || pos.getZ() == h.getZ();
        
        return (xOnEnd && yOnEnd) || (yOnEnd && zOnEnd) || (zOnEnd && xOnEnd);
    }
    
    public boolean isOnVertex(BlockPos pos) {
        boolean xOnEnd = pos.getX() == l.getX() || pos.getX() == h.getX();
        boolean yOnEnd = pos.getY() == l.getY() || pos.getY() == h.getY();
        boolean zOnEnd = pos.getZ() == l.getZ() || pos.getZ() == h.getZ();
        
        return xOnEnd && yOnEnd && zOnEnd;
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putInt("lX", l.getX());
        tag.putInt("lY", l.getY());
        tag.putInt("lZ", l.getZ());
        
        tag.putInt("hX", h.getX());
        tag.putInt("hY", h.getY());
        tag.putInt("hZ", h.getZ());
        
        return tag;
    }
    
    public static IntBox fromTag(CompoundTag tag) {
        return new IntBox(
            new BlockPos(
                tag.getInt("lX"),
                tag.getInt("lY"),
                tag.getInt("lZ")
            ),
            new BlockPos(
                tag.getInt("hX"),
                tag.getInt("hY"),
                tag.getInt("hZ")
            )
        );
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
