package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.sun.istack.internal.Nullable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ChunkTrackingGraph {
    
    //if server is about to be out of memory then load fewer chunks
    public static boolean isMemoryTight = false;
    
    private static final int unloadIdleTickTime = 20 * 15;
    private static final int unloadIdleTickTimeSameDimension = 20 * 10;
    private static final int unloadIdleTickTimeWhenMemoryTight = 1;
    
    //only 1 fifth of the chunks will be unload at a time
    private static final int bufferedChunkUnloadingFactor = 5;
    
    public static class Edge {
        public DimensionalChunkPos chunkPos;
        public ServerPlayerEntity player;
        public long lastActiveGameTime;
        public int distanceToSource = 2333;//2333 means uninitialized
        public boolean isSent = false;
        
        public Edge(
            DimensionalChunkPos chunkPos,
            ServerPlayerEntity player,
            long lastActiveGameTime
        ) {
            this.chunkPos = chunkPos;
            this.player = player;
            this.lastActiveGameTime = lastActiveGameTime;
        }
        
        public void resetDistanceToSource() {
            distanceToSource = 2333;
        }
        
        public void updateDistanceToSource(int newValue) {
            distanceToSource = Math.min(newValue, distanceToSource);
        }
    }
    
    public static final int portalLoadingRange = 64;
    public static final int secondaryPortalLoadingRange = 16;
    
    public final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    private Multimap<DimensionalChunkPos, Edge> chunkPosToEdges = HashMultimap.create();
    private Map<ServerPlayerEntity, Map<DimensionalChunkPos, Edge>> playerToEdges = new HashMap<>();
    
    public ChunkTrackingGraph() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ChunkTrackingGraph::tick);
    }
    
    public void cleanUp() {
        chunkPosToEdges.clear();
        playerToEdges.clear();
    }
    
    public void setIsLoadedByPortal(
        DimensionType dimension,
        ChunkPos chunkPos,
        boolean isLoadedNow
    ) {
        ServerWorld world = McHelper.getServer().getWorld(dimension);
        
        world.setChunkForced(chunkPos.x, chunkPos.z, isLoadedNow);
        //world.method_14178().setChunkForced(chunkPos, isLoadedNow);
    }
    
    @Nullable
    private Edge getEdge(DimensionalChunkPos chunkPos, ServerPlayerEntity playerEntity) {
        return chunkPosToEdges.get(chunkPos)
            .stream()
            .filter(edge -> edge.player == playerEntity)
            .findAny()
            .orElse(null);
    }
    
    private Edge getOrAddEdge(DimensionalChunkPos chunkPos, ServerPlayerEntity playerEntity) {
        return chunkPosToEdges.get(chunkPos)
            .stream()
            .filter(edge -> edge.player == playerEntity)
            .findAny()
            .orElseGet(() -> addEdge(chunkPos, playerEntity));
    }
    
    private Edge addEdge(DimensionalChunkPos chunkPos, ServerPlayerEntity player) {
        Edge edge = new Edge(chunkPos, player, McHelper.getServerGameTime());
        chunkPosToEdges.put(chunkPos, edge);
        playerToEdges
            .computeIfAbsent(player, k -> new HashMap<>())
            .put(chunkPos, edge);
    
        ModMain.serverTaskList.addTask(() -> {
            beginWatchChunkSignal.emit(player, chunkPos);
            return true;
        });
    
        return edge;
    }
    
    private void removeEdge(Edge edge) {
        chunkPosToEdges.entries().removeIf(
            entry -> entry.getValue() == edge
        );
        Map<DimensionalChunkPos, Edge> corr = playerToEdges.get(edge.player);
        if (corr != null) {
            corr.remove(edge.chunkPos);
        }
        if (corr.isEmpty()) {
            playerToEdges.remove(edge.player);
        }
    
        ModMain.serverTaskList.addTask(() -> {
            if (!edge.player.removed) {
                endWatchChunkSignal.emit(edge.player, edge.chunkPos);
            }
            return true;
        });
    }
    
    private void updatePlayer(ServerPlayerEntity playerEntity) {
        playerToEdges.computeIfAbsent(playerEntity, k -> new HashMap<>())
            .values().forEach(Edge::resetDistanceToSource);
    
        ChunkVisibilityManager.getChunkLoaders(playerEntity)
            .forEach(chunkLoader -> chunkLoader.foreachChunkPos(
                (dimension, x, z, distance) -> {
                    Edge edge = getOrAddEdge(
                        new DimensionalChunkPos(dimension, x, z),
                        playerEntity
                    );
                    edge.updateDistanceToSource(distance);
                    edge.lastActiveGameTime = McHelper.getServerGameTime();
                }
            ));
    
        removeInactiveEdges(playerEntity);
    }
    
    private void tick() {
        McHelper.getServer().getProfiler().push("chunk_tracker");
    
        long currTime = McHelper.getServerGameTime();
        for (ServerPlayerEntity player : McHelper.getCopiedPlayerList()) {
            if (currTime % 50 == player.getEntityId() % 50) {
                updatePlayer(player);
    
                updateForcedChunks();
            }
        }
    
        if (currTime % 100 == 66) {
            cleanupForRemovedPlayers();
        }
    
        McHelper.getServer().getProfiler().pop();
    }
    
    private void removeInactiveEdges(ServerPlayerEntity playerEntity) {
        long serverGameTime = McHelper.getServerGameTime();
        ArrayDeque<Edge> edgesToRemove = playerToEdges.get(playerEntity).values().stream()
            .filter(
                edge -> ((serverGameTime - edge.lastActiveGameTime) > getUnloadTime(edge))
            )
            .collect(Collectors.toCollection(ArrayDeque::new));
    
        edgesToRemove.forEach(this::removeEdge);
    
    }
    
    private long getUnloadTime(Edge edge) {
        if (isMemoryTight) {
            return unloadIdleTickTimeWhenMemoryTight;
        }
        if (edge.player.dimension == edge.chunkPos.dimension) {
            return unloadIdleTickTimeSameDimension;
        }
        else {
            return unloadIdleTickTime;
        }
    }
    
    private void updateForcedChunks() {
        Map<DimensionType, List<DimensionalChunkPos>> newForcedChunkMap =
            chunkPosToEdges.keySet().stream().collect(
                Collectors.groupingBy(chunkPos -> chunkPos.dimension)
            );
        McHelper.getServer().getWorlds().forEach(world -> {
            List<DimensionalChunkPos> newForcedChunks =
                newForcedChunkMap.computeIfAbsent(
                    world.dimension.getType(),
                    k -> new ArrayList<>()
                );
            LongSet oldForcedChunks = new LongOpenHashSet(world.getForcedChunks());
            Helper.compareOldAndNew(
                oldForcedChunks,
                newForcedChunks.stream()
                    .map(chunkPos -> chunkPos.getChunkPos().toLong())
                    .collect(Collectors.toSet()),
                longChunkPos -> setIsLoadedByPortal(
                    world.dimension.getType(),
                    new ChunkPos(longChunkPos),
                    false
                ),
                longChunkPos -> setIsLoadedByPortal(
                    world.dimension.getType(),
                    new ChunkPos(longChunkPos),
                    true
                )
            );
        });
    }
    
    private void cleanupForRemovedPlayers() {
        chunkPosToEdges.values().stream()
            .filter(edge -> edge.player.removed)
            .collect(Collectors.toList())
            .forEach(this::removeEdge);
        playerToEdges.keySet().stream()
            .filter(player -> player.removed)
            .collect(Collectors.toList())
            .forEach(player -> playerToEdges.remove(player));
    }
    
    public Stream<ServerPlayerEntity> getPlayersViewingChunk(
        DimensionType dimensionType,
        ChunkPos chunkPos
    ) {
        assert dimensionType != null;
        return chunkPosToEdges
            .get(new DimensionalChunkPos(dimensionType, chunkPos))
            .stream()
            .map(edge -> edge.player);
    }
    
    public boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        return chunkPosToEdges.get(chunkPos).stream()
            .anyMatch(edge -> edge.player == player);
    }
    
    public boolean isPlayerWatchingChunkWithinDistance(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        int maxDistance
    ) {
        return chunkPosToEdges.get(chunkPos).stream()
            .anyMatch(edge -> edge.player == player && edge.distanceToSource <= maxDistance);
    }
    
    public void onChunkDataSent(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        Edge edge = getOrAddEdge(chunkPos, player);
        if (edge.isSent) {
            Helper.log(String.format(
                "chunk data sent twice! %s %s",
                player, chunkPos
            ));
        }
        edge.isSent = true;
    }
    
    public boolean isChunkDataSent(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        Edge edge = getEdge(chunkPos, player);
        return edge != null && edge.isSent;
    }
    
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        playerToEdges.remove(oldPlayer);
        chunkPosToEdges.entries().removeIf(entry ->
            entry.getValue().player == oldPlayer
        );
    }
    
    public static int getRenderDistanceOnServer() {
        return McHelper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
    
}
