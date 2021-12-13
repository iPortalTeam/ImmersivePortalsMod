package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.SignalBiArged;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NewChunkTrackingGraph {
    
    public static final int updateInterval = 40;
    
    public static class PlayerWatchRecord {
        public final ServerPlayerEntity player;
        public final RegistryKey<World> dimension;
        public final long chunkPos;
        public long lastWatchTime;
        public int distanceToSource;
        public boolean isDirectLoading;
        public boolean isLoadedToPlayer;
        public boolean isValid = true;
        
        public PlayerWatchRecord(
            ServerPlayerEntity player, RegistryKey<World> dimension,
            long chunkPos, long lastWatchTime,
            int distanceToSource, boolean isDirectLoading, boolean isLoadedToPlayer
        ) {
            this.player = player;
            this.dimension = dimension;
            this.chunkPos = chunkPos;
            this.lastWatchTime = lastWatchTime;
            this.distanceToSource = distanceToSource;
            this.isDirectLoading = isDirectLoading;
            this.isLoadedToPlayer = isLoadedToPlayer;
        }
        
        @Override
        public String toString() {
            return String.format(
                "%s (%d,%d) distance:%d valid:%s loaded:%s",
                dimension.getValue(),
                ChunkPos.getPackedX(chunkPos),
                ChunkPos.getPackedZ(chunkPos),
                distanceToSource,
                isValid,
                isLoadedToPlayer
            );
        }
    }
    
    private static void removeInactiveWatchers(
        ArrayList<PlayerWatchRecord> records,
        Predicate<PlayerWatchRecord> predicate,
        Consumer<PlayerWatchRecord> informer
    ) {
        records.removeIf(r -> {
            Validate.isTrue(r.isValid);
            
            boolean shouldRemove = predicate.test(r);
            if (shouldRemove) {
                informer.accept(r);
                r.isValid = false;
            }
            return shouldRemove;
        });
    }
    
    private static boolean shouldAddCustomTicket(
        ServerWorld world,
        long chunkPos,
        ArrayList<PlayerWatchRecord> records
    ) {
        boolean isIndirectLoading = Helper.indexOf(records, r ->
            (r.isLoadedToPlayer) && (!r.isDirectLoading)
        ) != -1;
        
        return isIndirectLoading;
    }
    
    // Every chunk has a list of watching records
    private static final Map<RegistryKey<World>, Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>>>
        data = new HashMap<>();
    
    private static final ArrayList<WeakReference<ChunkLoader>>
        additionalChunkLoaders = new ArrayList<>();
    
    public static class PlayerInfo {
        public final Set<RegistryKey<World>> visibleDimensions = new HashSet<>();
        public final ArrayList<ChunkLoader> additionalChunkLoaders
            = new ArrayList<>();
        public final ArrayList<ArrayDeque<PlayerWatchRecord>> distanceToPendingChunks =
            new ArrayList<>();
        
        public PerformanceLevel performanceLevel = PerformanceLevel.bad;
        
        public PlayerInfo() {
        }
        
        // one chunk may mark pending loading multiple times with different distanceToSource
        public void markPendingLoading(PlayerWatchRecord record) {
            Helper.arrayListComputeIfAbsent(
                distanceToPendingChunks,
                record.distanceToSource,
                ArrayDeque::new
            ).add(record);
        }
    }
    
    private static final WeakHashMap<ServerPlayerEntity, PlayerInfo> playerInfoMap = new WeakHashMap<>();
    
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    private static Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>> getChunkRecordMap(RegistryKey<World> dimension) {
        return data.computeIfAbsent(dimension, k -> new Long2ObjectLinkedOpenHashMap<>());
    }
    
    public static PlayerInfo getPlayerInfo(ServerPlayerEntity player) {
        return playerInfoMap.computeIfAbsent(player, k -> new PlayerInfo());
    }
    
    public static void updateForPlayer(ServerPlayerEntity player) {
        PlayerInfo playerInfo = getPlayerInfo(player);
        playerInfo.visibleDimensions.clear();
        
        long gameTime = McHelper.getOverWorldOnServer().getTime();
        ChunkVisibility.getBaseChunkLoaders(player)
            .forEach(chunkLoader -> updatePlayerForChunkLoader(player, gameTime, chunkLoader, playerInfo));
        
        playerInfo.additionalChunkLoaders.forEach(l -> {
            ChunkLoader chunkLoader = l;
            Validate.notNull(chunkLoader);
            updatePlayerForChunkLoader(player, gameTime, chunkLoader, playerInfo);
        });
    }
    
    public static void flushPendingLoading(ServerPlayerEntity player) {
        PlayerInfo playerInfo = getPlayerInfo(player);
        
        final int limit = getChunkDeliveringLimitPerTick(player);
        int loaded = 0;
        int directLoaded = 0;
        
        for (int distance = 0; distance < playerInfo.distanceToPendingChunks.size(); distance++) {
            ArrayDeque<PlayerWatchRecord> records = playerInfo.distanceToPendingChunks.get(distance);
            if (records != null) {
                while (!records.isEmpty() && loaded < limit && directLoaded < 5) {
                    PlayerWatchRecord record = records.pollFirst();
                    if (record.isValid && !record.isLoadedToPlayer) {
                        record.isLoadedToPlayer = true;
                        beginWatchChunkSignal.emit(player, new DimensionalChunkPos(
                            record.dimension, new ChunkPos(record.chunkPos)
                        ));
                        if (!record.isDirectLoading) {
                            MyLoadingTicket.addTicketIfNotLoaded(
                                McHelper.getServerWorld(record.dimension),
                                new ChunkPos(record.chunkPos)
                            );
                        }
                        
                        if (!record.isDirectLoading) {
                            loaded++;
                        }
                        else {
                            directLoaded++;
                        }
                    }
                }
            }
        }
    }
    
    private static final Random random = new Random();
    
    private static int getChunkDeliveringLimitPerTick(ServerPlayerEntity player) {
        if (player.age < 100) {
            return 200;
        }
        
        PlayerInfo playerInfo = getPlayerInfo(player);
        
        if (playerInfo.performanceLevel == PerformanceLevel.good) {
            return 5;
        }
        else if (playerInfo.performanceLevel == PerformanceLevel.medium) {
            return 1;
        }
        else {
            return player.age % 4 == 0 ? 1 : 0;
        }
    }
    
    private static void updatePlayerForChunkLoader(
        ServerPlayerEntity player, long gameTime, ChunkLoader chunkLoader,
        PlayerInfo playerInfo
    ) {
        RegistryKey<World> chunkLoaderDim = chunkLoader.center.dimension;
        playerInfo.visibleDimensions.add(chunkLoaderDim);
        
        Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>> chunkRecordMap =
            getChunkRecordMap(chunkLoaderDim);
        
        chunkLoader.foreachChunkPos(
            (dimension, x, z, distanceToSource) -> {
                long chunkPos = ChunkPos.toLong(x, z);
                ArrayList<PlayerWatchRecord> records = chunkRecordMap.computeIfAbsent(
                    chunkPos,
                    k -> new ArrayList<>()
                );
                
                int index = Helper.indexOf(records, r -> r.player == player);
                if (index == -1) {
                    PlayerWatchRecord newRecord = new PlayerWatchRecord(
                        player, dimension, chunkPos, gameTime, distanceToSource, chunkLoader.isDirectLoader,
                        false
                    );
                    records.add(newRecord);
                    playerInfo.markPendingLoading(newRecord);
                }
                else {
                    PlayerWatchRecord record = records.get(index);
                    
                    if (record.lastWatchTime == gameTime) {
                        //being updated again in the same turn
                        int oldDistance = record.distanceToSource;
                        if (distanceToSource < oldDistance) {
                            record.distanceToSource = distanceToSource;
                            playerInfo.markPendingLoading(record);
                        }
                        
                        record.isDirectLoading = (record.isDirectLoading | chunkLoader.isDirectLoader);
                    }
                    else {
                        //being updated at the first time in this turn
                        int oldDistance = record.distanceToSource;
                        if (distanceToSource < oldDistance) {
                            playerInfo.markPendingLoading(record);
                        }
                        
                        record.distanceToSource = distanceToSource;
                        record.lastWatchTime = gameTime;
                        record.isDirectLoading = chunkLoader.isDirectLoader;
                    }
                }
            }
        );
    }
    
    private static void updateAndPurge() {
        long currTime = McHelper.getOverWorldOnServer().getTime();
        data.forEach((dimension, chunkRecords) -> {
            chunkRecords.long2ObjectEntrySet().removeIf(entry -> {
                long chunkPosLong = entry.getLongKey();
                
                ArrayList<PlayerWatchRecord> records = entry.getValue();
                
                removeInactiveWatchers(
                    records,
                    (record) -> {
                        return shouldUnload(currTime, record);
                    },
                    (record) -> {
                        if (record.player.isRemoved()) return;
                        
                        if (record.isLoadedToPlayer) {
                            endWatchChunkSignal.emit(
                                record.player,
                                new DimensionalChunkPos(
                                    dimension,
                                    ChunkPos.getPackedX(chunkPosLong),
                                    ChunkPos.getPackedZ(chunkPosLong)
                                )
                            );
                        }
                    }
                );
                
                return records.isEmpty();
            });
        });
        
        MiscHelper.getServer().getWorlds().forEach(world -> {
            
            Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>> chunkRecordMap = getChunkRecordMap(world.getRegistryKey());
            
            LongSortedSet additionalLoadedChunks = new LongLinkedOpenHashSet();
            additionalChunkLoaders.forEach(weakRef -> {
                ChunkLoader loader = weakRef.get();
                if (loader == null) return;
                loader.foreachChunkPos(
                    (dim, x, z, dis) -> {
                        if (world.getRegistryKey() == dim) {
                            additionalLoadedChunks.add(ChunkPos.toLong(x, z));
                            MyLoadingTicket.addTicketIfNotLoaded(world, new ChunkPos(x, z));
                        }
                    }
                );
            });
            additionalChunkLoaders.removeIf(ref -> ref.get() == null);
            
            LongList chunksToUnload = new LongArrayList();
            MyLoadingTicket.getRecord(world).forEach((long longChunkPos) -> {
                if (!chunkRecordMap.containsKey(longChunkPos) &&
                    !additionalLoadedChunks.contains(longChunkPos)
                ) {
                    chunksToUnload.add(longChunkPos);
                }
            });
            
            chunksToUnload.forEach((long longChunkPos) -> {
                MyLoadingTicket.removeTicketIfPresent(world, new ChunkPos(longChunkPos));
            });
        });
        
        playerInfoMap.entrySet().removeIf(e -> e.getKey().isRemoved());
    }
    
    private static boolean shouldUnload(long currTime, PlayerWatchRecord record) {
        if (record.player.isRemoved()) {
            return true;
        }
        long unloadDelay = IPGlobal.chunkUnloadDelayTicks;
        
        if (unloadDelay < updateInterval + 1) {
            unloadDelay = updateInterval + 1;
        }
        
        if (GcMonitor.isMemoryNotEnough()) {
            // does not delay unloading
            unloadDelay = updateInterval + 1;
        }
        
        return currTime - record.lastWatchTime > unloadDelay;
    }
    
    private static void tick() {
        MiscHelper.getServer().getProfiler().push("portal_chunk_tracking");
        
        long gameTime = McHelper.getOverWorldOnServer().getTime();
        McHelper.getCopiedPlayerList().forEach(player -> {
            if (player.getId() % updateInterval == gameTime % updateInterval) {
                updateForPlayer(player);
            }
            flushPendingLoading(player);
        });
        if (gameTime % updateInterval == 0) {
            updateAndPurge();
        }
        
        MiscHelper.getServer().getProfiler().pop();
    }
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(NewChunkTrackingGraph::tick);
        IPGlobal.serverCleanupSignal.connect(NewChunkTrackingGraph::cleanup);
    }
    
    public static boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        int x, int z,
        Predicate<PlayerWatchRecord> predicate
    ) {
        long chunkPos = ChunkPos.toLong(x, z);
        
        ArrayList<PlayerWatchRecord> recordMap = getChunkRecordMap(dimension).get(chunkPos);
        if (recordMap == null) {
            return false;
        }
        int i = Helper.indexOf(recordMap, r -> r.player == player);
        if (i == -1) {
            return false;
        }
        
        PlayerWatchRecord record = recordMap.get(i);
        
        if (!record.isLoadedToPlayer) {
            return false;
        }
        
        return predicate.test(record);
    }
    
    public static boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        int x, int z
    ) {
        return isPlayerWatchingChunk(player, dimension, x, z, r -> true);
    }
    
    public static boolean isPlayerWatchingChunkWithinRaidus(
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        int x, int z,
        int radiusBlocks
    ) {
        boolean result = isPlayerWatchingChunk(
            player, dimension, x, z,
            r -> r.distanceToSource * 16 <= radiusBlocks
        );
        return result;
    }
    
    private static void cleanup() {
        data.clear();
        additionalChunkLoaders.clear();
    }
    
    public static Stream<ServerPlayerEntity> getPlayersViewingChunk(
        RegistryKey<World> dimension,
        int x, int z
    ) {
        ArrayList<PlayerWatchRecord> records = getChunkRecordMap(dimension)
            .get(ChunkPos.toLong(x, z));
        if (records == null) {
            return Stream.empty();
        }
        return records.stream().filter(r -> r.isLoadedToPlayer).map(r -> r.player);
    }
    
    /**
     * {@link net.minecraft.server.world.ThreadedAnvilChunkStorage#getPlayersWatchingChunk(ChunkPos, boolean)}
     * The "onlyOnWatchDistanceEdge" is so weird!!!!!!
     * If it does not send only to edge players, placing a block will
     * send light updates and cause client to rebuild the chunk multiple times
     */
    public static Stream<ServerPlayerEntity> getFarWatchers(
        RegistryKey<World> dimension,
        int x, int z
    ) {
        return getPlayersViewingChunk(dimension, x, z)
            .filter(player -> {
                ChunkPos chunkPos = player.getChunkPos();
                return player.world.getRegistryKey() != dimension ||
                    Helper.getChebyshevDistance(x, z, chunkPos.x, chunkPos.z) > 4;
            });
    }
    
    public static void forceRemovePlayer(ServerPlayerEntity player) {
        Helper.log("Chunk Tracking Graph Force Remove " + player.getName().asString());
        data.forEach((dim, map) -> map.forEach(
            (chunkPos, records) -> removeInactiveWatchers(
                records,
                (r) -> r.player == player,
                record -> {
                    record.player.networkHandler.sendPacket(
                        IPNetworking.createRedirectedMessage(
                            dim, new UnloadChunkS2CPacket(
                                ChunkPos.getPackedX(chunkPos),
                                ChunkPos.getPackedZ(chunkPos)
                            )
                        )
                    );
                }
            )
        ));
    }
    
    public static boolean shouldLoadDimension(RegistryKey<World> dimension) {
        if (!data.containsKey(dimension)) {
            return false;
        }
        Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>> map =
            data.get(dimension);
        return !map.isEmpty();
    }
    
    public static void addGlobalAdditionalChunkLoader(ChunkLoader chunkLoader) {
        additionalChunkLoaders.add(new WeakReference<>(chunkLoader));
        updateAndPurge();
    }
    
    // if this method is accidentally not called
    // the chunk loader will still be removed if it's not GCed (maybe after a long time)
    public static void removeGlobalAdditionalChunkLoader(ChunkLoader chunkLoader) {
        // WeakReference does not have equals()
        additionalChunkLoaders.removeIf(weakRef -> weakRef.get() == chunkLoader);
    }
    
    // When changing a player's dimension on server, it will remove all
    // loading tickets of this player. Without this, the chunks nearby player
    // may have no ticket for a short period of time (because the chunk tracking refreshes
    // every 2 seconds) and the chunk may be unloaded and reloaded.
    public static void addAdditionalDirectLoadingTickets(ServerPlayerEntity player) {
        ChunkVisibility.playerDirectLoader(player).foreachChunkPos((dim, x, z, dis) -> {
            if (isPlayerWatchingChunk(player, dim, x, z)) {
                
                MyLoadingTicket.addTicketIfNotLoaded(((ServerWorld) player.world), new ChunkPos(x, z));
            }
        });
    }
    
    public static int getLoadedChunkNum(RegistryKey<World> dimension) {
        return getChunkRecordMap(dimension).size();
    }
    
    public static void addPerPlayerAdditionalChunkLoader(
        ServerPlayerEntity player,
        ChunkLoader chunkLoader
    ) {
        getPlayerInfo(player).additionalChunkLoaders.add(chunkLoader);
    }
    
    public static void removePerPlayerAdditionalChunkLoader(
        ServerPlayerEntity player,
        ChunkLoader chunkLoader
    ) {
        getPlayerInfo(player).additionalChunkLoaders.remove(chunkLoader);
    }
    
    public static Set<RegistryKey<World>> getVisibleDimensions(ServerPlayerEntity player) {
        return getPlayerInfo(player).visibleDimensions;
    }
    
    public static class RemoteCallables {
        public static void acceptClientPerformanceInfo(
            ServerPlayerEntity player,
            PerformanceLevel performanceLevel
        ) {
            PlayerInfo playerInfo = getPlayerInfo(player);
            playerInfo.performanceLevel = performanceLevel;
        }
    }
}
