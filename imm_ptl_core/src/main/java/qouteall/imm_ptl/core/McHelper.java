package qouteall.imm_ptl.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.ducks.IESectionedEntityCache;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mc_utils.MyNbtTextFormatter;
import qouteall.imm_ptl.core.mixin.common.mc_util.IESimpleEntityLookup;
import qouteall.imm_ptl.core.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.entity.SectionedEntityCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ducks.IEEntityTrackingSection;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

// mc related helper methods
public class McHelper {
    
    public static IEThreadedAnvilChunkStorage getIEStorage(RegistryKey<World> dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkManager) MiscHelper.getServer().getWorld(dimension).getChunkManager()
        ).threadedAnvilChunkStorage;
    }
    
    public static ArrayList<ServerPlayerEntity> getCopiedPlayerList() {
        return new ArrayList<>(MiscHelper.getServer().getPlayerManager().getPlayerList());
    }
    
    public static List<ServerPlayerEntity> getRawPlayerList() {
        return MiscHelper.getServer().getPlayerManager().getPlayerList();
    }
    
    public static Vec3d lastTickPosOf(Entity entity) {
        return new Vec3d(entity.prevX, entity.prevY, entity.prevZ);
    }
    
    public static ServerWorld getOverWorldOnServer() {
        return MiscHelper.getServer().getWorld(World.OVERWORLD);
    }
    
    public static void serverLog(
        ServerPlayerEntity player,
        String text
    ) {
        Helper.log(text);
        player.sendMessage(new LiteralText(text), false);
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getTime();
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
            Helper.err("Error Occured");
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
        IPGlobal.serverTaskList.addTask(() -> {
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
                Helper.err("The future is cancelled");
                finalizer.run();
                return true;
            }
            if (future.isCompletedExceptionally()) {
                Helper.err("The future is completed exceptionally");
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
    
    public static <ENTITY extends Entity> List<ENTITY> getEntitiesNearby(
        World world,
        Vec3d center,
        Class<ENTITY> entityClass,
        double range
    ) {
        return findEntitiesRough(
            entityClass,
            world,
            center,
            (int) (range / 16 + 1),
            e -> true
        );
    }
    
    public static <ENTITY extends Entity> List<ENTITY> getEntitiesNearby(
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
        Vec3d eyeOffset = GravityChangerInterface.invoker.getEyeOffset(entity);
        return entity.getPos().add(eyeOffset);
    }
    
    public static Vec3d getLastTickEyePos(Entity entity) {
        Vec3d eyeOffset = GravityChangerInterface.invoker.getEyeOffset(entity);
        return lastTickPosOf(entity).add(eyeOffset);
    }
    
    public static void setEyePos(Entity entity, Vec3d eyePos, Vec3d lastTickEyePos) {
        Vec3d eyeOffset = GravityChangerInterface.invoker.getEyeOffset(entity);
        
        setPosAndLastTickPos(
            entity,
            eyePos.subtract(eyeOffset),
            lastTickEyePos.subtract(eyeOffset)
        );

//        float eyeHeight = entity.getStandingEyeHeight();
//        setPosAndLastTickPos(
//            entity,
//            eyePos.add(0, -eyeHeight, 0),
//            lastTickEyePos.add(0, -eyeHeight, 0)
//        );
    }
    
    public static double getVehicleY(Entity vehicle, Entity passenger) {
        return passenger.getY() - vehicle.getMountedHeightOffset() - passenger.getHeightOffset();
    }
    
    public static void adjustVehicle(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return;
        }
        
        Vec3d currVelocity = vehicle.getVelocity();
        
        double newX = entity.getX();
        double newY = getVehicleY(vehicle, entity);
        double newZ = entity.getZ();
        vehicle.setPosition(newX, newY, newZ);
        Vec3d newPos = new Vec3d(newX, newY, newZ);
        McHelper.setPosAndLastTickPos(
            vehicle, newPos, newPos
        );
        
        // MinecartEntity or LivingEntity use position interpolation
        // disable the interpolation or it may interpolate into unloaded chunks
        vehicle.updateTrackedPositionAndAngles(
            newX, newY, newZ, vehicle.getYaw(), vehicle.getPitch(),
            0, false
        );
        
        vehicle.setVelocity(currVelocity);
        
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
        player.setPosition(player.getX(), player.getY(), player.getZ());
    }
    
    public static void updatePosition(Entity entity, Vec3d pos) {
        entity.setPosition(pos.x, pos.y, pos.z);
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
    
    
    public static Portal copyEntity(Portal portal) {
        Portal newPortal = ((Portal) portal.getType().create(portal.world));
        
        Validate.notNull(newPortal);
        
        newPortal.readNbt(portal.writeNbt(new NbtCompound()));
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
        Validate.isTrue(Thread.currentThread() == MiscHelper.getServer().getThread(), "must be on server thread");
    }
    
    public static void invokeCommandAs(Entity commandSender, List<String> commandList) {
        ServerCommandSource commandSource = commandSender.getCommandSource().withLevel(2).withSilent();
        CommandManager commandManager = MiscHelper.getServer().getCommandManager();
        
        for (String command : commandList) {
            commandManager.execute(commandSource, command);
        }
    }
    
    public static void resendSpawnPacketToTrackers(Entity entity) {
        getIEStorage(entity.world.getRegistryKey()).resendSpawnPacketToTrackers(entity);
    }
    
    public static void sendToTrackers(Entity entity, Packet<?> packet) {
        ThreadedAnvilChunkStorage.EntityTracker entityTracker =
            getIEStorage(entity.world.getRegistryKey()).getEntityTrackerMap().get(entity.getId());
        if (entityTracker == null) {
            Helper.err("missing entity tracker object");
            return;
        }
        
        entityTracker.sendToNearbyPlayers(packet);
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
        EntityLookup<Entity> entityLookup,
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
            entityClass, entityLookup,
            chunkXStart, chunkXEnd, chunkYStart, chunkYEnd, chunkZStart, chunkZEnd,
            entity -> {
                if (predicate.test(entity)) {
                    result.add(entity);
                }
            }
        );
        return result;
    }
    
    /**
     * the range is inclusive on both ends
     * similar to {@link SectionedEntityCache#forEachInBox(Box, Consumer)}
     * but without hardcoding the max entity radius
     */
    public static <T extends Entity> void foreachEntities(
        Class<T> entityClass, EntityLookup<Entity> entityLookup,
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Consumer<T> consumer
    ) {
        Validate.isTrue(chunkXEnd >= chunkXStart);
        Validate.isTrue(chunkYEnd >= chunkYStart);
        Validate.isTrue(chunkZEnd >= chunkZStart);
        Validate.isTrue(chunkXEnd - chunkXStart < 1000, "too big");
        Validate.isTrue(chunkZEnd - chunkZStart < 1000, "too big");
        
        TypeFilter<T, T> typeFilter = TypeFilter.instanceOf(entityClass);
        
        SectionedEntityCache<Entity> cache =
            (SectionedEntityCache<Entity>) ((IESimpleEntityLookup) entityLookup).getCache();
        
        ((IESectionedEntityCache) cache).forEachSectionInBox(
            chunkXStart, chunkXEnd,
            chunkYStart, chunkYEnd,
            chunkZStart, chunkZEnd,
            entityTrackingSection -> {
                ((IEEntityTrackingSection) entityTrackingSection).myForeach(
                    typeFilter, consumer
                );
            }
        );
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
            ((IEWorld) world).portal_getEntityLookup(),
            chunkPos.x - radiusChunks,
            chunkPos.x + radiusChunks,
            McHelper.getMinSectionY(world), McHelper.getMaxSectionYExclusive(world) - 1,
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
        
        int minChunkY = McHelper.getMinSectionY(world);
        int maxChunkYExclusive = McHelper.getMaxSectionYExclusive(world);
        
        foreachEntities(
            entityClass, ((IEWorld) world).portal_getEntityLookup(),
            xMin >> 4, xMax >> 4,
            MathHelper.clamp(yMin >> 4, minChunkY, maxChunkYExclusive - 1),
            MathHelper.clamp(yMax >> 4, minChunkY, maxChunkYExclusive - 1),
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
            return IPGlobal.gson.toJson(result);
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
        IPGlobal.serverTaskList.addTask(() -> {
            MinecraftServer server = MiscHelper.getServer();
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
        
        boolean spawned = entity.world.spawnEntity(entity);
        
        if (!spawned) {
            Helper.err("Failed to spawn " + entity + entity.world);
        }
    }
    
    public static ServerWorld getServerWorld(RegistryKey<World> dim) {
        ServerWorld world = MiscHelper.getServer().getWorld(dim);
        if (world == null) {
            throw new RuntimeException("Missing dimension " + dim.getValue());
        }
        return world;
    }
    
    public static Text compoundTagToTextSorted(NbtCompound tag, String indent, int depth) {
        return new MyNbtTextFormatter(" ", 0).apply(tag);
    }
    
    public static int getMinY(WorldAccess world) {
        return world.getBottomY();
    }
    
    public static int getMaxYExclusive(WorldAccess world) {
        return world.getTopY();
    }
    
    public static int getMaxContentYExclusive(WorldAccess world) {
        return world.getDimension().getLogicalHeight() + getMinY(world);
    }
    
    public static int getMinSectionY(WorldAccess world) {
        return world.getBottomSectionCoord();
    }
    
    public static int getMaxSectionYExclusive(WorldAccess world) {
        return world.getTopSectionCoord();
    }
    
    public static int getYSectionNumber(WorldAccess world) {
        return getMaxSectionYExclusive(world) - getMinSectionY(world);
    }
    
    public static Box getBoundingBoxWithMovedPosition(
        Entity entity, Vec3d newPos
    ) {
        return entity.getBoundingBox().offset(
            newPos.subtract(entity.getPos())
        );
    }
    
    public static String readTextResource(Identifier identifier) {
        String result = null;
        try {
            InputStream inputStream =
                MinecraftClient.getInstance().getResourceManager().getResource(
                    identifier
                ).getInputStream();
            
            result = IOUtils.toString(inputStream, Charset.defaultCharset());
        }
        catch (IOException e) {
            throw new RuntimeException("Error loading " + identifier, e);
        }
        return result;
    }
    
    public static Vec3d getWorldVelocity(Entity entity) {
        return GravityChangerInterface.invoker.getWorldVelocity(entity);
    }
    
    public static void setWorldVelocity(Entity entity, Vec3d newVelocity) {
        GravityChangerInterface.invoker.setWorldVelocity(entity, newVelocity);
    }
    
    public static Vec3d getEyeOffset(Entity entity) {
        return GravityChangerInterface.invoker.getEyeOffset(entity);
    }
    
}
