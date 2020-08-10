package com.qouteall.immersive_portals;

import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.ducks.IEWorldChunk;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class McHelper {
    
    public static WeakReference<MinecraftServer> refMinecraftServer =
        new WeakReference<>(null);
    
    public static IEThreadedAnvilChunkStorage getIEStorage(RegistryKey<World> dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkManager) getServer().getWorld(dimension).getChunkManager()
        ).threadedAnvilChunkStorage;
    }
    
    public static ArrayList<ServerPlayerEntity> getCopiedPlayerList() {
        return new ArrayList<>(getServer().getPlayerManager().getPlayerList());
    }
    
    public static List<ServerPlayerEntity> getRawPlayerList() {
        return getServer().getPlayerManager().getPlayerList();
    }
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
    }
    
    public static MinecraftServer getServer() {
        return refMinecraftServer.get();
    }
    
    public static ServerWorld getOverWorldOnServer() {
        return getServer().getWorld(World.OVERWORLD);
    }
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        player.sendMessage(new LiteralText(text), false);
    }
    
    public static Box getChunkBoundingBox(ChunkPos chunkPos) {
        return new Box(
            chunkPos.getCenterBlockPos(),
            chunkPos.getCenterBlockPos().add(16, 256, 16)
        );
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getTime();
    }
    
    public static <T> void performFindingTaskOnServer(
        boolean isMultithreaded,
        Stream<T> stream,
        Predicate<T> predicate,
        IntPredicate taskWatcher,//return false to abort the task
        Consumer<T> onFound,
        Runnable onNotFound,
        Runnable finalizer
    ) {
        if (isMultithreaded) {
            performMultiThreadedFindingTaskOnServer(
                stream, predicate, taskWatcher, onFound, onNotFound, finalizer
            );
        }
        else {
            performSplittedFindingTaskOnServer(
                stream, predicate, taskWatcher, onFound, onNotFound, finalizer
            );
        }
    }
    
    public static <T> void performSplittedFindingTaskOnServer(
        Stream<T> stream,
        Predicate<T> predicate,
        IntPredicate taskWatcher,//return false to abort the task
        Consumer<T> onFound,
        Runnable onNotFound,
        Runnable finalizer
    ) {
        final long timeValve = (1000000000L / 50);
        int[] countStorage = new int[1];
        countStorage[0] = 0;
        Iterator<T> iterator = stream.iterator();
        ModMain.serverTaskList.addTask(() -> {
            boolean shouldContinueRunning =
                taskWatcher.test(countStorage[0]);
            if (!shouldContinueRunning) {
                finalizer.run();
                return true;
            }
            long startTime = System.nanoTime();
            for (; ; ) {
                for (int i = 0; i < 300; i++) {
                    if (iterator.hasNext()) {
                        T next = iterator.next();
                        if (predicate.test(next)) {
                            onFound.accept(next);
                            finalizer.run();
                            return true;
                        }
                        countStorage[0] += 1;
                    }
                    else {
                        //finished searching
                        onNotFound.run();
                        finalizer.run();
                        return true;
                    }
                }
                
                long currTime = System.nanoTime();
                
                if (currTime - startTime > timeValve) {
                    //suspend the task and retry it next tick
                    return false;
                }
            }
        });
    }
    
    public static <T> void performMultiThreadedFindingTaskOnServer(
        Stream<T> stream,
        Predicate<T> predicate,
        IntPredicate taskWatcher,//return false to abort the task
        Consumer<T> onFound,
        Runnable onNotFound,
        Runnable finalizer
    ) {
        int[] progress = new int[1];
        Helper.SimpleBox<Boolean> isAborted = new Helper.SimpleBox<>(false);
        Helper.SimpleBox<Runnable> finishBehavior = new Helper.SimpleBox<>(() -> {
            Helper.err("Error Occured???");
        });
        CompletableFuture<Void> future = CompletableFuture.runAsync(
            () -> {
                try {
                    T result = stream.peek(
                        obj -> {
                            progress[0] += 1;
                        }
                    ).filter(
                        predicate
                    ).findFirst().orElse(null);
                    if (result != null) {
                        finishBehavior.obj = () -> onFound.accept(result);
                    }
                    else {
                        finishBehavior.obj = onNotFound;
                    }
                }
                catch (Throwable t) {
                    t.printStackTrace();
                    finishBehavior.obj = () -> {
                        t.printStackTrace();
                    };
                }
            },
            Util.getServerWorkerExecutor()
        );
        ModMain.serverTaskList.addTask(() -> {
            if (future.isDone()) {
                if (!isAborted.obj) {
                    finishBehavior.obj.run();
                    finalizer.run();
                }
                else {
                    Helper.log("Future done but the task is aborted");
                }
                return true;
            }
            if (future.isCancelled()) {
                Helper.err("The future is cancelled???");
                finalizer.run();
                return true;
            }
            if (future.isCompletedExceptionally()) {
                Helper.err("The future is completed exceptionally???");
                finalizer.run();
                return true;
            }
            boolean shouldContinue = taskWatcher.test(progress[0]);
            if (!shouldContinue) {
                isAborted.obj = true;
                future.cancel(true);
                finalizer.run();
                return true;
            }
            else {
                return false;
            }
        });
    }
    
    // TODO remove this
    public static <ENTITY extends Entity> Stream<ENTITY> getEntitiesNearby(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        Box box = new Box(center, center).expand(range);
        return (Stream) world.getEntities(entityClass, box, e -> true).stream();
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
    
    public static void runWithTransformation(
        MatrixStack matrixStack,
        Runnable renderingFunc
    ) {
        transformationPush(matrixStack);
        renderingFunc.run();
        transformationPop();
    }
    
    public static void transformationPop() {
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();
    }
    
    public static void transformationPush(MatrixStack matrixStack) {
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.multMatrix(matrixStack.peek().getModel());
    }
    
    public static List<GlobalTrackedPortal> getGlobalPortals(World world) {
        List<GlobalTrackedPortal> result;
        if (world.isClient()) {
            result = CHelper.getClientGlobalPortal(world);
        }
        else if (world instanceof ServerWorld) {
            result = GlobalPortalStorage.get(((ServerWorld) world)).data;
        }
        else {
            result = null;
        }
        return result != null ? result : Collections.emptyList();
    }
    
    public static Stream<Portal> getServerPortalsNearby(Entity center, double range) {
        List<GlobalTrackedPortal> globalPortals = GlobalPortalStorage.get(((ServerWorld) center.world)).data;
        Stream<Portal> nearbyPortals = McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
            center.world,
            center.getPos(),
            Portal.class,
            range
        );
        if (globalPortals == null) {
            return nearbyPortals;
        }
        else {
            return Streams.concat(
                globalPortals.stream().filter(
                    p -> p.getDistanceToNearestPointInPortal(center.getPos()) < range * 2
                ),
                nearbyPortals
            );
        }
    }
    
    public static int getRenderDistanceOnServer() {
        return getIEStorage(World.OVERWORLD).getWatchDistance();
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3d pos,
        Vec3d lastTickPos
    ) {
        
        
        //NOTE do not call entity.setPosition() because it may tick the entity
        entity.setPos(pos.x, pos.y, pos.z);
        entity.lastRenderX = lastTickPos.x;
        entity.lastRenderY = lastTickPos.y;
        entity.lastRenderZ = lastTickPos.z;
        entity.prevX = lastTickPos.x;
        entity.prevY = lastTickPos.y;
        entity.prevZ = lastTickPos.z;
    }
    
    public static Vec3d getEyePos(Entity entity) {
        float eyeHeight = entity.getStandingEyeHeight();
        return entity.getPos().add(0, eyeHeight, 0);
    }
    
    public static Vec3d getLastTickEyePos(Entity entity) {
        float eyeHeight = entity.getStandingEyeHeight();
        return lastTickPosOf(entity).add(0, eyeHeight, 0);
    }
    
    public static void setEyePos(Entity entity, Vec3d eyePos, Vec3d lastTickEyePos) {
        float eyeHeight = entity.getStandingEyeHeight();
        setPosAndLastTickPos(
            entity,
            eyePos.add(0, -eyeHeight, 0),
            lastTickEyePos.add(0, -eyeHeight, 0)
        );
    }
    
    public static double getVehicleY(Entity vehicle, Entity passenger) {
        return passenger.getY() - vehicle.getMountedHeightOffset() - passenger.getHeightOffset();
    }
    
    public static void adjustVehicle(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return;
        }
        
        vehicle.updatePosition(
            entity.getX(),
            getVehicleY(vehicle, entity),
            entity.getZ()
        );
    }
    
    public static WorldChunk getServerChunkIfPresent(
        RegistryKey<World> dimension,
        int x, int z
    ) {
        //TODO cleanup
        ChunkHolder chunkHolder_ = getIEStorage(dimension).getChunkHolder_(ChunkPos.toLong(x, z));
        if (chunkHolder_ == null) {
            return null;
        }
        return chunkHolder_.getWorldChunk();
    }
    
    public static WorldChunk getServerChunkIfPresent(
        ServerWorld world, int x, int z
    ) {
        ChunkHolder chunkHolder_ = ((IEThreadedAnvilChunkStorage) (
            (ServerChunkManager) world.getChunkManager()
        ).threadedAnvilChunkStorage).getChunkHolder_(ChunkPos.toLong(x, z));
        if (chunkHolder_ == null) {
            return null;
        }
        return chunkHolder_.getWorldChunk();
    }
    
    public static <ENTITY extends Entity> Stream<ENTITY> getServerEntitiesNearbyWithoutLoadingChunk(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        return McHelper.findEntitiesRough(
            entityClass,
            world,
            center,
            (int) (range / 16),
            e -> true
        ).stream();

//        Box box = new Box(center, center).expand(range);
//        return (Stream) ((IEServerWorld) world).getEntitiesWithoutImmediateChunkLoading(
//            entityClass,
//            box,
//            e -> true
//        ).stream();
    }
    
    public static void updateBoundingBox(Entity player) {
        player.updatePosition(player.getX(), player.getY(), player.getZ());
    }
    
    public static <T extends Entity> List<T> getEntitiesRegardingLargeEntities(
        World world,
        Box box,
        double maxEntitySizeHalf,
        Class<T> entityClass,
        Predicate<T> predicate
    ) {
        return findEntitiesByBox(
            entityClass,
            world,
            box,
            maxEntitySizeHalf,
            predicate
        );
    }
    
    
    //avoid dedicated server crash
    public static void onClientEntityTick(Entity entity) {
        CrossPortalEntityRenderer.onEntityTickClient(entity);
    }
    
    public static interface ChunkAccessor {
        WorldChunk getChunk(int x, int z);
    }
    
    public static ChunkAccessor getChunkAccessor(World world) {
        if (world.isClient()) {
            return world::getChunk;
        }
        else {
            return (x, z) -> getServerChunkIfPresent(((ServerWorld) world), x, z);
        }
    }
    
    public static <T extends Entity> List<T> findEntities(
        Class<T> entityClass,
        ChunkAccessor chunkAccessor,
        int chunkXStart,
        int chunkXEnd,
        int chunkYStart,
        int chunkYEnd,
        int chunkZStart,
        int chunkZEnd,
        Predicate<T> predicate
    ) {
        ArrayList<T> result = new ArrayList<>();
        for (int x = chunkXStart; x <= chunkXEnd; x++) {
            for (int z = chunkZStart; z <= chunkZEnd; z++) {
                WorldChunk chunk = chunkAccessor.getChunk(x, z);
                if (chunk != null) {
                    TypeFilterableList<Entity>[] entitySections =
                        ((IEWorldChunk) chunk).getEntitySections();
                    for (int i = chunkYStart; i <= chunkYEnd; i++) {
                        TypeFilterableList<Entity> entitySection = entitySections[i];
                        for (T entity : entitySection.getAllOfType(entityClass)) {
                            if (predicate.test(entity)) {
                                result.add(entity);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    //faster
    public static <T extends Entity> List<T> findEntitiesRough(
        Class<T> entityClass,
        World world,
        Vec3d center,
        int radiusChunks,
        Predicate<T> predicate
    ) {
        // the minimun is 1
        if (radiusChunks == 0) {
            radiusChunks = 1;
        }
        
        ChunkPos chunkPos = new ChunkPos(new BlockPos(center));
        return findEntities(
            entityClass,
            getChunkAccessor(world),
            chunkPos.x - radiusChunks,
            chunkPos.x + radiusChunks,
            0, 15,
            chunkPos.z - radiusChunks,
            chunkPos.z + radiusChunks,
            predicate
        );
    }
    
    //does not load chunk on server and works with large entities
    public static <T extends Entity> List<T> findEntitiesByBox(
        Class<T> entityClass,
        World world,
        Box box,
        double maxEntityRadius,
        Predicate<T> predicate
    ) {
        int xMin = (int) Math.floor(box.minX - maxEntityRadius);
        int yMin = (int) Math.floor(box.minY - maxEntityRadius);
        int zMin = (int) Math.floor(box.minZ - maxEntityRadius);
        int xMax = (int) Math.ceil(box.maxX + maxEntityRadius);
        int yMax = (int) Math.ceil(box.maxY + maxEntityRadius);
        int zMax = (int) Math.ceil(box.maxZ + maxEntityRadius);
        
        return findEntities(
            entityClass,
            getChunkAccessor(world),
            xMin >> 4,
            xMax >> 4,
            Math.max(0, yMin >> 4),
            Math.min(15, yMax >> 4),
            zMin >> 4,
            zMax >> 4,
            e -> e.getBoundingBox().intersects(box) && predicate.test(e)
        );
    }
    
    public static Identifier dimensionTypeId(RegistryKey<World> dimType) {
        return dimType.getValue();
    }
    
    public static <T> String serializeToJson(T object, Codec<T> codec) {
        DataResult<JsonElement> r = codec.encode(object, JsonOps.INSTANCE, new JsonObject());
        Either<JsonElement, DataResult.PartialResult<JsonElement>> either = r.get();
        JsonElement result = either.left().orElse(null);
        if (result != null) {
            return Global.gson.toJson(result);
        }
        
        return either.right().map(DataResult.PartialResult::toString).orElse("");
    }
    
    public static Vec3d getCurrentCameraPos() {
        return MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
    }
    
    public static class MyDecodeException extends RuntimeException {
        
        public MyDecodeException(String message) {
            super(message);
        }
    }
    
    public static <T, Serialized> T decodeFailHard(
        Codec<T> codec,
        DynamicOps<Serialized> ops,
        Serialized target
    ) {
        return codec.decode(ops, target)
            .getOrThrow(false, s -> {
                throw new MyDecodeException("Cannot decode" + s + target);
            }).getFirst();
    }
    
    public static <Serialized> Serialized getElementFailHard(
        DynamicOps<Serialized> ops,
        Serialized target,
        String key
    ) {
        return ops.get(target, key).getOrThrow(false, s -> {
            throw new MyDecodeException("Cannot find" + key + s + target);
        });
    }
    
    public static <T, Serialized> void encode(
        Codec<T> codec,
        DynamicOps<Serialized> ops,
        Serialized target,
        T object
    ) {
        codec.encode(object, ops, target);
    }
    
    public static <Serialized, T> T decodeElementFailHard(
        DynamicOps<Serialized> ops, Serialized input,
        Codec<T> codec, String key
    ) {
        return decodeFailHard(
            codec, ops,
            getElementFailHard(ops, input, key)
        );
    }
    
    public static void sendMessageToFirstLoggedPlayer(Text text) {
        Helper.log(text.asString());
        ModMain.serverTaskList.addTask(() -> {
            MinecraftServer server = getServer();
            if (server == null) {
                return false;
            }
            
            List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();
            if (playerList.isEmpty()) {
                return false;
            }
            
            for (ServerPlayerEntity player : playerList) {
                player.sendMessage(text, false);
            }
            
            return true;
        });
    }
    
    public static Iterable<Entity> getWorldEntityList(World world) {
        if (world.isClient()) {
            return CHelper.getWorldEntityList(world);
        }
        else {
            if (world instanceof ServerWorld) {
                return ((ServerWorld) world).iterateEntities();
            }
            else {
                return ((Iterable<Entity>) Collections.emptyList().iterator());
            }
        }
    }
    
    
    /**
     * It will spawn even if the chunk is not loaded
     * ServerWorld#addEntity(Entity)
     */
    public static void spawnServerEntityToUnloadedArea(Entity entity) {
        Validate.isTrue(!entity.world.isClient());

//        entity.teleporting = true;
        
        entity.world.spawnEntity(entity);

//        entity.teleporting = false;
    }
    
    public static void executeOnServerThread(Runnable runnable) {
        MinecraftServer server = McHelper.getServer();
        
        if (server.isOnThread()) {
            runnable.run();
        }
        else {
            server.execute(runnable);
        }
    }
}
