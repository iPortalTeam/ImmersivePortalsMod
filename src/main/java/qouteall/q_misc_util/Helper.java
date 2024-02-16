package qouteall.q_misc_util;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.LongBlockPos;
import qouteall.q_misc_util.my_util.RayTraceResult;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// helper methods
public class Helper {
    
    public static final Logger LOGGER = LogManager.getLogger("iPortal");
    
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();
    
    /**
     * Get the intersection point of a line and a plane.
     * The line: p = lineOrigin + t * lineDirection
     * The plane: (p - planeCenter) dot planeNormal = 0
     * Get the t of the colliding point.
     * Solving equation:
     * (lineOrigin - planeCenter) * planeNormal + t * (lineDirection * planeNormal) = 0
     * t = (planeCenter - lineOrigin) * planeNormal / (lineDirection * planeNormal)
     */
    public static double getCollidingT(
        Vec3 planeCenter,
        Vec3 planeNormal,
        Vec3 lineOrigin,
        Vec3 lineDirection // this can be non-normalized
    ) {
        return (planeCenter.subtract(lineOrigin).dot(planeNormal))
            /
            (lineDirection.dot(planeNormal));
    }
    
    /**
     * Given a plane origin and a plane normal (plane normal is a unit vector),
     * a line origin and a line delta, get the t of the colliding point.
     * Plane: planeNormal * (p - planeOrigin) = 0
     * Line: p = lineOrigin + t * lineDelta
     * planeNormal * (lineOrigin - planeOrigin + t * lineDelta) = 0
     * planeNormal * (lineOrigin - planeOrigin) + t * planeNormal * lineDelta = 0
     * t = planeNormal * (planeOrigin - lineOrigin) / (planeNormal * lineDelta)
     * @return NaN if there is no colliding point.
     */
    public static double getCollidingT(
        double planeOriginX, double planeOriginY, double planeOriginZ,
        double planeNormalX, double planeNormalY, double planeNormalZ,
        double lineOriginX, double lineOriginY, double lineOriginZ,
        double lineDeltaX, double lineDeltaY, double lineDeltaZ
    ) {
        double denom = lineDeltaX * planeNormalX +
            lineDeltaY * planeNormalY +
            lineDeltaZ * planeNormalZ;
        
        if (Math.abs(denom) < 1e-6) {
            return Double.NaN;
        }
        
        return ((planeOriginX - lineOriginX) * planeNormalX +
            (planeOriginY - lineOriginY) * planeNormalY +
            (planeOriginZ - lineOriginZ) * planeNormalZ
        ) / denom;
    }
    
    public static @Nullable RayTraceResult raytraceAABB(
        boolean boxFacingOutwards,
        double boxMinX, double boxMinY, double boxMinZ,
        double boxMaxX, double boxMaxY, double boxMaxZ,
        double lineOriginX, double lineOriginY, double lineOriginZ,
        double lineDeltaX, double lineDeltaY, double lineDeltaZ
    ) {
        boolean originInBox = lineOriginX > boxMinX && lineOriginX < boxMaxX &&
            lineOriginY > boxMinY && lineOriginY < boxMaxY &&
            lineOriginZ > boxMinZ && lineOriginZ < boxMaxZ;
        
        if (boxFacingOutwards && originInBox) {
            return null;
        }
        
        // if box is facing inwards, the testing direction is the same as line direction
        // but flip when the box is facing outwards
        boolean testXPosi = (lineDeltaX > 0) ^ boxFacingOutwards;
        boolean testYPosi = (lineDeltaY > 0) ^ boxFacingOutwards;
        boolean testZPosi = (lineDeltaZ > 0) ^ boxFacingOutwards;
        
        // the normal of the plane that the line is intersecting, whether it's positive
        // normalXPosi = testXPosi ^ !boxFacingOutwards = (lineDirectionX > 0) ^ 1
        boolean normalXPosi = lineDeltaX <= 0;
        boolean normalYPosi = lineDeltaY <= 0;
        boolean normalZPosi = lineDeltaZ <= 0;
        
        double tX = getCollidingT(
            testXPosi ? boxMaxX : boxMinX, 0, 0,
            normalXPosi ? 1 : -1, 0, 0,
            lineOriginX, lineOriginY, lineOriginZ,
            lineDeltaX, lineDeltaY, lineDeltaZ
        );
        if (!Double.isNaN(tX) && tX >= 0 && tX <= 1) {
            double y = lineOriginY + tX * lineDeltaY;
            double z = lineOriginZ + tX * lineDeltaZ;
            if (y >= boxMinY && y <= boxMaxY && z >= boxMinZ && z <= boxMaxZ) {
                return new RayTraceResult(
                    tX,
                    new Vec3(testXPosi ? boxMaxX : boxMinX, y, z),
                    new Vec3(testXPosi ? 1 : -1, 0, 0)
                );
            }
        }
        
        double tY = getCollidingT(
            0, testYPosi ? boxMaxY : boxMinY, 0,
            0, normalYPosi ? 1 : -1, 0,
            lineOriginX, lineOriginY, lineOriginZ,
            lineDeltaX, lineDeltaY, lineDeltaZ
        );
        if (!Double.isNaN(tY) && tY >= 0 && tY <= 1) {
            double x = lineOriginX + tY * lineDeltaX;
            double z = lineOriginZ + tY * lineDeltaZ;
            if (x >= boxMinX && x <= boxMaxX && z >= boxMinZ && z <= boxMaxZ) {
                return new RayTraceResult(
                    tY,
                    new Vec3(x, testYPosi ? boxMaxY : boxMinY, z),
                    new Vec3(0, testYPosi ? 1 : -1, 0)
                );
            }
        }
        
        double tZ = getCollidingT(
            0, 0, testZPosi ? boxMaxZ : boxMinZ,
            0, 0, normalZPosi ? 1 : -1,
            lineOriginX, lineOriginY, lineOriginZ,
            lineDeltaX, lineDeltaY, lineDeltaZ
        );
        if (!Double.isNaN(tZ) && tZ >= 0 && tZ <= 1) {
            double x = lineOriginX + tZ * lineDeltaX;
            double y = lineOriginY + tZ * lineDeltaY;
            if (x >= boxMinX && x <= boxMaxX && y >= boxMinY && y <= boxMaxY) {
                return new RayTraceResult(
                    tZ,
                    new Vec3(x, y, testZPosi ? boxMaxZ : boxMinZ),
                    new Vec3(0, 0, testZPosi ? 1 : -1)
                );
            }
        }
        
        return null;
    }
    
