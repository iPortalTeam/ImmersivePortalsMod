package qouteall.q_misc_util;

import com.google.common.collect.Streams;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.my_util.IntBox;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// helper methods
public class Helper {
    
    private static final Logger logger = LogManager.getLogger("Portal");
    
    //get the intersect point of a line and a plane
    //a line: p = lineCenter + t * lineDirection
    //get the t of the colliding point
    //normal and lineDirection have to be normalized
    public static double getCollidingT(
        Vec3d planeCenter,
        Vec3d planeNormal,
        Vec3d lineCenter,
        Vec3d lineDirection
    ) {
        return (planeCenter.subtract(lineCenter).dotProduct(planeNormal))
            /
            (lineDirection.dotProduct(planeNormal));
    }
    
    public static boolean isInFrontOfPlane(
        Vec3d pos,
        Vec3d planePos,
        Vec3d planeNormal
    ) {
        return pos.subtract(planePos).dotProduct(planeNormal) > 0;
    }
    
    public static Vec3d fallPointOntoPlane(
        Vec3d point,
        Vec3d planePos,
        Vec3d planeNormal
    ) {
        double t = getCollidingT(planePos, planeNormal, point, planeNormal);
        return point.add(planeNormal.multiply(t));
    }
    
    public static Vec3i getUnitFromAxis(Direction.Axis axis) {
        return Direction.get(
            Direction.AxisDirection.POSITIVE,
            axis
        ).getVector();
    }
    
    public static int getCoordinate(Vec3i v, Direction.Axis axis) {
        return axis.choose(v.getX(), v.getY(), v.getZ());
    }
    
    public static double getCoordinate(Vec3d v, Direction.Axis axis) {
        return axis.choose(v.x, v.y, v.z);
    }
    
    public static int getCoordinate(Vec3i v, Direction direction) {
        return getCoordinate(v, direction.getAxis()) *
            (direction.getDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1);
    }
    
    public static Vec3d putCoordinate(Vec3d v, Direction.Axis axis, double value) {
        if (axis == Direction.Axis.X) {
            return new Vec3d(value, v.y, v.z);
        }
        else if (axis == Direction.Axis.Y) {
            return new Vec3d(v.x, value, v.z);
        }
        else {
            return new Vec3d(v.x, v.y, value);
        }
    }
    
    public static double getBoxCoordinate(Box box, Direction direction) {
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
    
    public static Box replaceBoxCoordinate(Box box, Direction direction, double coordinate) {
        switch (direction) {
            case DOWN -> {return new Box(box.minX, coordinate, box.minZ, box.maxX, box.maxY, box.maxZ);}
            case UP -> {return new Box(box.minX, box.minY, box.minZ, box.maxX, coordinate, box.maxZ);}
            case NORTH -> {return new Box(box.minX, box.minY, coordinate, box.maxX, box.maxY, box.maxZ);}
            case SOUTH -> {return new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, coordinate);}
            case WEST -> {return new Box(coordinate, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);}
            case EAST -> {return new Box(box.minX, box.minY, box.minZ, coordinate, box.maxY, box.maxZ);}
        }
        throw new RuntimeException();
    }
    
