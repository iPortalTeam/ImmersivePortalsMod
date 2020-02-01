package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEServerWorld;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class NewChunkTrackingGraph {
    public static interface ShouldRemoveWatchPredicate {
        boolean test(ServerPlayerEntity player, long lastWatchTime, int distanceToSource);
    }
    
    public static class ChunkRecord {
        public ArrayList<ServerPlayerEntity> watchingPlayers = new ArrayList<>();
        public LongList lastWatchTimeList = new LongArrayList();
        public IntList distanceToSourceList = new IntArrayList();
        
        public void updateWatchingStatus(
            ServerPlayerEntity player,
            long currGameTime,
            int distanceToSource,
            Runnable addWatchInformer
        ) {
            int index = watchingPlayers.indexOf(player);
            if (index == -1) {
                watchingPlayers.add(player);
                lastWatchTimeList.add(currGameTime);
                distanceToSourceList.add(distanceToSource);
                
                addWatchInformer.run();
                return;
            }
            
            long lastWatchTime = lastWatchTimeList.getLong(index);
            if (lastWatchTime == currGameTime) {
                //being updated again in the same turn
                int oldDistance = distanceToSourceList.getInt(index);
                int newDistance = Math.min(oldDistance, distanceToSource);
                distanceToSourceList.set(index, newDistance);
            }
            else {
                //being updated at the first time in this turn
                distanceToSourceList.set(index, distanceToSource);
                lastWatchTimeList.set(index, currGameTime);
            }
        }
        
        public void removeInactiveWatcher(
            ShouldRemoveWatchPredicate predicate,
            Consumer<ServerPlayerEntity> informer
        ) {
            assert watchingPlayers.size() == lastWatchTimeList.size();
            assert lastWatchTimeList.size() == distanceToSourceList.size();
            
            //this is not the most efficent
            int size = watchingPlayers.size();
            int placingIndex = 0;
            for (int i = size - 1; i >= 0; i--) {
                boolean shouldRemove = predicate.test(
                    watchingPlayers.get(i),
                    lastWatchTimeList.getLong(i),
                    distanceToSourceList.getInt(i)
                );
                if (shouldRemove) {
                    ServerPlayerEntity removed = watchingPlayers.remove(i);
                    lastWatchTimeList.removeLong(i);
                    distanceToSourceList.removeInt(i);
                    informer.accept(removed);
                }
            }
        }
    
        public boolean isBeingWatchedByAnyPlayer() {
            return !watchingPlayers.isEmpty();
        }
    }
    
    private static final Map<DimensionType, Long2ObjectLinkedOpenHashMap<ChunkRecord>> data = new HashMap<>();
    
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public static final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    private static Long2ObjectLinkedOpenHashMap<ChunkRecord> getChunkRecordMap(DimensionType dimension) {
        return data.computeIfAbsent(dimension, k -> new Long2ObjectLinkedOpenHashMap<>());
    }
    
    public static void updateForPlayer(ServerPlayerEntity player) {
        long gameTime = McHelper.getOverWorldOnServer().getTime();
        ChunkVisibilityManager.getChunkLoaders(player)
            .forEach(chunkLoader -> chunkLoader.foreachChunkPos(
                (dimension, x, z, distanceToSource) -> {
                    getChunkRecordMap(dimension).computeIfAbsent(
                        ChunkPos.toLong(x, z),
                        k -> new ChunkRecord()
                    ).updateWatchingStatus(
                        player,
                        gameTime,
                        distanceToSource,
                        () -> beginWatchChunkSignal.emit(
                            player,
                            new DimensionalChunkPos(
                                dimension,
                                x, z
                            )
                        )
                    );
                }
            ));
    }
    
    private static void purge() {
        long unloadTimeValve = getUnloadTimeValve();
        long currTime = McHelper.getOverWorldOnServer().getTime();
        data.forEach((dimension, chunkRecords) -> {
            chunkRecords.long2ObjectEntrySet().removeIf(entry -> {
                long chunkPosLong = entry.getLongKey();
                ChunkRecord chunkRecord = entry.getValue();
                chunkRecord.removeInactiveWatcher(
                    (player, lastWatchTime, distanceToSource) -> {
                        return currTime - lastWatchTime > unloadTimeValve || player.removed;
                    },
                    player -> {
                        if (player.removed) return;
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
                return !chunkRecord.isBeingWatchedByAnyPlayer();
            });
        });
        
        McHelper.getServer().getWorlds().forEach(world -> {
    
            LongSortedSet currentLoadedChunks = getChunkRecordMap(world.dimension.getType()).keySet();
            
            currentLoadedChunks.forEach(
                (long longChunkPos) -> ((IEServerWorld) world).setChunkForcedWithoutImmediateLoading(
                    ChunkPos.getPackedX(longChunkPos),
                    ChunkPos.getPackedZ(longChunkPos),
                    true
                )
            );
            
            LongList chunksToUnload = new LongArrayList();
            //I can't use filter here because it will box Long
            world.getForcedChunks().forEach((long longChunkPos) -> {
                if (!currentLoadedChunks.contains(longChunkPos)) {
                    chunksToUnload.add(longChunkPos);
                }
            });
            
            chunksToUnload.forEach((long longChunkPos) -> world.setChunkForced(
                ChunkPos.getPackedX(longChunkPos),
                ChunkPos.getPackedZ(longChunkPos),
                false
            ));
        });
    }
    
    private static long getUnloadTimeValve() {
        if (ServerPerformanceAdjust.getIsServerLagging()) {
            return 41;
        }
        return 15 * 20;
    }
    
    private static void tick() {
        long gameTime = McHelper.getOverWorldOnServer().getTime();
        McHelper.getCopiedPlayerList().forEach(player -> {
            if (player.getEntityId() % 40 == gameTime % 40) {
                updateForPlayer(player);
            }
        });
        if (gameTime % 40 == 0) {
            purge();
        }
    }
    
    private static void setIsLoadedByPortal(
        DimensionType dimension,
        ChunkPos chunkPos,
        boolean isLoadedNow
    ) {
        ServerWorld world = McHelper.getServer().getWorld(dimension);
    
        world.setChunkForced(chunkPos.x, chunkPos.z, isLoadedNow);
    }
    
    public static void init() {
        ModMain.postServerTickSignal.connect(NewChunkTrackingGraph::tick);
    }
    
    public static boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        DimensionType dimension,
        int x, int z
    ) {
        ChunkRecord record = getChunkRecordMap(dimension)
            .get(ChunkPos.toLong(x, z));
        if (record == null) {
            return false;
        }
        return record.watchingPlayers.indexOf(player) != -1;
    }
    
    public static boolean isPlayerWatchingChunkWithinRaidus(
        ServerPlayerEntity player,
        DimensionType dimension,
        int x, int z,
        int radiusBlocks
    ) {
        ChunkRecord record = getChunkRecordMap(dimension)
            .get(ChunkPos.toLong(x, z));
        if (record == null) {
            return false;
        }
        int index = record.watchingPlayers.indexOf(player);
        if (index == -1) {
            return false;
        }
        int distanceToSource = record.distanceToSourceList.getInt(index);
        return distanceToSource * 16 <= radiusBlocks;
    }
    
    public static void cleanup() {
        data.clear();
    }
    
    public static Stream<ServerPlayerEntity> getPlayersViewingChunk(
        DimensionType dimension,
        int x, int z
    ) {
        ChunkRecord record = getChunkRecordMap(dimension)
            .get(ChunkPos.toLong(x, z));
        if (record == null) {
            return Stream.empty();
        }
        return record.watchingPlayers.stream();
    }
    
    public static void forceRemovePlayer(ServerPlayerEntity player) {
        data.values().forEach(map -> map.values().forEach(
            chunkRecord -> chunkRecord.removeInactiveWatcher(
                (player1, l, d) -> player1 == player,
                p -> {
                }
            )
        ));
    }
}
