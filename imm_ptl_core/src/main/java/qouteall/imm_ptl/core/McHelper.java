package qouteall.imm_ptl.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEEntityTrackingSection;
import qouteall.imm_ptl.core.ducks.IESectionedEntityCache;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mc_utils.MyNbtTextFormatter;
import qouteall.imm_ptl.core.mixin.common.mc_util.IELevelEntityGetterAdapter;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

// mc related helper methods
public class McHelper {
    
    public static class Placeholder {}
    
    public static final Placeholder placeholder = new Placeholder();
    
    public static IEThreadedAnvilChunkStorage getIEStorage(ResourceKey<Level> dimension) {
        return (IEThreadedAnvilChunkStorage) (
            (ServerChunkCache) getServerWorld(dimension).getChunkSource()
        ).chunkMap;
    }
    
    public static ArrayList<ServerPlayer> getCopiedPlayerList() {
        return new ArrayList<>(MiscHelper.getServer().getPlayerList().getPlayers());
    }
    
    public static List<ServerPlayer> getRawPlayerList() {
        return MiscHelper.getServer().getPlayerList().getPlayers();
    }
    
    public static Vec3 lastTickPosOf(Entity entity) {
        return new Vec3(entity.xo, entity.yo, entity.zo);
    }
    
    public static ServerLevel getOverWorldOnServer() {
        return MiscHelper.getServer().getLevel(Level.OVERWORLD);
    }
    
    public static void serverLog(
        ServerPlayer player,
        String text
    ) {
        Helper.log(text);
        player.displayClientMessage(Component.literal(text), false);
    }
    
