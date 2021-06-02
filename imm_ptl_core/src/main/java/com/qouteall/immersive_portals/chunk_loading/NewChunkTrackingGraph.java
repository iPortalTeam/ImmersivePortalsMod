package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.miscellaneous.GcMonitor;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NewChunkTrackingGraph {
    
    public static final int updateInterval = 40;
    public static boolean addCustomTicketForDirectLoadingDelayed = true;
    
    public static class PlayerWatchRecord {
        public ServerPlayerEntity player;
        public long lastWatchTime;
        public int distanceToSource;
        public boolean isDirectLoading;
        
        public PlayerWatchRecord(ServerPlayerEntity player, long lastWatchTime, int distanceToSource, boolean isDirectLoading) {
            this.player = player;
            this.lastWatchTime = lastWatchTime;
            this.distanceToSource = distanceToSource;
            this.isDirectLoading = isDirectLoading;
        }
    }
    
    private static void updateWatchingStatus(
        ArrayList<PlayerWatchRecord> records,
        ServerPlayerEntity player,
        long currGameTime,
        int distanceToSource,
        boolean isDirectLoading,
        Runnable addWatchInformer
    ) {
        int i = Helper.indexOf(records, r -> r.player == player);
        if (i == -1) {
            records.add(new PlayerWatchRecord(
                player, currGameTime, distanceToSource, isDirectLoading
            ));
            
            addWatchInformer.run();
        }
        else {
            PlayerWatchRecord record = records.get(i);
            
            if (record.lastWatchTime == currGameTime) {
                //being updated again in the same turn
                int oldDistance = record.distanceToSource;
                int newDistance = Math.min(oldDistance, distanceToSource);
                record.distanceToSource = newDistance;
                record.isDirectLoading = isDirectLoading;
            }
            else {
                //being updated at the first time in this turn
                record.distanceToSource = distanceToSource;
                record.lastWatchTime = currGameTime;
                record.isDirectLoading = (record.isDirectLoading | isDirectLoading);
            }
        }
    }
    
    private static void removeInactiveWatchers(
        ArrayList<PlayerWatchRecord> records,
        Predicate<PlayerWatchRecord> predicate,
        Consumer<ServerPlayerEntity> informer
    ) {
        records.removeIf(r -> {
            boolean shouldRemove = predicate.test(r);
            if (shouldRemove) {
                informer.accept(r.player);
            }
            return shouldRemove;
        });
    }
    
    private static boolean isBeingWatchedByAnyPlayer(ArrayList<PlayerWatchRecord> records) {
        return !records.isEmpty();
    }
    
    private static boolean shouldAddCustomTicket(
        ServerWorld world,
        long chunkPos,
        ArrayList<PlayerWatchRecord> records
    ) {
        boolean isIndirectLoading = Helper.indexOf(records, r -> !r.isDirectLoading) != -1;
        
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
        
        public PlayerInfo() {
        }
    }
    
    private static final WeakHashMap<ServerPlayerEntity, PlayerInfo> playerInfoMap = new WeakHashMap<>();
    
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    private static Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>> getChunkRecordMap(RegistryKey<World> dimension) {
        return data.computeIfAbsent(dimension, k -> new Long2ObjectLinkedOpenHashMap<>());
    }
    
    private static PlayerInfo getPlayerInfo(ServerPlayerEntity player) {
        return playerInfoMap.computeIfAbsent(player, k -> new PlayerInfo());
    }
    
    public static void updateForPlayer(ServerPlayerEntity player) {
        ((IEEntity) player).portal_requestUpdateChunkPos();
//        ((ServerWorld) player.world).checkEntityChunkPos(player);
        
        PlayerInfo playerInfo = getPlayerInfo(player);
        playerInfo.visibleDimensions.clear();
        
        long gameTime = McHelper.getOverWorldOnServer().getTime();
        ChunkVisibility.getBaseChunkLoaders(player)
            .forEach(chunkLoader -> updatePlayerForChunkLoader(player, gameTime, chunkLoader));
        
        playerInfo.additionalChunkLoaders.forEach(l -> {
            ChunkLoader chunkLoader = l;
            assert chunkLoader != null;
            updatePlayerForChunkLoader(player, gameTime, chunkLoader);
        });
    }
    
    private static void updatePlayerForChunkLoader(
        ServerPlayerEntity player, long gameTime, ChunkLoader chunkLoader
    ) {
        getPlayerInfo(player).visibleDimensions.add(chunkLoader.center.dimension);
        
        chunkLoader.foreachChunkPos(
            (dimension, x, z, distanceToSource) -> {
                ArrayList<PlayerWatchRecord> records = getChunkRecordMap(dimension).computeIfAbsent(
                    ChunkPos.toLong(x, z),
                    k -> new ArrayList<>()
                );
                updateWatchingStatus(
                    records,
                    player,
                    gameTime,
                    distanceToSource,
                    chunkLoader.isDirectLoader,
                    () -> beginWatchChunkSignal.emit(
                        player,
                        new DimensionalChunkPos(
                            dimension,
                            x, z
                        )
                    )
                );
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
                    player -> {
                        if (player.isRemoved()) return;
                        endWatchChunkSignal.emit(
                            player,
                            new DimensionalChunkPos(
                                dimension,
                                ChunkPos.getPackedX(chunkPosLong),
                                ChunkPos.getPackedZ(chunkPosLong)
                            )
                        );
                    }
                );
                
                return !isBeingWatchedByAnyPlayer(records);
            });
        });
        
        McHelper.getServer().getWorlds().forEach(world -> {
            
            Long2ObjectLinkedOpenHashMap<ArrayList<PlayerWatchRecord>> chunkRecordMap = getChunkRecordMap(world.getRegistryKey());
            
            chunkRecordMap.long2ObjectEntrySet().forEach(entry -> {
                long longChunkPos = entry.getLongKey();
                ArrayList<PlayerWatchRecord> records = entry.getValue();
                
                if (shouldAddCustomTicket(world, longChunkPos, records)) {
                    MyLoadingTicket.addTicketIfNotLoaded(world, new ChunkPos(longChunkPos));
                }
            });
            
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
                MyLoadingTicket.removeTicket(world, new ChunkPos(longChunkPos));
            });
        });
        
        playerInfoMap.entrySet().removeIf(e -> e.getKey().isRemoved());
    }
    
    private static boolean shouldUnload(long currTime, PlayerWatchRecord record) {
        if (record.player.isRemoved()) {
            return true;
        }
        long unloadDelay = Global.chunkUnloadDelayTicks;
        
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
        McHelper.getServer().getProfiler().push("portal_chunk_tracking");
        
        long gameTime = McHelper.getOverWorldOnServer().getTime();
        McHelper.getCopiedPlayerList().forEach(player -> {
            if (player.getId() % updateInterval == gameTime % updateInterval) {
                updateForPlayer(player);
            }
        });
        if (gameTime % updateInterval == 0) {
            updateAndPurge();
        }
        
        McHelper.getServer().getProfiler().pop();
    }
    
    private static void setIsLoadedByPortal(
        RegistryKey<World> dimension,
        ChunkPos chunkPos,
        boolean isLoadedNow
    ) {
        ServerWorld world = McHelper.getServer().getWorld(dimension);
        
        world.setChunkForced(chunkPos.x, chunkPos.z, isLoadedNow);
    }
    
    public static void init() {
        ModMain.postServerTickSignal.connect(NewChunkTrackingGraph::tick);
        ModMain.serverCleanupSignal.connect(NewChunkTrackingGraph::cleanup);
    }
    
    public static boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        int x, int z,
        Predicate<PlayerWatchRecord> predicate
    ) {
        ArrayList<PlayerWatchRecord> record = getChunkRecordMap(dimension)
            .get(ChunkPos.toLong(x, z));
        if (record == null) {
            return false;
        }
        int i = Helper.indexOf(record, r -> r.player == player);
        if (i == -1) {
            return false;
        }
        
        return predicate.test(record.get(i));
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
        return isPlayerWatchingChunk(
            player, dimension, x, z,
            r -> r.distanceToSource * 16 <= radiusBlocks
        );
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
        return records.stream().map(r -> r.player);
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
                p -> {
                    //it solves issue but making respawn laggier
                    p.networkHandler.sendPacket(
                        MyNetwork.createRedirectedMessage(
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
}
