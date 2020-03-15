package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Helper {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static void assertWithSideEffects(boolean cond) {
        //assert cond;
        if (!cond) {
            Helper.err("ASSERTION FAILED");
        }
    }
    
    //copied from Project class
    public static void doTransformation(FloatBuffer m, float[] in, float[] out) {
        for (int i = 0; i < 4; i++) {
            out[i] =
                in[0] * m.get(m.position() + 0 * 4 + i)
                    + in[1] * m.get(m.position() + 1 * 4 + i)
                    + in[2] * m.get(m.position() + 2 * 4 + i)
                    + in[3] * m.get(m.position() + 3 * 4 + i);
            
        }
    }
    
    //NOTE the w value is omitted
    public static Vec3d doTransformation(FloatBuffer m, Vec3d in) {
        float[] input = {(float) in.x, (float) in.y, (float) in.z, 1};
        float[] output = new float[4];
        doTransformation(m, input, output);
        return new Vec3d(output[0], output[1], output[2]);
    }
    
    public static final float[] IDENTITY_MATRIX =
        new float[]{
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f};
    
    private static void loadIdentityMatrix(FloatBuffer m) {
        int oldPos = m.position();
        m.put(IDENTITY_MATRIX);
        m.position(oldPos);
    }
    
    public static FloatBuffer getModelViewMatrix() {
        return getMatrix(GL11.GL_MODELVIEW_MATRIX);
    }
    
    public static FloatBuffer getProjectionMatrix() {
        return getMatrix(GL11.GL_PROJECTION_MATRIX);
    }
    
    public static FloatBuffer getTextureMatrix() {
        return getMatrix(GL11.GL_TEXTURE_MATRIX);
    }
    
    public static FloatBuffer getMatrix(int matrixId) {
        FloatBuffer temp = BufferUtils.createFloatBuffer(16);
        
        GL11.glGetFloatv(matrixId, temp);
        
        return temp;
    }
    
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
    
    @Deprecated
    public static Pair<Direction.Axis, Direction.Axis> getPerpendicularAxis(Direction facing) {
        Pair<Direction.Axis, Direction.Axis> axises = getAnotherTwoAxis(facing.getAxis());
        if (facing.getDirection() == Direction.AxisDirection.NEGATIVE) {
            axises = new Pair<>(axises.getRight(), axises.getLeft());
        }
        return axises;
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
    
    public static Box getBoxSurface(Box box, Direction direction) {
        double size = getCoordinate(getBoxSize(box), direction.getAxis());
        Vec3d shrinkVec =  Vec3d.method_24954(direction.getVector()).multiply(size);
        return box.shrink(shrinkVec.x, shrinkVec.y, shrinkVec.z);
    }
    
    public static IntegerAABBInclusive expandRectangle(
        BlockPos startingPos,
        Predicate<BlockPos> blockPosPredicate, Direction.Axis axis
    ) {
        IntegerAABBInclusive wallArea = new IntegerAABBInclusive(startingPos, startingPos);
        
        for (Direction direction : getAnotherFourDirections(axis)) {
            
            wallArea = expandArea(
                wallArea,
                blockPosPredicate,
                direction
            );
        }
        return wallArea;
    }
    
    
    public static class SimpleBox<T> {
        public T obj;
        
        public SimpleBox(T obj) {
            this.obj = obj;
        }
    }
    
    //@Nullable
    public static <T> T getLastSatisfying(Stream<T> stream, Predicate<T> predicate) {
        SimpleBox<T> box = new SimpleBox<T>(null);
        stream.filter(curr -> {
            if (predicate.test(curr)) {
                box.obj = curr;
                return false;
            }
            else {
                return true;
            }
        }).findFirst();
        return box.obj;
    }
    
    public interface CallableWithoutException<T> {
        public T run();
    }
    
    public static Vec3d interpolatePos(Entity entity, float partialTicks) {
        Vec3d currPos = entity.getPos();
        Vec3d lastTickPos = McHelper.lastTickPosOf(entity);
        return lastTickPos.add(currPos.subtract(lastTickPos).multiply(partialTicks));
    }
    
    public static Runnable noException(Callable func) {
        return () -> {
            try {
                func.call();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
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
    
    //sometimes it catches all types of exception
    //and the exception message will be hided
    //use this to avoid that
    public static <T> T doNotEatExceptionMessage(
        Supplier<T> func
    ) {
        try {
            return func.get();
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
    
    //NOTE this is not concatenation, it's composing
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
        LOGGER.info("[Portal] " + str);
    }
    
    public static void err(Object str) {
        LOGGER.error("[Portal] " + str);
    }
    
    public static void dbg(Object str) {
        LOGGER.debug("[Portal] " + str);
    }
    
    public static Vec3d[] eightVerticesOf(Box box) {
        return new Vec3d[]{
            new Vec3d(box.x1, box.y1, box.z1),
            new Vec3d(box.x1, box.y1, box.z2),
            new Vec3d(box.x1, box.y2, box.z1),
            new Vec3d(box.x1, box.y2, box.z2),
            new Vec3d(box.x2, box.y1, box.z1),
            new Vec3d(box.x2, box.y1, box.z2),
            new Vec3d(box.x2, box.y2, box.z1),
            new Vec3d(box.x2, box.y2, box.z2)
        };
    }
    
    public static void putVec3d(CompoundTag compoundTag, String name, Vec3d vec3d) {
        compoundTag.putDouble(name + "X", vec3d.x);
        compoundTag.putDouble(name + "Y", vec3d.y);
        compoundTag.putDouble(name + "Z", vec3d.z);
    }
    
    public static Vec3d getVec3d(CompoundTag compoundTag, String name) {
        return new Vec3d(
            compoundTag.getDouble(name + "X"),
            compoundTag.getDouble(name + "Y"),
            compoundTag.getDouble(name + "Z")
        );
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
    
    public static IntegerAABBInclusive expandArea(
        IntegerAABBInclusive originalArea,
        Predicate<BlockPos> predicate,
        Direction direction
    ) {
        IntegerAABBInclusive currentBox = originalArea;
        for (int i = 1; i < 42; i++) {
            IntegerAABBInclusive expanded = currentBox.getExpanded(direction, 1);
            if (expanded.getSurfaceLayer(direction).stream().allMatch(predicate)) {
                currentBox = expanded;
            }
            else {
                return currentBox;
            }
        }
        return currentBox;
    }
    
    public static <A, B> B reduceWithDifferentType(
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
//        SimpleBox<B> bBox = new SimpleBox<>(start);
//        stream.forEach(a -> {
//            bBox.obj = func.apply(bBox.obj, a);
//        });
//        return bBox.obj;
    }
    
    public static <T> T noError(Callable<T> func) {
        try {
            return func.call();
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static interface ExceptionalRunnable {
        void run() throws Throwable;
    }
    
    public static void noError(ExceptionalRunnable runnable) {
        try {
            runnable.run();
        }
        catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
    
    //ObjectList does not override removeIf() so its complexity is O(n^2)
    //this is O(n)
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
    
    //NOTE this will mutate a and return a
    public static Quaternion quaternionNumAdd(Quaternion a, Quaternion b) {
        //TODO correct wrong parameter name for yarn
        a.set(
            a.getB() + b.getB(),
            a.getC() + b.getC(),
            a.getD() + b.getD(),
            a.getA() + b.getA()
        );
        return a;
    }
    
    //NOTE this will mutate a and reutrn a
    public static Quaternion quaternionScale(Quaternion a, float scale) {
        a.set(
            a.getB() * scale,
            a.getC() * scale,
            a.getD() * scale,
            a.getA() * scale
        );
        return a;
    }
    
    //a quaternion is a 4d vector on 4d sphere
    public static Quaternion interpolateQuaternion(
        Quaternion v0,
        Quaternion v1,
        float t
    ) {
        v0.normalize();
        v1.normalize();
    
        double dot = v0.getA() * v1.getA() +
            v0.getB() * v1.getB() +
            v0.getC() * v1.getC() +
            v0.getD() * v1.getD();

//        if (dot < 0.0f) {
//            v1.scale(-1);
//            dot = -dot;
//        }
    
        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.
        
            Quaternion result = quaternionNumAdd(
                quaternionScale(v0, 1 - t),
                quaternionScale(v1, t)
            );
            result.normalize();
            return result;
        }
        
        // Since dot is in range [0, DOT_THRESHOLD], acos is safe
        double theta_0 = Math.acos(dot);        // theta_0 = angle between input vectors
        double theta = theta_0 * t;          // theta = angle between v0 and result
        double sin_theta = Math.sin(theta);     // compute this value only once
        double sin_theta_0 = Math.sin(theta_0); // compute this value only once
    
        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;  // == sin(theta_0 - theta) / sin(theta_0)
        double s1 = sin_theta / sin_theta_0;
    
        return quaternionNumAdd(
            quaternionScale(v0, (float) s0),
            quaternionScale(v1, (float) s1)
        );
    }
    
    public static boolean isClose(Quaternion a, Quaternion b, float valve) {
        a.normalize();
        b.normalize();
        if (a.getA() * b.getA() < 0) {
            a.scale(-1);
        }
        float da = a.getA() - b.getA();
        float db = a.getB() - b.getB();
        float dc = a.getC() - b.getC();
        float dd = a.getD() - b.getD();
        return da * da + db * db + dc * dc + dd * dd < valve;
    }
    
    public static Vec3d getRotated(Quaternion rotation, Vec3d vec) {
        Vector3f vector3f = new Vector3f(vec);
        vector3f.rotate(rotation);
        return new Vec3d(vector3f);
    }
}