    public static long getServerGameTime() {
        return getOverWorldOnServer().getGameTime();
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
            Util.backgroundExecutor()
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
        Level world,
        Vec3 center,
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
            center.level,
            center.position(),
            entityClass,
            range
        );
    }
    
    public static int getRenderDistanceOnServer() {
        return getIEStorage(Level.OVERWORLD).ip_getWatchDistance();
    }
    
    public static void setPosAndLastTickPos(
        Entity entity,
        Vec3 pos,
        Vec3 lastTickPos
    ) {
        entity.setPosRaw(pos.x, pos.y, pos.z);
        entity.xOld = lastTickPos.x;
        entity.yOld = lastTickPos.y;
        entity.zOld = lastTickPos.z;
        entity.xo = lastTickPos.x;
        entity.yo = lastTickPos.y;
        entity.zo = lastTickPos.z;
    }
    
    public static void setPosAndLastTickPosWithoutTriggeringCallback(
        Entity entity,
        Vec3 pos,
        Vec3 lastTickPos
    ) {
        ((IEEntity) entity).ip_setPositionWithoutTriggeringCallback(pos);
        entity.xOld = lastTickPos.x;
        entity.yOld = lastTickPos.y;
        entity.zOld = lastTickPos.z;
        entity.xo = lastTickPos.x;
        entity.yo = lastTickPos.y;
        entity.zo = lastTickPos.z;
    }
    
    public static Vec3 getEyePos(Entity entity) {
        Vec3 eyeOffset = GravityChangerInterface.invoker.getEyeOffset(entity);
        return entity.position().add(eyeOffset);
    }
    
    public static Vec3 getLastTickEyePos(Entity entity) {
        Vec3 eyeOffset = GravityChangerInterface.invoker.getEyeOffset(entity);
        return lastTickPosOf(entity).add(eyeOffset);
    }
    
    public static void setEyePos(Entity entity, Vec3 eyePos, Vec3 lastTickEyePos) {
        Vec3 eyeOffset = GravityChangerInterface.invoker.getEyeOffset(entity);
        
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
        return passenger.getY() - vehicle.getPassengersRidingOffset() - passenger.getMyRidingOffset();
    }
    
    public static void adjustVehicle(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return;
        }
        
        Vec3 currVelocity = vehicle.getDeltaMovement();
        
        double newX = entity.getX();
        double newY = getVehicleY(vehicle, entity);
        double newZ = entity.getZ();
        vehicle.setPos(newX, newY, newZ);
        Vec3 newPos = new Vec3(newX, newY, newZ);
        McHelper.setPosAndLastTickPos(
            vehicle, newPos, newPos
        );
        
        // MinecartEntity or LivingEntity use position interpolation
        // disable the interpolation or it may interpolate into unloaded chunks
        vehicle.lerpTo(
            newX, newY, newZ, vehicle.getYRot(), vehicle.getXRot(),
            0, false
        );
        
        vehicle.setDeltaMovement(currVelocity);
        
    }
    
    public static LevelChunk getServerChunkIfPresent(
        ResourceKey<Level> dimension,
        int x, int z
    ) {
        ChunkHolder chunkHolder_ = getIEStorage(dimension).ip_getChunkHolder(ChunkPos.asLong(x, z));
        if (chunkHolder_ == null) {
            return null;
        }
        return chunkHolder_.getTickingChunk();
    }
    
    public static LevelChunk getServerChunkIfPresent(
        ServerLevel world, int x, int z
    ) {
        ChunkHolder chunkHolder_ = ((IEThreadedAnvilChunkStorage) (
            (ServerChunkCache) world.getChunkSource()
        ).chunkMap).ip_getChunkHolder(ChunkPos.asLong(x, z));
        if (chunkHolder_ == null) {
            return null;
        }
        return chunkHolder_.getTickingChunk();
    }
    
    @Deprecated
    public static <ENTITY extends Entity> Stream<ENTITY> getServerEntitiesNearbyWithoutLoadingChunk(
        Level world,
        Vec3 center,
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
        player.setPos(player.getX(), player.getY(), player.getZ());
    }
    
    public static void updatePosition(Entity entity, Vec3 pos) {
        entity.setPos(pos.x, pos.y, pos.z);
    }
    
    public static <T extends Entity> List<T> getEntitiesRegardingLargeEntities(
        Level world,
        AABB box,
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
        Portal newPortal = ((Portal) portal.getType().create(portal.level));
        
        Validate.notNull(newPortal);
        
        newPortal.load(portal.saveWithoutId(new CompoundTag()));
        return newPortal;
    }
    
    /**
     * {@link net.minecraft.world.level.chunk.storage.RegionFileStorage}
     * MC does not provide a clean interface to tell whether a chunk exists.
     * Only check whether the region file exists now.
     */
    public static boolean getDoesRegionFileExist(ResourceKey<Level> toDimension, BlockPos toPos) {
        ChunkPos chunkPos = new ChunkPos(toPos);
        
        LevelStorageSource.LevelStorageAccess storageSource = MiscHelper.getServer().storageSource;
        
        Path regionFilePath = storageSource.getDimensionPath(toDimension).resolve("region")
            .resolve("r." + chunkPos.getRegionX() + "." + chunkPos.getRegionZ() + ".mca");
        
        return regionFilePath.toFile().exists();
    }
    
    // because withUnderline is client only
    @Environment(EnvType.CLIENT)
    public static MutableComponent getLinkText(String link) {
        return Component.literal(link).withStyle(
            style -> style.withClickEvent(new ClickEvent(
                ClickEvent.Action.OPEN_URL, link
            )).withUnderlined(true)
        );
    }
    
    public static void validateOnServerThread() {
        Validate.isTrue(Thread.currentThread() == MiscHelper.getServer().getRunningThread(), "must be on server thread");
    }
    
    public static void invokeCommandAs(Entity commandSender, List<String> commandList) {
        CommandSourceStack commandSource = commandSender.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        Commands commandManager = MiscHelper.getServer().getCommands();
        
        for (String command : commandList) {
            commandManager.performPrefixedCommand(commandSource, command);
        }
    }
    
    public static void resendSpawnPacketToTrackers(Entity entity) {
        getIEStorage(entity.level.dimension()).ip_resendSpawnPacketToTrackers(entity);
    }
    
    public static void sendToTrackers(Entity entity, Packet<?> packet) {
        ChunkMap.TrackedEntity entityTracker =
            getIEStorage(entity.level.dimension()).ip_getEntityTrackerMap().get(entity.getId());
        if (entityTracker == null) {
//            Helper.err("missing entity tracker object");
            return;
        }
        
        entityTracker.broadcastAndSend(packet);
    }
    
    //it's a little bit incorrect with corner glass pane
    @Nullable
    public static AABB getWallBox(Level world, IntBox glassArea) {
        Stream<BlockPos> blockPosStream = glassArea.stream();
        return getWallBox(world, blockPosStream);
    }
    
    @Nullable
    public static AABB getWallBox(Level world, Stream<BlockPos> blockPosStream) {
        return blockPosStream.map(blockPos -> {
            VoxelShape collisionShape = world.getBlockState(blockPos).getCollisionShape(world, blockPos);
            
            if (collisionShape.isEmpty()) {
                return null;
            }
            
            return collisionShape.bounds().move(Vec3.atLowerCornerOf(blockPos));
        }).filter(b -> b != null).reduce(AABB::minmax).orElse(null);
    }
    
    
    public static boolean isServerChunkFullyLoaded(ServerLevel world, ChunkPos chunkPos) {
        LevelChunk chunk = getServerChunkIfPresent(
            world.dimension(), chunkPos.x, chunkPos.z
        );
        
        if (chunk == null) {
            return false;
        }
        
        boolean entitiesLoaded = world.areEntitiesLoaded(chunkPos.toLong());
        
        return entitiesLoaded;
    }
    
    public static interface ChunkAccessor {
        LevelChunk getChunk(int x, int z);
    }
    
    public static ChunkAccessor getChunkAccessor(Level world) {
        if (world.isClientSide()) {
            return world::getChunk;
        }
        else {
            return (x, z) -> getServerChunkIfPresent(((ServerLevel) world), x, z);
        }
    }
    
    public static <T extends Entity> List<T> findEntities(
        Class<T> entityClass,
        LevelEntityGetter<Entity> entityLookup,
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
    
    @Nullable
    public static <T extends Entity, R> R traverseEntities(
        Class<T> entityClass, LevelEntityGetter<Entity> entityLookup,
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Function<T, R> function
    ) {
        Validate.isTrue(chunkXEnd >= chunkXStart);
        Validate.isTrue(chunkYEnd >= chunkYStart);
        Validate.isTrue(chunkZEnd >= chunkZStart);
        Validate.isTrue(chunkXEnd - chunkXStart < 1000, "range too big");
        Validate.isTrue(chunkZEnd - chunkZStart < 1000, "range too big");
        
        EntityTypeTest<Entity, T> typeFilter = EntityTypeTest.forClass(entityClass);
        
        EntitySectionStorage<Entity> cache =
            (EntitySectionStorage<Entity>) ((IELevelEntityGetterAdapter) entityLookup).getCache();
        
        return ((IESectionedEntityCache<Entity>) cache).ip_traverseSectionInBox(
            chunkXStart, chunkXEnd,
            chunkYStart, chunkYEnd,
            chunkZStart, chunkZEnd,
            entityTrackingSection -> {
                return ((IEEntityTrackingSection<Entity>) entityTrackingSection).ip_traverse(
                    typeFilter, function
                );
            }
        );
    }
    
    public static <E extends Entity, R> R traverseEntitiesByBox(
        Class<E> entityClass,
        Level world,
        AABB box,
        double maxEntityRadius,
        Function<E, R> function
    ) {
        return traverseEntitiesByApproximateRegion(
            entityClass, world, box,
            maxEntityRadius,
            entity -> {
                if (entity.getBoundingBox().intersects(box)) {
                    return function.apply(entity);
                }
                else {
                    return null;
                }
            }
        );
    }
    
    /**
     * the range is inclusive on both ends
     * similar to {@link EntitySectionStorage#forEachAccessibleNonEmptySection(AABB, AbortableIterationConsumer)}
     * but without hardcoding the max entity radius
     */
    public static <T extends Entity> void foreachEntities(
        Class<T> entityClass, LevelEntityGetter<Entity> entityLookup,
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Consumer<T> consumer
    ) {
        traverseEntities(
            entityClass, entityLookup,
            chunkXStart, chunkXEnd,
            chunkYStart, chunkYEnd,
            chunkZStart, chunkZEnd,
            e -> {
                consumer.accept(e);
                return null;
            }
        );
    }
    
    public static <T extends Entity> List<T> findEntitiesRough(
        Class<T> entityClass,
        Level world,
        Vec3 center,
        int radiusChunks,
        Predicate<T> predicate
    ) {
        // the minimum is 1
        if (radiusChunks <= 0) {
            radiusChunks = 1;
        }
        
        if (radiusChunks > 32) {
            radiusChunks = 32;
        }
        
        SectionPos sectionPos = SectionPos.of(center);
        
        return findEntities(
            entityClass,
            ((IEWorld) world).portal_getEntityLookup(),
            sectionPos.x() - radiusChunks,
            sectionPos.x() + radiusChunks,
            sectionPos.y() - radiusChunks,
            sectionPos.y() + radiusChunks,
            sectionPos.z() - radiusChunks,
            sectionPos.z() + radiusChunks,
            predicate
        );
    }
    
    //does not load chunk on server and works with large entities
    public static <T extends Entity> List<T> findEntitiesByBox(
        Class<T> entityClass,
        Level world,
        AABB box,
        double maxEntityRadius,
        Predicate<T> predicate
    ) {
        ArrayList<T> result = new ArrayList<>();
        
        foreachEntitiesByBox(entityClass, world, box, maxEntityRadius, predicate, result::add);
        return result;
    }
    
    public static <T extends Entity> void foreachEntitiesByBox(
        Class<T> entityClass, Level world, AABB box,
        double maxEntityRadius, Predicate<T> predicate, Consumer<T> consumer
    ) {
        
        foreachEntitiesByBoxApproximateRegions(entityClass, world, box, maxEntityRadius, entity -> {
            if (entity.getBoundingBox().intersects(box) && predicate.test(entity)) {
                consumer.accept(entity);
            }
        });
    }
    
    public static <T extends Entity> void foreachEntitiesByBoxApproximateRegions(
        Class<T> entityClass, Level world, AABB box, double maxEntityRadius, Consumer<T> consumer
    ) {
        int xMin = (int) Math.floor(box.minX - maxEntityRadius);
        int yMin = (int) Math.floor(box.minY - maxEntityRadius);
        int zMin = (int) Math.floor(box.minZ - maxEntityRadius);
        int xMax = (int) Math.ceil(box.maxX + maxEntityRadius);
        int yMax = (int) Math.ceil(box.maxY + maxEntityRadius);
        int zMax = (int) Math.ceil(box.maxZ + maxEntityRadius);
        
        
        foreachEntities(
            entityClass, ((IEWorld) world).portal_getEntityLookup(),
            xMin >> 4, xMax >> 4,
            yMin >> 4, yMax >> 4,
            zMin >> 4, zMax >> 4,
            consumer
        );
    }
    
    public static <E extends Entity, R> R traverseEntitiesByApproximateRegion(
        Class<E> entityClass, Level world, AABB box, double maxEntityRadius,
        Function<E, R> function
    ) {
        int xMin = (int) Math.floor(box.minX - maxEntityRadius);
        int yMin = (int) Math.floor(box.minY - maxEntityRadius);
        int zMin = (int) Math.floor(box.minZ - maxEntityRadius);
        int xMax = (int) Math.ceil(box.maxX + maxEntityRadius);
        int yMax = (int) Math.ceil(box.maxY + maxEntityRadius);
        int zMax = (int) Math.ceil(box.maxZ + maxEntityRadius);
        
        return traverseEntities(
            entityClass, ((IEWorld) world).portal_getEntityLookup(),
            xMin >> 4, xMax >> 4,
            yMin >> 4, yMax >> 4,
            zMin >> 4, zMax >> 4,
            function
        );
    }
    
    public static <T extends Entity> void foreachEntitiesByPointAndRoughRadius(
        Class<T> entityClass, Level world, Vec3 point, int roughRadius,
        Consumer<T> consumer
    ) {
        traverseEntitiesByPointAndRoughRadius(
            entityClass, world, point, roughRadius,
            entity -> {
                consumer.accept(entity);
                return null;
            }
        );
    }
    
    public static <T extends Entity, R> void traverseEntitiesByPointAndRoughRadius(
        Class<T> entityClass, Level world, Vec3 point, int roughRadius,
        Function<T, R> function
    ) {
        SectionPos sectionPos = SectionPos.of(BlockPos.containing(point));
        int roughRadiusChunks = roughRadius / 16;
        if (roughRadiusChunks == 0) {
            roughRadiusChunks = 1;
        }
        
        traverseEntities(
            entityClass, ((IEWorld) world).portal_getEntityLookup(),
            sectionPos.x() - roughRadiusChunks,
            sectionPos.x() + roughRadiusChunks,
            sectionPos.y() - roughRadiusChunks,
            sectionPos.y() + roughRadiusChunks,
            sectionPos.z() - roughRadiusChunks,
            sectionPos.z() + roughRadiusChunks,
            function
        );
    }
    
    
    public static ResourceLocation dimensionTypeId(ResourceKey<Level> dimType) {
        return dimType.location();
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
    
    public static void sendMessageToFirstLoggedPlayer(Component text) {
        Helper.log(text.getContents());
        IPGlobal.serverTaskList.addTask(() -> {
            MinecraftServer server = MiscHelper.getServer();
            if (server == null) {
                return false;
            }
            
            List<ServerPlayer> playerList = server.getPlayerList().getPlayers();
            if (playerList.isEmpty()) {
                return false;
            }
            
            for (ServerPlayer player : playerList) {
                player.displayClientMessage(text, false);
            }
            
            return true;
        });
    }
    
    public static Iterable<Entity> getWorldEntityList(Level world) {
        if (world.isClientSide()) {
            return CHelper.getWorldEntityList(world);
        }
        else {
            if (world instanceof ServerLevel) {
                return ((ServerLevel) world).getAllEntities();
            }
            else {
                return ((Iterable<Entity>) Collections.emptyList().iterator());
            }
        }
    }
    
    /**
     * It will spawn even if the chunk is not loaded
     */
    public static void spawnServerEntity(Entity entity) {
        Validate.isTrue(!entity.level.isClientSide());
        
        boolean spawned = entity.level.addFreshEntity(entity);
        
        if (!spawned) {
            Helper.err("Failed to spawn " + entity + entity.level);
        }
    }
    
    public static ServerLevel getServerWorld(ResourceKey<Level> dim) {
        ServerLevel world = MiscHelper.getServer().getLevel(dim);
        if (world == null) {
            throw new RuntimeException("Missing dimension " + dim.location());
        }
        return world;
    }
    
    public static Component compoundTagToTextSorted(CompoundTag tag, String indent, int depth) {
        return new MyNbtTextFormatter(" ", 0).apply(tag);
    }
    
    public static int getMinY(LevelAccessor world) {
        return world.getMinBuildHeight();
    }
    
    public static int getMaxYExclusive(LevelAccessor world) {
        return world.getMaxBuildHeight();
    }
    
    public static int getMaxContentYExclusive(LevelAccessor world) {
        return world.dimensionType().logicalHeight() + getMinY(world);
    }
    
    public static int getMinSectionY(LevelAccessor world) {
        return world.getMinSection();
    }
    
    public static int getMaxSectionYExclusive(LevelAccessor world) {
        return world.getMaxSection();
    }
    
    public static int getYSectionNumber(LevelAccessor world) {
        return getMaxSectionYExclusive(world) - getMinSectionY(world);
    }
    
    public static AABB getBoundingBoxWithMovedPosition(
        Entity entity, Vec3 newPos
    ) {
        return entity.getBoundingBox().move(
            newPos.subtract(entity.position())
        );
    }
    
    public static String readTextResource(ResourceLocation identifier) {
        String result = null;
        try {
            InputStream inputStream =
                Minecraft.getInstance().getResourceManager().getResource(
                    identifier
                ).get().open();
            
            result = IOUtils.toString(inputStream, Charset.defaultCharset());
        }
        catch (IOException e) {
            throw new RuntimeException("Error loading " + identifier, e);
        }
        return result;
    }
    
    public static Vec3 getWorldVelocity(Entity entity) {
        return GravityChangerInterface.invoker.getWorldVelocity(entity);
    }
    
    public static void setWorldVelocity(Entity entity, Vec3 newVelocity) {
        GravityChangerInterface.invoker.setWorldVelocity(entity, newVelocity);
    }
    
    public static Vec3 getEyeOffset(Entity entity) {
        return GravityChangerInterface.invoker.getEyeOffset(entity);
    }
    
    public static Vec3 getAxisWFromOrientation(DQuaternion quaternion) {
        return quaternion.rotate(new Vec3(1, 0, 0));
    }
    
    public static Vec3 getAxisHFromOrientation(DQuaternion quaternion) {
        return quaternion.rotate(new Vec3(0, 1, 0));
    }
    
    public static Vec3 getNormalFromOrientation(DQuaternion quaternion) {
        return quaternion.rotate(new Vec3(0, 0, 1));
    }
}