    public static boolean isInFrontOfPlane(
        Vec3 pos,
        Vec3 planePos,
        Vec3 planeNormal
    ) {
        return pos.subtract(planePos).dot(planeNormal) > 0;
    }
    
    public static Vec3 fallPointOntoPlane(
        Vec3 point,
        Vec3 planePos,
        Vec3 planeNormal
    ) {
        double t = getCollidingT(planePos, planeNormal, point, planeNormal);
        return point.add(planeNormal.scale(t));
    }
    
    public static Vec3i getUnitFromAxis(Direction.Axis axis) {
        return Direction.get(
            Direction.AxisDirection.POSITIVE,
            axis
        ).getNormal();
    }
    
    public static int getCoordinate(Vec3i v, Direction.Axis axis) {
        return axis.choose(v.getX(), v.getY(), v.getZ());
    }
    
    public static double getCoordinate(Vec3 v, Direction.Axis axis) {
        return axis.choose(v.x, v.y, v.z);
    }
    
    public static int getCoordinate(Vec3i v, Direction direction) {
        return getCoordinate(v, direction.getAxis()) *
            (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
    }
    
    public static Vec3 putCoordinate(Vec3 v, Direction.Axis axis, double value) {
        return switch (axis) {
            case X -> new Vec3(value, v.y, v.z);
            case Y -> new Vec3(v.x, value, v.z);
            default -> new Vec3(v.x, v.y, value);
        };
    }
    
    public static BlockPos putCoordinate(Vec3i v, Direction.Axis axis, int value) {
        return switch (axis) {
            case X -> new BlockPos(value, v.getY(), v.getZ());
            case Y -> new BlockPos(v.getX(), value, v.getZ());
            default -> new BlockPos(v.getX(), v.getY(), value);
        };
    }
    
    public static Vec3 putSignedCoordinate(Vec3 vec, Direction direction, double value) {
        return switch (direction) {
            case DOWN -> new Vec3(vec.x, -value, vec.z);
            case UP -> new Vec3(vec.x, value, vec.z);
            case NORTH -> new Vec3(vec.x, vec.y, -value);
            case SOUTH -> new Vec3(vec.x, vec.y, value);
            case WEST -> new Vec3(-value, vec.y, vec.z);
            case EAST -> new Vec3(value, vec.y, vec.z);
            default -> throw new RuntimeException();
        };
    }
    
    public static double getSignedCoordinate(Vec3 vec, Direction direction) {
        // fully written by Copilot
        return switch (direction) {
            case DOWN -> -vec.y;
            case UP -> vec.y;
            case NORTH -> -vec.z;
            case SOUTH -> vec.z;
            case WEST -> -vec.x;
            case EAST -> vec.x;
            default -> throw new RuntimeException();
        };
    }
    
    public static double getDistanceSqrOnAxisPlane(Vec3 vec, Direction.Axis axis) {
        return switch (axis) {
            case X -> vec.y * vec.y + vec.z * vec.z;
            case Y -> vec.x * vec.x + vec.z * vec.z;
            case Z -> vec.x * vec.x + vec.y * vec.y;
        };
    }
    
    public static double getBoxCoordinate(AABB box, Direction direction) {
        switch (direction) {
            case DOWN -> {return box.minY;}
            case UP -> {return box.maxY;}
            case NORTH -> {return box.minZ;}
            case SOUTH -> {return box.maxZ;}
            case WEST -> {return box.minX;}
            case EAST -> {return box.maxX;}
        }
        throw new RuntimeException();
    }
    
    public static AABB replaceBoxCoordinate(AABB box, Direction direction, double coordinate) {
        switch (direction) {
            case DOWN -> {return new AABB(box.minX, coordinate, box.minZ, box.maxX, box.maxY, box.maxZ);}
            case UP -> {return new AABB(box.minX, box.minY, box.minZ, box.maxX, coordinate, box.maxZ);}
            case NORTH -> {return new AABB(box.minX, box.minY, coordinate, box.maxX, box.maxY, box.maxZ);}
            case SOUTH -> {return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, coordinate);}
            case WEST -> {return new AABB(coordinate, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);}
            case EAST -> {return new AABB(box.minX, box.minY, box.minZ, coordinate, box.maxY, box.maxZ);}
        }
        throw new RuntimeException();
    }
    
    public static <A, B> Tuple<B, A> swaped(Tuple<A, B> p) {
        return new Tuple<>(p.getB(), p.getA());
    }
    
