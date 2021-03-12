package com.qouteall.immersive_portals;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
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
import com.qouteall.immersive_portals.render.CrossPortalEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

// mc related helper methods
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
        Helper.log(text);
        player.sendMessage(new LiteralText(text), false);
    }
    
    public static Box getChunkBoundingBox(ChunkPos chunkPos) {
        return new Box(
            chunkPos.getStartPos(),
            chunkPos.getStartPos().add(16, 256, 16)
        );
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getTime();
    }
    
    @Deprecated
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
    
    @Deprecated
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
            Util.getMainWorkerExecutor()
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
        return (Stream) world.getEntitiesByClass(entityClass, box, e -> true).stream();
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
    
    @Nonnull
    public static List<Portal> getGlobalPortals(World world) {
        List<Portal> result;
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
    
    // includes global portals
    public static Stream<Portal> getNearbyPortals(Entity center, double range) {
        return getNearbyPortals(center.world, center.getPos(), range);
    }
    
    // includes global portals
    public static Stream<Portal> getNearbyPortals(World world, Vec3d pos, double range) {
        List<Portal> globalPortals = getGlobalPortals(world);
        
        Stream<Portal> nearbyPortals = McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
            world, pos, Portal.class, range
        );
        return Streams.concat(
            globalPortals.stream().filter(
                p -> p.getDistanceToNearestPointInPortal(pos) < range * 2
            ),
            nearbyPortals
        );
    }
    
    public static int getRenderDistanceOnServer() {
        return getIEStorage(World.OVERWORLD).getWatchDistance();
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3d pos,
        Vec3d lastTickPos
    ) {
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
    }
    
    public static void updateBoundingBox(Entity player) {
        player.updatePosition(player.getX(), player.getY(), player.getZ());
    }
    
    public static void updatePosition(Entity entity, Vec3d pos) {
        entity.updatePosition(pos.x, pos.y, pos.z);
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
    
    public static Portal copyEntity(Portal portal) {
        Portal newPortal = ((Portal) portal.getType().create(portal.world));
        
        Validate.notNull(newPortal);
        
        newPortal.fromTag(portal.toTag(new CompoundTag()));
        return newPortal;
    }
    
    public static boolean getIsServerChunkGenerated(RegistryKey<World> toDimension, BlockPos toPos) {
        return getIEStorage(toDimension)
            .portal_isChunkGenerated(new ChunkPos(toPos));
    }
    
    // because withUnderline is client only
    @Environment(EnvType.CLIENT)
    public static MutableText getLinkText(String link) {
        return new LiteralText(link).styled(
            style -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.OPEN_URL, link
            )).withUnderline(true)
        );
    }
    
    public static void validateOnServerThread() {
        Validate.isTrue(Thread.currentThread() == getServer().getThread(), "must be on server thread");
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
        
        foreachEntities(
            entityClass, chunkAccessor,
            chunkXStart, chunkXEnd, chunkYStart, chunkYEnd, chunkZStart, chunkZEnd,
            entity -> {
                if (predicate.test(entity)) {
                    result.add(entity);
                }
            }
        );
        return result;
    }
    
    public static <T extends Entity> void foreachEntities(
        Class<T> entityClass, ChunkAccessor chunkAccessor,
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Consumer<T> consumer
    ) {
        Validate.isTrue(chunkXEnd >= chunkXStart);
        Validate.isTrue(chunkYEnd >= chunkYStart);
        Validate.isTrue(chunkZEnd >= chunkZStart);
        Validate.isTrue(chunkYStart >= 0);
        Validate.isTrue(chunkYEnd < 16);
        Validate.isTrue(chunkXEnd - chunkXStart < 1000, "too big");
        Validate.isTrue(chunkZEnd - chunkZStart < 1000, "too big");
        
        for (int x = chunkXStart; x <= chunkXEnd; x++) {
            for (int z = chunkZStart; z <= chunkZEnd; z++) {
                WorldChunk chunk = chunkAccessor.getChunk(x, z);
                if (chunk != null && !(chunk instanceof EmptyChunk)) {
                    TypeFilterableList<Entity>[] entitySections =
                        ((IEWorldChunk) chunk).portal_getEntitySections();
                    for (int i = chunkYStart; i <= chunkYEnd; i++) {
                        TypeFilterableList<Entity> entitySection = entitySections[i];
                        for (T entity : entitySection.getAllOfType(entityClass)) {
                            consumer.accept(entity);
                        }
                    }
                }
            }
        }
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
        ArrayList<T> result = new ArrayList<>();
        
        foreachEntitiesByBox(entityClass, world, box, maxEntityRadius, predicate, result::add);
        return result;
    }
    
    public static <T extends Entity> void foreachEntitiesByBox(
        Class<T> entityClass, World world, Box box,
        double maxEntityRadius, Predicate<T> predicate, Consumer<T> consumer
    ) {
        
        foreachEntitiesByBoxApproximateRegions(entityClass, world, box, maxEntityRadius, entity -> {
            if (entity.getBoundingBox().intersects(box) && predicate.test(entity)) {
                consumer.accept(entity);
            }
        });
    }
    
    public static <T extends Entity> void foreachEntitiesByBoxApproximateRegions(
        Class<T> entityClass, World world, Box box, double maxEntityRadius, Consumer<T> consumer
    ) {
        int xMin = (int) Math.floor(box.minX - maxEntityRadius);
        int yMin = (int) Math.floor(box.minY - maxEntityRadius);
        int zMin = (int) Math.floor(box.minZ - maxEntityRadius);
        int xMax = (int) Math.ceil(box.maxX + maxEntityRadius);
        int yMax = (int) Math.ceil(box.maxY + maxEntityRadius);
        int zMax = (int) Math.ceil(box.maxZ + maxEntityRadius);
        
        foreachEntities(
            entityClass, getChunkAccessor(world),
            xMin >> 4, xMax >> 4,
            MathHelper.clamp(yMin >> 4, 0, 15),
            MathHelper.clamp(yMax >> 4, 0, 15),
            zMin >> 4, zMax >> 4,
            consumer
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
     *
     * @link ServerWorld#addEntity(Entity)
     */
    public static void spawnServerEntity(Entity entity) {
        Validate.isTrue(!entity.world.isClient());
        
        entity.teleporting = true;
        
        boolean spawned = entity.world.spawnEntity(entity);
        
        if (!spawned) {
            Helper.err("Failed to spawn " + entity + entity.world);
        }
        
        entity.teleporting = false;
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
    
    public static <T> SimpleRegistry<T> filterAndCopyRegistry(
        SimpleRegistry<T> registry, BiPredicate<RegistryKey<T>, T> predicate
    ) {
        SimpleRegistry<T> newRegistry = new SimpleRegistry<>(
            registry.getKey(),
            registry.getLifecycle()
        );
        
        for (Map.Entry<RegistryKey<T>, T> entry : registry.getEntries()) {
            T object = entry.getValue();
            RegistryKey<T> key = entry.getKey();
            if (predicate.test(key, object)) {
                newRegistry.add(
                    key, object, registry.getEntryLifecycle(object)
                );
            }
        }
        
        return newRegistry;
    }
    
    public static ServerWorld getServerWorld(RegistryKey<World> dim) {
        ServerWorld world = McHelper.getServer().getWorld(dim);
        if (world == null) {
            throw new RuntimeException("Missing dimension " + dim.getValue());
        }
        return world;
    }
    
    private static Text prettyPrintTagKey(String key) {
        return (new LiteralText(key)).formatted(Formatting.AQUA);
    }
    
    public static Text tagToTextSorted(Tag tag, String indent, int depth) {
        if (tag instanceof CompoundTag) {
            return compoundTagToTextSorted(((CompoundTag) tag), indent, depth);
        }
        if (tag instanceof ListTag) {
            if (!((ListTag) tag).isEmpty()) {
                Tag firstElement = ((ListTag) tag).get(0);
                if (firstElement instanceof IntTag || firstElement instanceof DoubleTag) {
                    return tag.toText("", depth);
                }
            }
        }
        if (tag instanceof ByteTag) {
            byte value = ((ByteTag) tag).getByte();
            if (value == 1) {
                return new LiteralText("true").formatted(Formatting.GOLD);
            }
            else if (value == 0) {
                return new LiteralText("false").formatted(Formatting.GOLD);
            }
        }
        return tag.toText(indent, depth);
    }
    
    /**
     * {@link CompoundTag#toText(String, int)}
     */
    public static Text compoundTagToTextSorted(CompoundTag tag, String indent, int depth) {
        if (tag.isEmpty()) {
            return new LiteralText("{}");
        }
        else {
            MutableText mutableText = new LiteralText("{");
            Collection<String> collection = tag.getKeys();
            
            List<String> list = Lists.newArrayList(collection);
            Collections.sort(list);
            collection = list;
            
            
            if (!indent.isEmpty()) {
                mutableText.append("\n");
            }
            
            MutableText mutableText2;
            for (Iterator iterator = ((Collection) collection).iterator(); iterator.hasNext(); mutableText.append((Text) mutableText2)) {
                String keyName = (String) iterator.next();
                mutableText2 = (new LiteralText(Strings.repeat(indent, depth + 1)))
                    .append(prettyPrintTagKey(keyName))
                    .append(String.valueOf(':'))
                    .append(" ")
                    .append(
                        tagToTextSorted(tag.get(keyName), indent, depth)
                    );
                if (iterator.hasNext()) {
                    mutableText2.append(String.valueOf(',')).append(indent.isEmpty() ? " " : "\n");
                }
            }
            
            if (!indent.isEmpty()) {
                mutableText.append("\n").append(Strings.repeat(indent, depth));
            }
            
            mutableText.append("}");
            return mutableText;
        }
    }
    
    public static int getMinY(WorldAccess world) {
        return 0;
    }
    
    public static int getMaxYExclusive(WorldAccess world) {
        return 256;
    }
    
    public static int getMaxContentYExclusive(WorldAccess world) {
        return world.getDimensionHeight();
    }
    
    public static int getMinChunkY(WorldAccess world) {
        return 0;
    }
    
    public static int getMaxChunkYExclusive(WorldAccess world) {
        return 16;
    }
    
}
