package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.portal_entity.Portal;
import com.sun.istack.internal.Nullable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChunkTracker {
    
    /**
     * {@link net.minecraft.server.world.ChunkTicketManager.DistanceFromNearestPlayerTracker#getInitialLevel(long)}
     * {@link net.minecraft.server.world.ChunkTicketManager#handleChunkEnter(ChunkSectionPos, ServerPlayerEntity)}
     * {@link net.minecraft.server.world.ChunkTicketManager#handleChunkLeave(ChunkSectionPos, ServerPlayerEntity)}}
     * {@link net.minecraft.server.world.ThreadedAnvilChunkStorage#getPlayersWatchingChunk(ChunkPos, boolean)}
     * {@link ChunkHolder#sendPacketToPlayersWatching(Packet, boolean)}
     * {@link ThreadedAnvilChunkStorage#updateCameraPosition(ServerPlayerEntity)}
     */

    //it's a graph

    public static class Edge {
        public DimensionalChunkPos chunkPos;
        public ServerPlayerEntity player;
        public long lastActiveGameTime;
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
    }
    
    private static final ChunkTicketType<ChunkPos> immersiveTicketType =
        ChunkTicketType.create(
            "immersive_portal_ticket",
            Comparator.comparingLong(ChunkPos::toLong)
        );
    
    public static final int portalLoadingRange = 48;
    
    public final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    //TODO optimize using nested map
    private Multimap<DimensionalChunkPos, Edge> chunkPosToEdges = HashMultimap.create();
    private Multimap<ServerPlayerEntity, Edge> playerToEdges = HashMultimap.create();
    
    public ChunkTracker() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ChunkTracker::tick);
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
        ServerWorld world = Helper.getServer().getWorld(dimension);
        
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
        Edge edge = new Edge(chunkPos, player, Helper.getServerGameTime());
        chunkPosToEdges.put(chunkPos, edge);
        playerToEdges.put(player, edge);
    
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
        playerToEdges.entries().removeIf(
            entry -> entry.getValue() == edge
        );
    
        ModMain.serverTaskList.addTask(() -> {
            if (!edge.player.removed) {
                endWatchChunkSignal.emit(edge.player, edge.chunkPos);
            }
            return true;
        });
    }
    
    private void updatePlayer(ServerPlayerEntity playerEntity) {
        Set<DimensionalChunkPos> newPlayerViewingChunks = getPlayerViewingChunks(
            playerEntity
        );
        newPlayerViewingChunks.forEach(chunkPos -> {
            Edge edge = getOrAddEdge(chunkPos, playerEntity);
            edge.lastActiveGameTime = Helper.getServerGameTime();
        });
    
        removeInactiveEdges(playerEntity);
    }
    
    private Set<DimensionalChunkPos> getPlayerViewingChunks(
        ServerPlayerEntity player
    ) {
        int portalChunkLoadingRadius = getRenderDistanceOnServer() / 3;
        return Streams.concat(
            //directly watching chunks
            getNearbyChunkPoses(
                player.dimension,
                player.getBlockPos(),
                getRenderDistanceOnServer()
            ),
    
            //indirectly watching chunks
            Helper.getEntitiesNearby(
                player,
                Portal.class,
                portalLoadingRange
            ).flatMap(
                portalEntity -> getNearbyChunkPoses(
                    portalEntity.dimensionTo,
                    new BlockPos(portalEntity.destination),
                    portalChunkLoadingRadius
                )
            )
        ).collect(Collectors.toSet());
    }
    
    private void tick() {
        List<ServerPlayerEntity> playerList =
            new ArrayList<>(Helper.getServer().getPlayerManager().getPlayerList());
        long currTime = Helper.getServerGameTime();
        for (ServerPlayerEntity player : playerList) {
            if (currTime % 50 == player.getEntityId() % 50) {
                updatePlayer(player);
    
                updateForcedChunks();
            }
        }
    
        if (currTime % 100 == 66) {
            cleanupForRemovedPlayers();
        }
    }
    
    private void removeInactiveEdges(ServerPlayerEntity playerEntity) {
        long serverGameTime = Helper.getServerGameTime();
        playerToEdges.get(playerEntity).stream()
            .filter(
                edge -> serverGameTime - edge.lastActiveGameTime > 20 * 10
            )
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(this::removeEdge);
    }
    
    private void updateForcedChunks() {
        Map<DimensionType, List<DimensionalChunkPos>> newForcedChunkMap =
            chunkPosToEdges.keySet().stream().collect(
                Collectors.groupingBy(chunkPos -> chunkPos.dimension)
            );
        Helper.getServer().getWorlds().forEach(world -> {
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
        playerToEdges.entries().stream()
            .filter(entry -> entry.getKey().removed)
            .map(Map.Entry::getValue)
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(this::removeEdge);
    }
    
    private Stream<DimensionalChunkPos> getNearbyChunkPoses(
        DimensionType dimension,
        BlockPos pos, int radius
    ) {
        ArrayDeque<DimensionalChunkPos> chunkPoses = new ArrayDeque<>();
        ChunkPos portalChunkPos = new ChunkPos(pos);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunkPoses.add(new DimensionalChunkPos(
                    dimension,
                    portalChunkPos.x + dx,
                    portalChunkPos.z + dz
                ));
            }
        }
        return chunkPoses.stream();
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
    
    public void onChunkDataSent(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        Edge edge = getOrAddEdge(chunkPos, player);
        if (edge.isSent) {
            Helper.err(String.format("chunk data sent twice! %s %s",
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
        playerToEdges.removeAll(oldPlayer);
        chunkPosToEdges.entries().removeIf(entry ->
            entry.getValue().player == oldPlayer
        );
    }
    
    public static int getRenderDistanceOnServer() {
        return Helper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
    
}
