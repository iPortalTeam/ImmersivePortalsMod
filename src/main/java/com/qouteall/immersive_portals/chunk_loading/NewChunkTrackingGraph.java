package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.lang.ref.WeakReference;
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
    
    private static final ArrayList<WeakReference<ChunkVisibilityManager.ChunkLoader>>
        additionalChunkLoaders = new ArrayList<>();
    
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
    
    private static void updateAndPurge() {
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
                (long longChunkPos) -> {
                    MyLoadingTicket.load(world, new ChunkPos(longChunkPos));
                }
            );
            
            LongSortedSet additionalLoadedChunks = new LongLinkedOpenHashSet();
            additionalChunkLoaders.forEach(weakRef -> {
                ChunkVisibilityManager.ChunkLoader loader = weakRef.get();
                if (loader == null) return;
                loader.foreachChunkPos(
                    (dim, x, z, dis) -> {
                        if (world.dimension.getType() == dim) {
                            additionalLoadedChunks.add(ChunkPos.toLong(x, z));
                            MyLoadingTicket.load(world, new ChunkPos(x, z));
                        }
                    }
                );
            });
            additionalChunkLoaders.removeIf(ref -> ref.get() == null);
            
            LongList chunksToUnload = new LongArrayList();
            MyLoadingTicket.getRecord(world).forEach((long longChunkPos) -> {
                if (!currentLoadedChunks.contains(longChunkPos) &&
                    !additionalLoadedChunks.contains(longChunkPos)
                ) {
                    chunksToUnload.add(longChunkPos);
                }
            });
            
            chunksToUnload.forEach((long longChunkPos) -> {
                MyLoadingTicket.unload(world, new ChunkPos(longChunkPos));
            });
        });
    }
    
    private static long getUnloadTimeValve() {
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
            updateAndPurge();
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
        additionalChunkLoaders.clear();
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
    
    /**
     * {@link net.minecraft.server.world.ThreadedAnvilChunkStorage#getPlayersWatchingChunk(ChunkPos, boolean)}
     * The "onlyOnWatchDistanceEdge" is so weird!!!!!!
     * If it does not send only to edge players, placing a block will
     * send light updates and cause client to rebuild the chunk multiple times
     */
    public static Stream<ServerPlayerEntity> getFarWatchers(
        DimensionType dimension,
        int x, int z
    ) {
        return getPlayersViewingChunk(dimension, x, z)
            .filter(player -> player.dimension != dimension ||
                Helper.getChebyshevDistance(x, z, player.chunkX, player.chunkZ) > 4);
    }
    
    public static void forceRemovePlayer(ServerPlayerEntity player) {
        data.forEach((dim, map) -> map.forEach(
            (chunkPos, chunkRecord) -> chunkRecord.removeInactiveWatcher(
                (player1, l, d) -> player1 == player,
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
    
    public static boolean shouldLoadDimension(DimensionType dimension) {
        if (!data.containsKey(dimension)) {
            return false;
        }
        Long2ObjectLinkedOpenHashMap<ChunkRecord> map =
            data.get(dimension);
        return !map.isEmpty();
    }
    
    public static void addAdditionalChunkLoader(ChunkVisibilityManager.ChunkLoader chunkLoader) {
        additionalChunkLoaders.add(new WeakReference<>(chunkLoader));
        updateAndPurge();
    }
    
    // if this method is accidentally not called
    // the chunk loader will still be removed if it's not referenced (maybe after a long time)
    public static void removeAdditionalChunkLoader(ChunkVisibilityManager.ChunkLoader chunkLoader) {
        // WeakReference does not have equals()
        additionalChunkLoaders.removeIf(weakRef -> weakRef.get() == chunkLoader);
    }
}