    public static <T> T uniqueOfThree(T a, T b, T c) {
        if (a.equals(b)) {
            return c;
        }
        else if (b.equals(c)) {
            return a;
        }
        else {
            assert a.equals(c);
            return b;
        }
    }
    
    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ())
        );
    }
    
    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ())
        );
    }
    
    public static Tuple<Direction.Axis, Direction.Axis> getAnotherTwoAxis(Direction.Axis axis) {
        switch (axis) {
            case X:
                return new Tuple<>(Direction.Axis.Y, Direction.Axis.Z);
            case Y:
                return new Tuple<>(Direction.Axis.Z, Direction.Axis.X);
            case Z:
                return new Tuple<>(Direction.Axis.X, Direction.Axis.Y);
        }
        throw new IllegalArgumentException();
    }
    
    public static BlockPos scale(Vec3i v, int m) {
        return new BlockPos(v.getX() * m, v.getY() * m, v.getZ() * m);
    }
    
    public static BlockPos divide(Vec3i v, int d) {
        return new BlockPos(v.getX() / d, v.getY() / d, v.getZ() / d);
    }
    
    public static BlockPos floorDiv(Vec3i v, int d) {
        return new BlockPos(
            Math.floorDiv(v.getX(), d),
            Math.floorDiv(v.getY(), d),
            Math.floorDiv(v.getZ(), d)
        );
    }
    
    public static Direction[] getAnotherFourDirections(Direction.Axis axisOfNormal) {
        Tuple<Direction.Axis, Direction.Axis> anotherTwoAxis = getAnotherTwoAxis(
            axisOfNormal
        );
        return new Direction[]{
            Direction.get(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getA()
            ),
            Direction.get(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getB()
            ),
            Direction.get(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getA()
            ),
            Direction.get(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getB()
            )
        };
    }
    
    public static Tuple<Direction, Direction> getPerpendicularDirections(Direction facing) {
        Tuple<Direction.Axis, Direction.Axis> axises = getAnotherTwoAxis(facing.getAxis());
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Tuple<>(axises.getB(), axises.getA());
        }
        return new Tuple<>(
            Direction.get(Direction.AxisDirection.POSITIVE, axises.getA()),
            Direction.get(Direction.AxisDirection.POSITIVE, axises.getB())
        );
    }
    
    public static Vec3 getBoxSize(AABB box) {
        return new Vec3(box.getXsize(), box.getYsize(), box.getZsize());
    }
    
    public static AABB getBoxSurfaceInversed(AABB box, Direction direction) {
        double size = getCoordinate(getBoxSize(box), direction.getAxis());
        Vec3 shrinkVec = Vec3.atLowerCornerOf(direction.getNormal()).scale(size);
        return box.contract(shrinkVec.x, shrinkVec.y, shrinkVec.z);
    }
    
    public static AABB getBoxSurface(AABB box, Direction direction) {
        return getBoxSurfaceInversed(box, direction.getOpposite());
    }
    
    public static IntBox expandRectangle(
        BlockPos startingPos,
        Predicate<BlockPos> blockPosPredicate, Direction.Axis axis
    ) {
        IntBox wallArea = new IntBox(startingPos, startingPos);
        
        for (Direction direction : getAnotherFourDirections(axis)) {
            
            wallArea = expandArea(
                wallArea, blockPosPredicate, direction
            );
        }
        return wallArea;
    }
    
    public static IntBox expandBoxArea(
        BlockPos startingPos,
        Predicate<BlockPos> blockPosPredicate
    ) {
        IntBox box = new IntBox(startingPos, startingPos);
        
        for (Direction direction : Direction.values()) {
            box = expandArea(
                box, blockPosPredicate, direction
            );
        }
        return box;
    }
    
    public static int getChebyshevDistance(
        int x1, int z1,
        int x2, int z2
    ) {
        return Math.max(
            Math.abs(x1 - x2),
            Math.abs(z1 - z2)
        );
    }
    
    public static AABB getBoxByBottomPosAndSize(Vec3 boxBottomCenter, Vec3 viewBoxSize) {
        return new AABB(
            boxBottomCenter.subtract(viewBoxSize.x / 2, 0, viewBoxSize.z / 2),
            boxBottomCenter.add(viewBoxSize.x / 2, viewBoxSize.y, viewBoxSize.z / 2)
        );
    }
    
    public static Vec3 getBoxBottomCenter(AABB box) {
        return new Vec3(
            (box.maxX + box.minX) / 2,
            box.minY,
            (box.maxZ + box.minZ) / 2
        );
    }
    
    public static double getDistanceToRectangle(
        double pointX, double pointY,
        double rectAX, double rectAY,
        double rectBX, double rectBY
    ) {
        assert rectAX <= rectBX;
        assert rectAY <= rectBY;
        
        double wx1 = rectAX - pointX;
        double wx2 = rectBX - pointX;
        double dx = (wx1 * wx2 < 0 ? 0 : Math.min(Math.abs(wx1), Math.abs(wx2)));
        
        double wy1 = rectAY - pointY;
        double wy2 = rectBY - pointY;
        double dy = (wy1 * wy2 < 0 ? 0 : Math.min(Math.abs(wy1), Math.abs(wy2)));
        
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public static <T> void swapListElement(List<T> entries, int i1, int i2) {
        T temp = entries.get(i1);
        entries.set(i1, entries.get(i2));
        entries.set(i2, temp);
    }
    
    // positive if it's rotating counter clock wise
    public static double crossProduct2D(
        double x1, double y1,
        double x2, double y2
    ) {
        return x1 * y2 - x2 * y1;
    }
    
    public static ResourceKey<Level> dimIdToKey(ResourceLocation identifier) {
        return ResourceKey.create(Registries.DIMENSION, identifier);
    }
    
    public static ResourceKey<Level> dimIdToKey(String str) {
        return dimIdToKey(new ResourceLocation(str));
    }
    
    public static void putWorldId(CompoundTag tag, String tagName, ResourceKey<Level> dim) {
        tag.putString(tagName, dim.location().toString());
    }
    
    public static ResourceKey<Level> getWorldId(CompoundTag tag, String tagName) {
        Tag term = tag.get(tagName);
        
        if (term instanceof StringTag) {
            String id = ((StringTag) term).getAsString();
            return dimIdToKey(id);
        }
        
        LOGGER.error("Cannot read world id from {}. Fallback to overworld", tag);
        return Level.OVERWORLD;
    }
    
    public static class SimpleBox<T> {
        public T obj;
        
        public SimpleBox(T obj) {
            this.obj = obj;
        }
    }
    
    @Nullable
    public static <T> T getLastSatisfying(Stream<T> stream, Predicate<T> predicate) {
        T last = null;
        
        Iterator<T> iterator = stream.iterator();
        
        while (iterator.hasNext()) {
            T obj = iterator.next();
            if (predicate.test(obj)) {
                last = obj;
            }
            else {
                return last;
            }
        }
        
        return last;
    }
    
    public static void doNotEatExceptionMessage(
        Runnable func
    ) {
        try {
            func.run();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public static <T> String myToString(
        Stream<T> stream
    ) {
        StringBuilder stringBuilder = new StringBuilder();
        stream.forEach(obj -> {
            stringBuilder.append(obj.toString());
            stringBuilder.append('\n');
        });
        return stringBuilder.toString();
    }
    
    public static <A, B> Stream<Tuple<A, B>> composeTwoStreamsWithEqualLength(
        Stream<A> a,
        Stream<B> b
    ) {
        Iterator<A> aIterator = a.iterator();
        Iterator<B> bIterator = b.iterator();
        Iterator<Tuple<A, B>> iterator = new Iterator<Tuple<A, B>>() {
            
            @Override
            public boolean hasNext() {
                assert aIterator.hasNext() == bIterator.hasNext();
                return aIterator.hasNext();
            }
            
            @Override
            public Tuple<A, B> next() {
                return new Tuple<>(aIterator.next(), bIterator.next());
            }
        };
        
        return Streams.stream(iterator);
    }
    
    // TODO use separate logger for each class
    @Deprecated
    public static void log(Object str) {
        LOGGER.info(str);
    }
    
    // TODO use separate logger for each class
    @Deprecated
    public static void err(Object str) {
        LOGGER.error(str);
    }
    
    @Deprecated
    public static void dbg(Object str) {
        LOGGER.debug(str);
    }
    
    public static Vec3[] eightVerticesOf(AABB box) {
        return new Vec3[]{
            new Vec3(box.minX, box.minY, box.minZ),
            new Vec3(box.minX, box.minY, box.maxZ),
            new Vec3(box.minX, box.maxY, box.minZ),
            new Vec3(box.minX, box.maxY, box.maxZ),
            new Vec3(box.maxX, box.minY, box.minZ),
            new Vec3(box.maxX, box.minY, box.maxZ),
            new Vec3(box.maxX, box.maxY, box.minZ),
            new Vec3(box.maxX, box.maxY, box.maxZ)
        };
    }
    
    public static void putVec3d(CompoundTag compoundTag, String name, Vec3 vec3d) {
        compoundTag.putDouble(name + "X", vec3d.x);
        compoundTag.putDouble(name + "Y", vec3d.y);
        compoundTag.putDouble(name + "Z", vec3d.z);
    }
    
    public static Vec3 getVec3d(CompoundTag compoundTag, String name) {
        return new Vec3(
            compoundTag.getDouble(name + "X"),
            compoundTag.getDouble(name + "Y"),
            compoundTag.getDouble(name + "Z")
        );
    }
    
    @Nullable
    public static Vec3 getVec3dOptional(CompoundTag compoundTag, String name) {
        if (compoundTag.contains(name + "X")) {
            return getVec3d(compoundTag, name);
        }
        else {
            return null;
        }
    }
    
    public static void putVec3i(CompoundTag compoundTag, String name, Vec3i vec3i) {
        compoundTag.putInt(name + "X", vec3i.getX());
        compoundTag.putInt(name + "Y", vec3i.getY());
        compoundTag.putInt(name + "Z", vec3i.getZ());
    }
    
    public static BlockPos getVec3i(CompoundTag compoundTag, String name) {
        return new BlockPos(
            compoundTag.getInt(name + "X"),
            compoundTag.getInt(name + "Y"),
            compoundTag.getInt(name + "Z")
        );
    }
    
    public static void putQuaternion(CompoundTag compoundTag, String name, @Nullable DQuaternion quaternion) {
        if (quaternion != null) {
            compoundTag.putDouble(name + "X", quaternion.getX());
            compoundTag.putDouble(name + "Y", quaternion.getY());
            compoundTag.putDouble(name + "Z", quaternion.getZ());
            compoundTag.putDouble(name + "W", quaternion.getW());
        }
    }
    
    @Nullable
    public static DQuaternion getQuaternion(CompoundTag compoundTag, String name) {
        if (compoundTag.contains(name + "X")) {
            return new DQuaternion(
                compoundTag.getDouble(name + "X"),
                compoundTag.getDouble(name + "Y"),
                compoundTag.getDouble(name + "Z"),
                compoundTag.getDouble(name + "W")
            );
        }
        else {
            return null;
        }
    }
    
    public static ListTag getCompoundList(CompoundTag tag, String name) {
        return tag.getList(name, 10);
    }
    
    /**
     * It's safe to modify the result array list.
     * Note: if the deserializer returns null, it won't be in the result list.
     */
    public static <X> ArrayList<X> listTagToList(ListTag listTag, Function<CompoundTag, X> deserializer) {
        return listTagDeserialize(listTag, deserializer, CompoundTag.class);
    }
    
    public static <X> ListTag listToListTag(List<X> list, Function<X, CompoundTag> serializer) {
        return listTagSerialize(list, serializer);
    }
    
    /**
     * It's safe to modify the result array list.
     * Note: if the deserializer returns null, it won't be in the result list.
     */
    public static <X, TT extends Tag> ArrayList<X> listTagDeserialize(
        ListTag listTag, Function<TT, X> deserializer,
        Class<TT> tagClass
    ) {
        ArrayList<X> result = new ArrayList<>();
        listTag.forEach(tag -> {
            if (tag.getClass() == tagClass) {
                X obj = deserializer.apply((TT) tag);
                if (obj != null) {
                    result.add(obj);
                }
            }
            else {
                LOGGER.error("Unexpected tag class: {}", tag.getClass(), new Throwable());
            }
        });
        return result;
    }
    
    public static <X, TT extends Tag> ListTag listTagSerialize(List<X> list, Function<X, TT> serializer) {
        ListTag listTag = new ListTag();
        for (X x : list) {
            listTag.add(serializer.apply(x));
        }
        return listTag;
    }
    
    public static <T> void compareOldAndNew(
        Set<T> oldSet,
        Set<T> newSet,
        Consumer<T> forRemoved,
        Consumer<T> forAdded
    ) {
        oldSet.stream().filter(
            e -> !newSet.contains(e)
        ).forEach(
            forRemoved
        );
        newSet.stream().filter(
            e -> !oldSet.contains(e)
        ).forEach(
            forAdded
        );
    }
    
    public static long secondToNano(double second) {
        return (long) (second * 1000000000L);
    }
    
    public static double nanoToSecond(long nano) {
        return nano / 1000000000.0;
    }
    
    public static IntBox expandArea(
        IntBox originalArea,
        Predicate<BlockPos> predicate,
        Direction direction
    ) {
        IntBox currentBox = originalArea;
        for (int i = 1; i < 42; i++) {
            IntBox expanded = currentBox.getExpanded(direction, 1);
            if (expanded.getSurfaceLayer(direction).stream().allMatch(predicate)) {
                currentBox = expanded;
            }
            else {
                return currentBox;
            }
        }
        return currentBox;
    }
    
    public static <A, B> B reduce(
        B start,
        Stream<A> stream,
        BiFunction<B, A, B> func
    ) {
        return stream.reduce(
            start,
            func,
            (a, b) -> {
                throw new IllegalStateException("combiner should only be used in parallel");
            }
        );
    }
    
    public static <T> T noError(Callable<T> func) {
        try {
            return func.call();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * {@link ObjectList} does not override removeIf() so it's O(n^2)
     * {@link ArrayList#removeIf(Predicate)} uses a bitset to ensure integrity
     * in case of exception thrown but introduces performance overhead
     */
    public static <T> void removeIf(ObjectList<T> list, Predicate<T> predicate) {
        int placingIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            T curr = list.get(i);
            if (!predicate.test(curr)) {
                list.set(placingIndex, curr);
                placingIndex += 1;
            }
        }
        list.removeElements(placingIndex, list.size());
    }
    
    /**
     * removeIf, but can early exit when the MutableBoolean is set to true
     */
    public static <T> void removeIfWithEarlyExit(
        ObjectList<T> list, BiPredicate<T, MutableBoolean> predicate
    ) {
        MutableBoolean shouldStop = new MutableBoolean(false);
        
        int placingIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            T curr = list.get(i);
            // if stopped, it will be deemed as non-remove and not call the predicate
            if (shouldStop.booleanValue() || !predicate.test(curr, shouldStop)) {
                list.set(placingIndex, curr);
                placingIndex += 1;
            }
        }
        list.removeElements(placingIndex, list.size());
    }
    
    public static <T, S> Stream<S> wrapAdjacentAndMap(
        Stream<T> stream,
        BiFunction<T, T, S> function
    ) {
        Iterator<T> iterator = stream.iterator();
        return Streams.stream(new Iterator<S>() {
            private boolean isBuffered = false;
            private T buffer;
            
            private void fillBuffer() {
                if (!isBuffered) {
                    assert iterator.hasNext();
                    isBuffered = true;
                    buffer = iterator.next();
                }
            }
            
            private T takeBuffer() {
                assert isBuffered;
                isBuffered = false;
                return buffer;
            }
            
            @Override
            public boolean hasNext() {
                if (!iterator.hasNext()) {
                    return false;
                }
                fillBuffer();
                return iterator.hasNext();
            }
            
            @Override
            public S next() {
                fillBuffer();
                T a = takeBuffer();
                fillBuffer();
                return function.apply(a, buffer);
            }
        });
    }
    
    //map and reduce at the same time
    public static <A, B> Stream<B> mapReduce(
        Stream<A> stream,
        BiFunction<B, A, B> func,
        SimpleBox<B> startValue
    ) {
        return stream.map(a -> {
            startValue.obj = func.apply(startValue.obj, a);
            return startValue.obj;
        });
    }
    
    //another implementation using mapReduce but creates more garbage objects
    public static <T, S> Stream<S> wrapAdjacentAndMap1(
        Stream<T> stream,
        BiFunction<T, T, S> function
    ) {
        Iterator<T> iterator = stream.iterator();
        if (!iterator.hasNext()) {
            return Stream.empty();
        }
        T firstValue = iterator.next();
        Stream<T> newStream = Streams.stream(iterator);
        return mapReduce(
            newStream,
            (Tuple<T, S> lastPair, T curr) ->
                new Tuple<T, S>(curr, function.apply(lastPair.getA(), curr)),
            new SimpleBox<>(new Tuple<T, S>(firstValue, null))
        ).map(pair -> pair.getB());
    }
    
    public static <T> T makeIntoExpression(T t, Consumer<T> func) {
        func.accept(t);
        return t;
    }
    
    public static void putUuid(CompoundTag tag, String key, UUID uuid) {
        tag.putLong(key + "Most", uuid.getMostSignificantBits());
        tag.putLong(key + "Least", uuid.getLeastSignificantBits());
    }
    
    @Nullable
    public static UUID getUuid(CompoundTag tag, String key) {
        String key1 = key + "Most";
        
        if (!tag.contains(key1)) {
            return null;
        }
        
        return new UUID(tag.getLong(key1), tag.getLong(key + "Least"));
    }
    
    public static Vec3 getFlippedVec(Vec3 vec, Vec3 flippingAxis) {
        Vec3 component = getProjection(vec, flippingAxis);
        return vec.subtract(component).scale(-1).add(component);
    }
    
    public static Vec3 getProjection(Vec3 vec, Vec3 direction) {
        return direction.scale(vec.dot(direction));
    }
    
    /**
     * @param vec  The {@link Vec3} to get the {@link Direction} of.
     * @param axis The {@link Direction.Axis} of directions to exclude.
     * @return The {@link Direction} of the passed {@code vec}, excluding directions of axis {@code axis}.
     */
    @SuppressWarnings("WeakerAccess")
    public static Direction getFacingExcludingAxis(Vec3 vec, Direction.Axis axis) {
        return Arrays.stream(Direction.values())
            .filter(d -> d.getAxis() != axis)
            .max(Comparator.comparingDouble(
                dir -> vec.x * dir.getStepX() + vec.y * dir.getStepY() + vec.z * dir.getStepZ()
            ))
            .orElse(null);
    }
    
    // calculate upon first retrieval and cache it
    public static <T> Supplier<T> cached(Supplier<T> supplier) {
        return new Supplier<T>() {
            T cache = null;
            
            @Override
            public T get() {
                if (cache == null) {
                    cache = supplier.get();
                }
                Validate.notNull(cache);
                return cache;
            }
        };
    }
    
    // I cannot find existing indexOf with predicate
    public static <T> int indexOf(List<T> list, Predicate<T> predicate) {
        for (int i = 0; i < list.size(); i++) {
            T ele = list.get(i);
            if (predicate.test(ele)) {
                return i;
            }
        }
        return -1;
    }
    
    public static List<String> splitStringByLen(String str, int len) {
        List<String> result = new ArrayList<>();
        
        for (int i = 0; i * len < str.length(); i++) {
            result.add(
                str.substring(i * len, Math.min(str.length(), (i + 1) * len))
            );
        }
        
        return result;
    }
    
    // this will expand the box because the box can only be axis aligned
    public static AABB transformBox(
        AABB box, Function<Vec3, Vec3> function
    ) {
        List<Vec3> result =
            Arrays.stream(eightVerticesOf(box)).map(function).collect(Collectors.toList());
        
        return new AABB(
            result.stream().mapToDouble(b -> b.x).min().getAsDouble(),
            result.stream().mapToDouble(b -> b.y).min().getAsDouble(),
            result.stream().mapToDouble(b -> b.z).min().getAsDouble(),
            result.stream().mapToDouble(b -> b.x).max().getAsDouble(),
            result.stream().mapToDouble(b -> b.y).max().getAsDouble(),
            result.stream().mapToDouble(b -> b.z).max().getAsDouble()
        );
    }
    
    public static double getDistanceToRange(double start, double end, double pos) {
        Validate.isTrue(end >= start);
        if (pos >= start) {
            if (pos <= end) {
                return 0;
            }
            else {
                return pos - end;
            }
        }
        else {
            return start - pos;
        }
    }
    
    public static double getSignedDistanceToRange(double start, double end, double pos) {
        Validate.isTrue(end >= start);
        if (pos >= start) {
            if (pos <= end) {
                return -Math.min(pos - start, end - pos);
            }
            else {
                return pos - end;
            }
        }
        else {
            return start - pos;
        }
    }
    
    public static double getDistanceToBox(AABB box, Vec3 point) {
        double dx = getDistanceToRange(box.minX, box.maxX, point.x);
        double dy = getDistanceToRange(box.minY, box.maxY, point.y);
        double dz = getDistanceToRange(box.minZ, box.maxZ, point.z);
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public static double getSignedDistanceToBox(AABB aabb, Vec3 point) {
        double dx = getSignedDistanceToRange(aabb.minX, aabb.maxX, point.x);
        double dy = getSignedDistanceToRange(aabb.minY, aabb.maxY, point.y);
        double dz = getSignedDistanceToRange(aabb.minZ, aabb.maxZ, point.z);
        
        double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dx < 0 && dy < 0 && dz < 0) {
            return -l;
        }
        else {
            return l;
        }
    }
    
    public static <T> T firstOf(List<T> list) {
        Validate.isTrue(!list.isEmpty());
        return list.get(0);
    }
    
    public static <T> T lastOf(List<T> list) {
        Validate.isTrue(!list.isEmpty());
        return list.get(list.size() - 1);
    }
    
    @Nullable
    public static <T> T combineNullable(@Nullable T a, @Nullable T b, BiFunction<T, T, T> combiner) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return combiner.apply(a, b);
    }
    
    // treat ArrayList as an integer to object map
    // do computeIfAbsent
    public static <T> T arrayListComputeIfAbsent(
        List<T> arrayList,
        int index,
        Supplier<T> supplier
    ) {
        if (arrayList.size() <= index) {
            while (arrayList.size() <= index) {
                arrayList.add(null);
            }
        }
        T value = arrayList.get(index);
        if (value == null) {
            value = supplier.get();
            arrayList.set(index, value);
        }
        return value;
    }
    
    public static interface IntObjectConsumer<T> {
        void consume(int index, T object);
    }
    
    public static <T> void arrayListKeyValueForeach(
        ArrayList<T> arrayList,
        IntObjectConsumer<T> func
    ) {
        for (int i = 0; i < arrayList.size(); i++) {
            T value = arrayList.get(i);
            if (value != null) {
                func.consume(i, value);
            }
        }
    }
    
    public static Object reflectionInvoke(Object target, String methodName) {
        return noError(() -> {
            Method method = target.getClass().getDeclaredMethod(methodName);
            return method.invoke(target);
        });
    }
    
    public static Vec3 interpolatePos(Vec3 from, Vec3 to, double progress) {
        return from.lerp(to, progress);
//        return new Vec3(
//            Mth.lerp(progress, from.x, to.x),
//            Mth.lerp(progress, from.y, to.y),
//            Mth.lerp(progress, from.z, to.z)
//        );
    }
    
    public static <T> T getFirstNullable(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        else {
            return list.get(0);
        }
    }
    
    public static <T> T minBy(T a, T b, Comparator<T> comparator) {
        if (comparator.compare(a, b) <= 0) {
            return a;
        }
        else {
            return b;
        }
    }
    
    public static <T> T maxBy(T a, T b, Comparator<T> comparator) {
        if (comparator.compare(a, b) >= 0) {
            return a;
        }
        else {
            return b;
        }
    }
    
    public static boolean boxContains(AABB outer, AABB inner) {
        return outer.contains(inner.minX, inner.minY, inner.minZ) &&
            outer.contains(inner.maxX, inner.maxY, inner.maxZ);
    }
    
    public static <A, B> List<B> mappedListView(
        List<A> originalList, Function<A, B> mapping
    ) {
        return new AbstractList<B>() {
            @Override
            public B get(int index) {
                return mapping.apply(originalList.get(index));
            }
            
            @Override
            public int size() {
                return originalList.size();
            }
        };
    }
    
    // parse double without try catching
    public static OptionalDouble parseDouble(String str) {
        try {
            return OptionalDouble.of(Double.parseDouble(str));
        }
        catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
    
    // parse int without try catching
    public static OptionalInt parseInt(String str) {
        try {
            return OptionalInt.of(Integer.parseInt(str));
        }
        catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
    
    public static List<Vec3> deduplicateWithPrecision(
        Collection<Vec3> points,
        int precision
    ) {
        HashSet<LongBlockPos> set = new HashSet<>();
        for (Vec3 point : points) {
            set.add(new LongBlockPos(
                (long) Math.round(point.x * precision),
                (long) Math.round(point.y * precision),
                (long) Math.round(point.z * precision)
            ));
        }
        return set.stream().map(
            blockPos -> new Vec3(
                blockPos.x() / (double) precision,
                blockPos.y() / (double) precision,
                blockPos.z() / (double) precision
            )
        ).collect(Collectors.toList());
    }
    
    public static double getDistanceFromPointToLine(
        Vec3 point,
        Vec3 lineOrigin,
        Vec3 lineDirection
    ) {
        Vec3 lineDirectionNormalized = lineDirection.normalize();
        
        Vec3 pointToLineOrigin = lineOrigin.subtract(point);
        Vec3 pointToLineOriginProjected = pointToLineOrigin.subtract(
            lineDirectionNormalized.scale(pointToLineOrigin.dot(lineDirectionNormalized))
        );
        return pointToLineOriginProjected.length();
    }
    
    public static interface BoxEdgeConsumer {
        void consume(
            int ax, int ay, int az,
            int bx, int by, int bz
        );
    }
    
    public static void traverseBoxEdge(
        BoxEdgeConsumer consumer
    ) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                consumer.consume(dx, dy, 0, dx, dy, 1);
            }
        }
        
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                consumer.consume(dx, 0, dz, dx, 1, dz);
            }
        }
        
        for (int dy = 0; dy <= 1; dy++) {
            for (int dz = 0; dz <= 1; dz++) {
                consumer.consume(0, dy, dz, 1, dy, dz);
            }
        }
    }
    
    public static List<Vec3> verticesAndEdgeMidpoints(AABB box) {
        List<Vec3> result = new ArrayList<>();
        for (int xi = 0; xi <= 2; xi++) {
            for (int yi = 0; yi <= 2; yi++) {
                for (int zi = 0; zi <= 2; zi++) {
                    if (xi != 1 || yi != 1 || zi != 1) {
                        double x = box.minX + (xi / 2.0) * (box.maxX - box.minX);
                        double y = box.minY + (yi / 2.0) * (box.maxY - box.minY);
                        double z = box.minZ + (zi / 2.0) * (box.maxZ - box.minZ);
                        
                        result.add(new Vec3(x, y, z));
                    }
                }
            }
        }
        
        return result;
    }
    
    public static Vec3 alignToBoxSurface(AABB box, Vec3 pos, int gridCount) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        
        if (x < box.minX) x = box.minX;
        if (y < box.minY) y = box.minY;
        if (z < box.minZ) z = box.minZ;
        if (x > box.maxX) x = box.maxX;
        if (y > box.maxY) y = box.maxY;
        if (z > box.maxZ) z = box.maxZ;
        
        double boxSizeX = box.getXsize();
        double boxSizeY = box.getYsize();
        double boxSizeZ = box.getZsize();
        
        double xOffset = x - box.minX;
        double yOffset = y - box.minY;
        double zOffset = z - box.minZ;
        
        // align to grid
        xOffset = Math.round(xOffset * gridCount) / (double) gridCount;
        yOffset = Math.round(yOffset * gridCount) / (double) gridCount;
        zOffset = Math.round(zOffset * gridCount) / (double) gridCount;
        
        // align to the nearest surface
        double distanceToXMin = xOffset;
        double distanceToXMax = boxSizeX - xOffset;
        double distanceToYMin = yOffset;
        double distanceToYMax = boxSizeY - yOffset;
        double distanceToZMin = zOffset;
        double distanceToZMax = boxSizeZ - zOffset;
        
        double minDistance = Math.min(
            Math.min(distanceToXMin, distanceToXMax),
            Math.min(
                Math.min(distanceToYMin, distanceToYMax),
                Math.min(distanceToZMin, distanceToZMax)
            )
        );
        
        if (minDistance == distanceToXMin) {
            xOffset = 0;
        }
        else if (minDistance == distanceToXMax) {
            xOffset = boxSizeX;
        }
        else if (minDistance == distanceToYMin) {
            yOffset = 0;
        }
        else if (minDistance == distanceToYMax) {
            yOffset = boxSizeY;
        }
        else if (minDistance == distanceToZMin) {
            zOffset = 0;
        }
        else if (minDistance == distanceToZMax) {
            zOffset = boxSizeZ;
        }
        
        return new Vec3(
            box.minX + xOffset, box.minY + yOffset, box.minZ + zOffset
        );
    }
    
    @FunctionalInterface
    public static interface SwappingFunc {
        void swap(int validElementIndex, int invalidElementIndex);
    }
    
    /**
     * Compacts an array in-place, by moving valid elements at the end to
     * fill in the places of invalid elements in the beginning.
     * After using this, all valid elements will be at the beginning of the array without gaps.
     *
     * @param arraySize      size of the array
     * @param isElementValid predicate that returns true if the element at the
     * @param swap           function that swaps a valid element with an invalid element.
     *                       Its first argument is the index of a valid element on the right side,
     *                       and its second argument is the index of an invalid element on the left side.
     * @return number of valid elements in the beginning of the array
     */
    public static int compactArrayStorage(
        int arraySize,
        IntPredicate isElementValid,
        SwappingFunc swap
    ) {
        int validElementCount = 0;
        int invalidElementCount = 0;
        
        while (validElementCount + invalidElementCount < arraySize) {
            if (isElementValid.test(validElementCount)) {
                validElementCount++;
            }
            else {
                // the element at `validElementCount` is invalid
                invalidElementCount++;
                int invalidElementIndex = arraySize - invalidElementCount;
                
                while (invalidElementIndex > validElementCount) {
                    if (isElementValid.test(invalidElementIndex)) {
                        // the element at `invalidElementIndex` is valid
                        swap.swap(invalidElementIndex, validElementCount);
                        validElementCount++;
                        break;
                    }
                    else {
                        invalidElementCount++;
                        invalidElementIndex = arraySize - invalidElementCount;
                    }
                }
            }
        }
        
        return validElementCount;
    }
    
    public static <T> Stream<T> listReverseStream(List<T> list) {
        return Streams.stream(new Iterator<T>() {
            int index = list.size() - 1;
            
            @Override
            public boolean hasNext() {
                return index >= 0;
            }
            
            @Override
            public T next() {
                int i = index;
                index -= 1;
                return list.get(i);
            }
        });
    }
    
    public static Event<Runnable> createRunnableEvent() {
        return EventFactory.createArrayBacked(
            Runnable.class,
            (listeners) -> () -> {
                for (Runnable listener : listeners) {
                    listener.run();
                }
            }
        );
    }
    
    public static <T> Event<Consumer<T>> createConsumerEvent() {
        return EventFactory.createArrayBacked(
            Consumer.class,
            (listeners) -> (t) -> {
                for (Consumer<T> listener : listeners) {
                    listener.accept(t);
                }
            }
        );
    }
    
    public static <A, B> Event<BiConsumer<A, B>> createBiConsumerEvent() {
        return EventFactory.createArrayBacked(
            BiConsumer.class,
            (listeners) -> (a, b) -> {
                for (BiConsumer<A, B> listener : listeners) {
                    listener.accept(a, b);
                }
            }
        );
    }
    
    public static ListTag vec3ToListTag(Vec3 vec) {
        ListTag listTag = new ListTag();
        listTag.add(DoubleTag.valueOf(vec.x));
        listTag.add(DoubleTag.valueOf(vec.y));
        listTag.add(DoubleTag.valueOf(vec.z));
        return listTag;
    }
    
    public static @Nullable Vec3 vec3FromListTag(Tag tag) {
        if (tag instanceof ListTag listTag) {
            if (listTag.getElementType() == Tag.TAG_DOUBLE && listTag.size() == 3) {
                return new Vec3(
                    listTag.getDouble(0),
                    listTag.getDouble(1),
                    listTag.getDouble(2)
                );
            }
        }
        
        return null;
    }
    
    public static AABB boundingBoxOfPoints(Vec3[] arr) {
        Validate.isTrue(arr.length != 0, "arr must not be empty");
        
        double minX = arr[0].x;
        double minY = arr[0].y;
        double minZ = arr[0].z;
        double maxX = arr[0].x;
        double maxY = arr[0].y;
        double maxZ = arr[0].z;
        
        for (int i = 1; i < arr.length; i++) {
            Vec3 vec3 = arr[i];
            minX = Math.min(minX, vec3.x);
            minY = Math.min(minY, vec3.y);
            minZ = Math.min(minZ, vec3.z);
            maxX = Math.max(maxX, vec3.x);
            maxY = Math.max(maxY, vec3.y);
            maxZ = Math.max(maxZ, vec3.z);
        }
        
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