    public static <A, B> Pair<B, A> swaped(Pair<A, B> p) {
        return new Pair<>(p.getRight(), p.getLeft());
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
    
    public static Pair<Direction.Axis, Direction.Axis> getAnotherTwoAxis(Direction.Axis axis) {
        switch (axis) {
            case X:
                return new Pair<>(Direction.Axis.Y, Direction.Axis.Z);
            case Y:
                return new Pair<>(Direction.Axis.Z, Direction.Axis.X);
            case Z:
                return new Pair<>(Direction.Axis.X, Direction.Axis.Y);
        }
        throw new IllegalArgumentException();
    }
    
    public static BlockPos scale(Vec3i v, int m) {
        return new BlockPos(v.getX() * m, v.getY() * m, v.getZ() * m);
    }
    
    public static BlockPos divide(Vec3i v, int d) {
        return new BlockPos(v.getX() / d, v.getY() / d, v.getZ() / d);
    }
    
    public static Direction[] getAnotherFourDirections(Direction.Axis axisOfNormal) {
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = getAnotherTwoAxis(
            axisOfNormal
        );
        return new Direction[]{
            Direction.get(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getLeft()
            ),
            Direction.get(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getRight()
            ),
            Direction.get(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getLeft()
            ),
            Direction.get(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getRight()
            )
        };
    }
    
    public static Pair<Direction, Direction> getPerpendicularDirections(Direction facing) {
        Pair<Direction.Axis, Direction.Axis> axises = getAnotherTwoAxis(facing.getAxis());
        if (facing.getDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Pair<>(axises.getRight(), axises.getLeft());
        }
        return new Pair<>(
            Direction.get(Direction.AxisDirection.POSITIVE, axises.getLeft()),
            Direction.get(Direction.AxisDirection.POSITIVE, axises.getRight())
        );
    }
    
    public static Vec3d getBoxSize(Box box) {
        return new Vec3d(box.getXLength(), box.getYLength(), box.getZLength());
    }
    
    public static Box getBoxSurfaceInversed(Box box, Direction direction) {
        double size = getCoordinate(getBoxSize(box), direction.getAxis());
        Vec3d shrinkVec = Vec3d.of(direction.getVector()).multiply(size);
        return box.shrink(shrinkVec.x, shrinkVec.y, shrinkVec.z);
    }
    
    public static Box getBoxSurface(Box box, Direction direction) {
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
    
    public static Box getBoxByBottomPosAndSize(Vec3d boxBottomCenter, Vec3d viewBoxSize) {
        return new Box(
            boxBottomCenter.subtract(viewBoxSize.x / 2, 0, viewBoxSize.z / 2),
            boxBottomCenter.add(viewBoxSize.x / 2, viewBoxSize.y, viewBoxSize.z / 2)
        );
    }
    
    public static Vec3d getBoxBottomCenter(Box box) {
        return new Vec3d(
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
    
    public static <A, B> Stream<Pair<A, B>> composeTwoStreamsWithEqualLength(
        Stream<A> a,
        Stream<B> b
    ) {
        Iterator<A> aIterator = a.iterator();
        Iterator<B> bIterator = b.iterator();
        Iterator<Pair<A, B>> iterator = new Iterator<Pair<A, B>>() {
            
            @Override
            public boolean hasNext() {
                assert aIterator.hasNext() == bIterator.hasNext();
                return aIterator.hasNext();
            }
            
            @Override
            public Pair<A, B> next() {
                return new Pair<>(aIterator.next(), bIterator.next());
            }
        };
        
        return Streams.stream(iterator);
    }
    
    public static void log(Object str) {
        logger.info(str);
    }
    
    public static void err(Object str) {
        logger.error(str);
    }
    
    public static void dbg(Object str) {
        logger.debug(str);
    }
    
    public static Vec3d[] eightVerticesOf(Box box) {
        return new Vec3d[]{
            new Vec3d(box.minX, box.minY, box.minZ),
            new Vec3d(box.minX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.minZ),
            new Vec3d(box.minX, box.maxY, box.maxZ),
            new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
    }
    
    public static void putVec3d(NbtCompound compoundTag, String name, Vec3d vec3d) {
        compoundTag.putDouble(name + "X", vec3d.x);
        compoundTag.putDouble(name + "Y", vec3d.y);
        compoundTag.putDouble(name + "Z", vec3d.z);
    }
    
    public static Vec3d getVec3d(NbtCompound compoundTag, String name) {
        return new Vec3d(
            compoundTag.getDouble(name + "X"),
            compoundTag.getDouble(name + "Y"),
            compoundTag.getDouble(name + "Z")
        );
    }
    
    public static void putVec3i(NbtCompound compoundTag, String name, Vec3i vec3i) {
        compoundTag.putInt(name + "X", vec3i.getX());
        compoundTag.putInt(name + "Y", vec3i.getY());
        compoundTag.putInt(name + "Z", vec3i.getZ());
    }
    
    public static BlockPos getVec3i(NbtCompound compoundTag, String name) {
        return new BlockPos(
            compoundTag.getInt(name + "X"),
            compoundTag.getInt(name + "Y"),
            compoundTag.getInt(name + "Z")
        );
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
            (Pair<T, S> lastPair, T curr) ->
                new Pair<T, S>(curr, function.apply(lastPair.getLeft(), curr)),
            new SimpleBox<>(new Pair<T, S>(firstValue, null))
        ).map(pair -> pair.getRight());
    }
    
    public static <T> T makeIntoExpression(T t, Consumer<T> func) {
        func.accept(t);
        return t;
    }
    
    public static void putUuid(NbtCompound tag, String key, UUID uuid) {
        tag.putLong(key + "Most", uuid.getMostSignificantBits());
        tag.putLong(key + "Least", uuid.getLeastSignificantBits());
    }
    
    @Nullable
    public static UUID getUuid(NbtCompound tag, String key) {
        String key1 = key + "Most";
        
        if (!tag.contains(key1)) {
            return null;
        }
        
        return new UUID(tag.getLong(key1), tag.getLong(key + "Least"));
    }
    
    public static Vec3d getFlippedVec(Vec3d vec, Vec3d flippingAxis) {
        Vec3d component = getProjection(vec, flippingAxis);
        return vec.subtract(component).multiply(-1).add(component);
    }
    
    public static Vec3d getProjection(Vec3d vec, Vec3d direction) {
        return direction.multiply(vec.dotProduct(direction));
    }
    
    /**
     * @param vec  The {@link Vec3d} to get the {@link Direction} of.
     * @param axis The {@link Direction.Axis} of directions to exclude.
     * @return The {@link Direction} of the passed {@code vec}, excluding directions of axis {@code axis}.
     */
    @SuppressWarnings("WeakerAccess")
    public static Direction getFacingExcludingAxis(Vec3d vec, Direction.Axis axis) {
        return Arrays.stream(Direction.values())
            .filter(d -> d.getAxis() != axis)
            .max(Comparator.comparingDouble(
                dir -> vec.x * dir.getOffsetX() + vec.y * dir.getOffsetY() + vec.z * dir.getOffsetZ()
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
    public static Box transformBox(
        Box box, Function<Vec3d, Vec3d> function
    ) {
        List<Vec3d> result =
            Arrays.stream(eightVerticesOf(box)).map(function).collect(Collectors.toList());
        
        return new Box(
            result.stream().mapToDouble(b -> b.x).min().getAsDouble(),
            result.stream().mapToDouble(b -> b.y).min().getAsDouble(),
            result.stream().mapToDouble(b -> b.z).min().getAsDouble(),
            result.stream().mapToDouble(b -> b.x).max().getAsDouble(),
            result.stream().mapToDouble(b -> b.y).max().getAsDouble(),
            result.stream().mapToDouble(b -> b.z).max().getAsDouble()
        );
    }
    
    private static double getDistanceToRange(double start, double end, double pos) {
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
    
    public static double getDistanceToBox(Box box, Vec3d point) {
        double dx = getDistanceToRange(box.minX, box.maxX, point.x);
        double dy = getDistanceToRange(box.minY, box.maxY, point.y);
        double dz = getDistanceToRange(box.minZ, box.maxZ, point.z);
        
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
        ArrayList<T> arrayList,
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
    
    public static Vec3d interpolatePos(Vec3d from, Vec3d to, double progress) {
        return new Vec3d(
            MathHelper.lerp(progress, from.x, to.x),
            MathHelper.lerp(progress, from.y, to.y),
            MathHelper.lerp(progress, from.z, to.z)
        );
    }
}
