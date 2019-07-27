package com.qouteall.immersive_portals.my_util;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import net.minecraft.util.Pair;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;

//import com.sun.istack.internal.NotNull;
//import com.sun.istack.internal.Nullable;


public class Helper {
    
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
        Vec3d portalCenter,
        Vec3d portalNormal,
        Vec3d lineCenter,
        Vec3d lineDirection
    ) {
        return (portalCenter.subtract(lineCenter).dotProduct(portalNormal))
            /
            (lineDirection.dotProduct(portalNormal));
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
                return new Pair<>(Direction.Axis.Z, Direction.Axis.Z);
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
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getLeft()
            ),
            Direction.get(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getLeft()
            ),
            Direction.get(
                Direction.AxisDirection.NEGATIVE, anotherTwoAxis.getRight()
            ),
            Direction.get(
                Direction.AxisDirection.POSITIVE, anotherTwoAxis.getRight()
            )
        };
    }
    
    public static IEThreadedAnvilChunkStorage getIEStorage(DimensionType dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkManager) Helper.getServer().getWorld(dimension).getChunkManager()
        ).threadedAnvilChunkStorage;
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
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
    }
    
    public static Vec3d interpolatePos(Entity entity, float partialTicks) {
        Vec3d currPos = entity.getPos();
        Vec3d lastTickPos = lastTickPosOf(entity);
        return lastTickPos.add(currPos.subtract(lastTickPos).multiply(partialTicks));
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3d pos,
        Vec3d lastTickPos
    ) {
        
        
        //NOTE do not call entity.setPosition() because it may tick the entity
        
        entity.x = pos.x;
        entity.y = pos.y;
        entity.z = pos.z;
        entity.prevRenderX = lastTickPos.x;
        entity.prevRenderY = lastTickPos.y;
        entity.prevRenderZ = lastTickPos.z;
        entity.prevX = lastTickPos.x;
        entity.prevY = lastTickPos.y;
        entity.prevZ = lastTickPos.z;
    }
    
    public static WeakReference<MinecraftServer> refMinecraftServer;
    
    public static MinecraftServer getServer() {
        return refMinecraftServer.get();
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
    
    public static <MSG> void sendToServer(MSG message) {
        assert false;
    }
    
    public static <MSG> void sendToPlayer(ServerPlayerEntity player, MSG message) {
        assert false;
    }
    
    public static void checkGlError() {
        int errorCode = GL11.glGetError();
        if (errorCode != GL_NO_ERROR) {
            System.err.print("OPENGL ERROR ");
            Helper.err(errorCode);
        }
    }
    
    public static ServerWorld getOverWorldOnServer() {
        return getServer().getWorld(DimensionType.OVERWORLD);
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
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        //Helper.log(text);
        
        player.sendMessage(new LiteralText(text));
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
    
    @Deprecated
    public static ServerWorld getWorldAndLoad(DimensionType dimension) {
        return getServer().getWorld(dimension);
    }
    
    @Deprecated
    public static ServerWorld getWorldIfLoaded(DimensionType dimension) {
        return getServer().getWorld(dimension);
    }
    
    public static ClientWorld loadClientWorld(DimensionType dimension) {
        return Globals.clientWorldLoader.getOrCreateFakedWorld(dimension);
    }
    
    public static void log(Object str) {
        System.out.println(str);
    }
    
    public static void err(Object str) {
        System.err.println(str);
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
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        Box box = new Box(center, center).expand(range);
        return (Stream) world.getEntities(entityClass, box).stream();
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        Entity center,
        Class<ENTITY> entityClass,
        double range
    ) {
        return getEntitiesNearby(
            center.world,
            center.getPos(),
            entityClass,
            range
        );
    }
    
    public static Box getChunkBoundingBox(ChunkPos chunkPos) {
        return new Box(
            chunkPos.getCenterBlockPos(),
            chunkPos.getCenterBlockPos().add(16, 256, 16)
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
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getTime();
    }
    
    public static long secondToNano(double second) {
        return (long) (second * 1000000000L);
    }
}
