package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IERayTraceContext;
import com.qouteall.immersive_portals.ducks.IEWorldChunk;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.Portal;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.TypeFilterableList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Helper {
    
    private static final Logger LOGGER = LogManager.getLogger("Portal");
    
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
        Vec3d shrinkVec = new Vec3d(direction.getVector()).multiply(size);
        return box.shrink(shrinkVec.x, shrinkVec.y, shrinkVec.z);
    }
    
    public static IntBox expandRectangle(
        BlockPos startingPos,
        Predicate<BlockPos> blockPosPredicate, Direction.Axis axis
    ) {
        IntBox wallArea = new IntBox(startingPos, startingPos);
        
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
        LOGGER.info(str);
    }
    
    public static void err(Object str) {
        LOGGER.error(str);
    }
    
    public static void dbg(Object str) {
        LOGGER.debug(str);
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
    //this method may mutate argument but will not change rotation
    public static Quaternion interpolateQuaternion(
        Quaternion a,
        Quaternion b,
        float t
    ) {
        a.normalize();
        b.normalize();
        
        double dot = dotProduct4d(a, b);
        
        if (dot < 0.0f) {
            a.scale(-1);
            dot = -dot;
        }
        
        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.
            
            Quaternion result = quaternionNumAdd(
                quaternionScale(a.copy(), 1 - t),
                quaternionScale(b.copy(), t)
            );
            result.normalize();
            return result;
        }
        
        double theta_0 = Math.acos(dot);
        double theta = theta_0 * t;
        double sin_theta = Math.sin(theta);
        double sin_theta_0 = Math.sin(theta_0);
        
        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;
        double s1 = sin_theta / sin_theta_0;
        
        return quaternionNumAdd(
            quaternionScale(a.copy(), (float) s0),
            quaternionScale(b.copy(), (float) s1)
        );
    }
    
    public static double dotProduct4d(Quaternion a, Quaternion b) {
        return a.getA() * b.getA() +
            a.getB() * b.getB() +
            a.getC() * b.getC() +
            a.getD() * b.getD();
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
    
    public static Quaternion ortholize(Quaternion quaternion) {
        if (quaternion.getA() < 0) {
            quaternion.scale(-1);
        }
        return quaternion;
    }
    
    //naive interpolation is better?
    //not better
    public static Quaternion interpolateQuaternionNaive(
        Quaternion a,
        Quaternion b,
        float t
    ) {
        return makeIntoExpression(
            new Quaternion(
                MathHelper.lerp(t, a.getB(), b.getB()),
                MathHelper.lerp(t, a.getC(), b.getC()),
                MathHelper.lerp(t, a.getD(), b.getD()),
                MathHelper.lerp(t, a.getA(), b.getA())
            ),
            Quaternion::normalize
        );
    }

    /**
     * Searches nearby chunks to look for a certain sub/class of entity. In the specified {@code world}, the chunk that
     * {@code pos} is in will be used as the center of search. That chunk will be expanded by {@code chunkRadius} chunks
     * in all directions to define the search area. Then, on all Y levels, those chunks will be searched for entities of
     * class {@code entityClass}. Then all entities found will be returned.
     * <p>
     * If you define a {@code chunkRadius} of 1, 9 chunks will be searched. If you define one of 2, then 25 chunks will
     * be searched. This can be an extreme performance bottleneck, so yse it sparingly such as a response to user input.
     *
     * @param world       The world in which to search for entities.
     * @param pos         The chunk that this position is located in will be used as the center of search.
     * @param chunkRadius Integer number of chunks to expand the square search area by.
     * @param entityClass The entity class to search for.
     * @param <T>         The entity class that will be returned in the list.
     * @return All entities in the nearby chunks with type T.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static <T extends Entity> List<T> getNearbyEntities(World world, Vec3d pos, int chunkRadius, Class<T> entityClass) {
        ArrayList<T> entities = new ArrayList<>();
        int chunkX = (int) pos.x / 16;
        int chunkZ = (int) pos.z / 16;

        for (int z = -chunkRadius + 1; z < chunkRadius; z++) {
            for (int x = -chunkRadius + 1; x < chunkRadius; x++) {
                int aX = chunkX + x;
                int aZ = chunkZ + z;

                // WorldChunk contains a private variable called entitySections that groups all entities in the chunk by
                // their Y level. Here we are using a Mixin duck typing interface thing to get that private variable and
                // then manually search it. This is faster than using the built-in WorldChunk methods that do not do
                // what we want.
                TypeFilterableList<Entity>[] entitySections = ((IEWorldChunk) world.getChunk(aX, aZ)).getEntitySections();
                for (TypeFilterableList<Entity> entitySection : entitySections) {
                    entities.addAll(entitySection.getAllOfType(entityClass));
                }
            }
        }

        return entities;
    }

    /**
     * Returns all portals intersecting the line from start->end.
     *
     * @param world                The world in which to ray trace for portals.
     * @param start                The start of the line defining the ray to trace.
     * @param end                  The end of the line defining the ray to trace.
     * @param includeGlobalPortals Whether or not to include global portals in the ray trace.
     * @param filter               Filter the portals that this function returns. Nullable
     * @return A list of portals and their intersection points with the line, sorted by nearest portals first.
     * @author LoganDark
     */
    @SuppressWarnings("WeakerAccess")
    public static List<Pair<Portal, Vec3d>> rayTracePortals(World world, Vec3d start, Vec3d end, boolean includeGlobalPortals, Predicate<Portal> filter) {
        // This will be the center of the chunk search, rather than using start or end. This will allow the radius to be
        // smaller, and as a result, the search to be faster and slightly less inefficient.
        //
        // The searching method employed by getNearbyEntities is still not ideal, but it's the best idea I have.
        Vec3d middle = start.multiply(0.5).add(end.multiply(0.5));

        // This could result in searching more chunks than necessary, but it always expands to completely cover any
        // chunks the line from start->end passes through.
        int chunkRadius = (int) Math.ceil(Math.abs(start.distanceTo(end) / 2) / 16);
        List<Portal> nearby = getNearbyEntities(world, middle, chunkRadius, Portal.class);

        if (includeGlobalPortals) {
            nearby.addAll(McHelper.getGlobalPortals(world));
        }

        // Make a list of all portals actually intersecting with this line, and then sort them by the distance from the
        // start position. Nearest portals first.
        List<Pair<Portal, Vec3d>> hits = new ArrayList<>();

        nearby.forEach(portal -> {
            if (filter == null || filter.test(portal)) {
                Vec3d intersection = portal.rayTrace(start, end);

                if (intersection != null) {
                    hits.add(new Pair<>(portal, intersection));
                }
            }
        });

        hits.sort((pair1, pair2) -> {
            Vec3d intersection1 = pair1.getRight();
            Vec3d intersection2 = pair2.getRight();

            // Return a negative number if intersection1 is smaller (should come first)
            return (int) Math.signum(intersection1.squaredDistanceTo(start) - intersection2.squaredDistanceTo(start));
        });

        return hits;
    }

    /**
     * @see #withSwitchedContext(World, Supplier) 
     */
    @Environment(EnvType.CLIENT)
    private static <T> T withSwitchedContextClient(ClientWorld world, Supplier<T> func) {
        boolean wasSwitched = BlockManipulationClient.isContextSwitched;
        BlockManipulationClient.isContextSwitched = true;

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld lastWorld = mc.world;
        mc.world = world;

        try {
            return func.get();
        } finally {
            mc.world = lastWorld;
            BlockManipulationClient.isContextSwitched = wasSwitched;
        }
    }

    /**
     * @see #withSwitchedContext(World, Supplier)
     */
    @SuppressWarnings("unused")
    private static <T> T withSwitchedContextServer(ServerWorld world, Supplier<T> func) {
        // lol
        return func.get();
    }

    /**
     * Execute {@code func} with the world being set to {@code world}, hopefully bypassing any issues that may be
     * related to mutating a world that is not currently set as the current world.
     * <p>
     * You may safely nest this function within other context switches. It works on both the client and the server.
     *
     * @param world The world to switch the context to. The context will be restored when {@code func} is complete.
     * @param func  The function to execute while the context is switched.
     * @param <T>   The return type of {@code func}.
     * @return Whatever {@code func} returned.
     */
    private static <T> T withSwitchedContext(World world, Supplier<T> func) {
        if (world.isClient) {
            return withSwitchedContextClient((ClientWorld) world, func);
        } else {
            return withSwitchedContextServer((ServerWorld) world, func);
        }
    }

    /**
     * @see Helper#rayTrace(World, RayTraceContext, boolean)
     * @author LoganDark
     */
    private static Pair<BlockHitResult, List<Portal>> rayTrace(
        World world,
        RayTraceContext context,
        boolean includeGlobalPortals,
        List<Portal> portals
    ) {
        Vec3d start = context.getStart();
        Vec3d end = context.getEnd();

        // If we're past the max portal layer, don't let the player target behind this portal, create a missed result
        if (portals.size() > Global.maxPortalLayer) {
            Vec3d diff = end.subtract(start);

            return new Pair<>(
                BlockHitResult.createMissed(
                    end,
                    Direction.getFacing(diff.x, diff.y, diff.z),
                    new BlockPos(end)
                ),
                portals
            );
        }

        // First ray trace normally
        BlockHitResult hitResult = world.rayTrace(context);

        List<Pair<Portal, Vec3d>> rayTracedPortals = withSwitchedContext(world,
            () -> rayTracePortals(world, start, end, includeGlobalPortals, Portal::isInteractable)
        );

        if (rayTracedPortals.isEmpty()) {
            return new Pair<>(hitResult, portals);
        }

        Pair<Portal, Vec3d> portalHit = rayTracedPortals.get(0);
        Portal portal = portalHit.getLeft();
        Vec3d intersection = portalHit.getRight();

        // If the portal is not closer, return the hit result we just got
        if (hitResult.getPos().squaredDistanceTo(start) < intersection.squaredDistanceTo(start)) {
            return new Pair<>(hitResult, portals);
        }

        // If the portal is closer, recurse

        IERayTraceContext betterContext = (IERayTraceContext) context;

        betterContext
            .setStart(portal.transformPoint(intersection))
            .setEnd(portal.transformPoint(end));

        portals.add(portal);
        Pair<BlockHitResult, List<Portal>> recursion =
            rayTrace(portal.getDestinationWorld(world.isClient), context, includeGlobalPortals, portals);

        betterContext
            .setStart(start)
            .setEnd(end);

        return recursion;
    }

    /**
     * Ray traces for blocks or whatever the {@code context} dictates.
     *
     * @param world                The world to ray trace in.
     * @param context              The ray tracing context to use. This context will be mutated as it goes but will be
     *                             returned back to normal before a result is returned to you, so you can act like it
     *                             hasn't been  mutated.
     * @param includeGlobalPortals Whether or not to include global portals in the ray trace. If this is false, then the
     *                             ray trace can pass right through them.
     * @return The BlockHitResult and the list of portals that we've passed through to get there. This list can be used
     * to transform looking directions or do whatever you want really.
     * @author LoganDark
     */
    public static Pair<BlockHitResult, List<Portal>> rayTrace(
        World world,
        RayTraceContext context,
        boolean includeGlobalPortals
    ) {
        return rayTrace(world, context, includeGlobalPortals, new ArrayList<>());
    }

    /**
     * @param hitResult The HitResult to check.
     * @return If the HitResult passed is either {@code null}, or of type {@link HitResult.Type#MISS}.
     */
    public static boolean hitResultIsMissedOrNull(HitResult hitResult) {
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    /**
     * @param vec  The {@link Vec3d} to get the {@link Direction} of.
     * @param axis The {@link Direction.Axis} of directions to exclude.
     * @return The {@link Direction} of the passed {@code vec}, excluding directions of axis {@code axis}.
     */
    public static Direction getFacingExcludingAxis(Vec3d vec, Direction.Axis axis) {
        Stream<Direction> directions = Arrays.stream(Direction.values()).filter(d -> !d.getAxis().equals(axis));
        Direction samestDirection = Direction.NORTH;
        double bestSameness = Double.MIN_VALUE;

        for (Iterator<Direction> it = directions.iterator(); it.hasNext(); ) {
            Direction dir = it.next();
            double sameness = vec.x * dir.getOffsetX() + vec.y * dir.getOffsetY() + vec.z * dir.getOffsetZ();

            if (sameness > bestSameness) {
                bestSameness = sameness;
                samestDirection = dir;
            }
        }

        return samestDirection;
    }

    /**
     * Places a portal based on {@code entity}'s looking direction. Does not set the portal destination or add it to the
     * world, you will have to do that yourself.
     *
     * @param width  The width of the portal.
     * @param height The height of the portal.
     * @param entity The entity to place this portal as.
     * @return The placed portal, with no destination set.
     * @author LoganDark
     */
    public static Portal placePortal(double width, double height, Entity entity) {
        Vec3d playerLook = entity.getRotationVector();

        Pair<BlockHitResult, List<Portal>> rayTrace =
            rayTrace(
                entity.world,
                new RayTraceContext(
                    entity.getCameraPosVec(1.0f),
                    entity.getCameraPosVec(1.0f).add(playerLook.multiply(100.0)),
                    RayTraceContext.ShapeType.OUTLINE,
                    RayTraceContext.FluidHandling.NONE,
                    entity
                ),
                true
            );

        BlockHitResult hitResult = rayTrace.getLeft();
        List<Portal> hitPortals = rayTrace.getRight();

        if (hitResultIsMissedOrNull(hitResult)) {
            return null;
        }

        for (Portal hitPortal : hitPortals) {
            playerLook = hitPortal.transformLocalVec(playerLook);
        }

        Direction lookingDirection = getFacingExcludingAxis(playerLook, hitResult.getSide().getAxis());

        Vec3d axisH = new Vec3d(hitResult.getSide().getVector());
        Vec3d axisW = axisH.crossProduct(new Vec3d(lookingDirection.getOpposite().getVector()));
        Vec3d pos = new Vec3d(hitResult.getBlockPos()).add(.5, .5, .5)
            .add(axisH.multiply(0.5 + height / 2));

        World world = hitPortals.isEmpty()
            ? entity.world
            : hitPortals.get(hitPortals.size() - 1).getDestinationWorld(false);

        Portal portal = new Portal(Portal.entityType, world);

        portal.setPos(pos.x, pos.y, pos.z);

        portal.axisW = axisW;
        portal.axisH = axisH;

        portal.width = width;
        portal.height = height;

        return portal;
    }
}
