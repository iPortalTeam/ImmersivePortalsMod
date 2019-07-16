package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.exposer.IEServerChunkManager;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
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
    
    private Map<ServerPlayerEntity, Vec3d> lastPosUponUpdatingMap = new HashMap<>();
    private Set<DimensionalChunkPos> portalLoadedChunks = new HashSet<>();
    private Multimap<DimensionalChunkPos, Edge> chunkPosToEdges = HashMultimap.create();
    private Multimap<ServerPlayerEntity, Edge> playerToEdges = HashMultimap.create();
    
    public ChunkTracker() {
        ModMain.postClientTickSignal.connectWithWeakRef(this, ChunkTracker::tick);
    }
    
    /**
     * {@link ChunkTicketManager#setChunkForced(ChunkPos, boolean)}
     */
    private void setIsLoadedByPortal(
        DimensionType dimension,
        ChunkPos chunkPos,
        boolean isLoadedNow
    ) {
        ServerWorld world = Helper.getServer().getWorld(dimension);
        ServerChunkManager chunkManager = world.method_14178();
        ChunkTicketManager ticketManager = ((IEServerChunkManager) chunkManager).getTicketManager();
        
        if (isLoadedNow) {
            ticketManager.addTicket(
                immersiveTicketType,
                chunkPos,
                1,
                chunkPos
            );
        }
        else {
            ticketManager.removeTicket(
                immersiveTicketType,
                chunkPos,
                1,
                chunkPos
            );
        }
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
        
        beginWatchChunkSignal.emit(player, chunkPos);
        
        return edge;
    }
    
    private void removeEdge(Edge edge) {
        chunkPosToEdges.entries().removeIf(
            entry -> entry.getValue() == edge
        );
        playerToEdges.entries().removeIf(
            entry -> entry.getValue() == edge
        );
        
        if (!edge.player.removed) {
            endWatchChunkSignal.emit(edge.player, edge.chunkPos);
        }
    }
    
    private void updatePlayer(ServerPlayerEntity playerEntity) {
        Set<DimensionalChunkPos> newPlayerViewingChunks = getPlayerViewingChunks(
            playerEntity
        );
        newPlayerViewingChunks.forEach(chunkPos -> {
            Edge edge = getOrAddEdge(chunkPos, playerEntity);
            edge.lastActiveGameTime = Helper.getServerGameTime();
        });
    }
    
    //TODO invoke this upon creating portal
    public void notifyToUpdatePlayer(ServerPlayerEntity playerEntity) {
        lastPosUponUpdatingMap.remove(playerEntity);
    }
    
    private Set<DimensionalChunkPos> getPlayerViewingChunks(
        ServerPlayerEntity player
    ) {
        int portalChunkLoadingRadius = getRenderDistanceOnServer() / 3 + 1;
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
        for (ServerPlayerEntity player : playerList) {
            Vec3d lastUpdatePos = lastPosUponUpdatingMap.get(player);
            if (lastUpdatePos == null ||
                player.getPos().squaredDistanceTo(lastUpdatePos) > 8 * 8
            ) {
                lastPosUponUpdatingMap.put(player, player.getPos());
                updatePlayer(player);
            }
        }
    
        if (Helper.getServerGameTime() % 20 == 7) {
            removeInactiveEdges();
        
            cleanupForRemovedPlayers();
            
            updateChunkTickets();
        }
    }
    
    private void removeInactiveEdges() {
        long serverGameTime = Helper.getServerGameTime();
        playerToEdges.values().stream()
            .filter(
                edge -> serverGameTime - edge.lastActiveGameTime > 20 * 10
            )
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(this::removeEdge);
    }
    
    private void updateChunkTickets() {
        Set<DimensionalChunkPos> oldPortalLoadedChunks = this.portalLoadedChunks;
        Set<DimensionalChunkPos> newPortalLoadedChunks = chunkPosToEdges.keySet();
        
        portalLoadedChunks = newPortalLoadedChunks;
        
        Helper.compareOldAndNew(
            oldPortalLoadedChunks,
            newPortalLoadedChunks,
            chunkPos -> setIsLoadedByPortal(
                chunkPos.dimension, chunkPos.getChunkPos(), false
            ),
            chunkPos -> setIsLoadedByPortal(
                chunkPos.dimension, chunkPos.getChunkPos(), true
            )
        );
    }
    
    private void cleanupForRemovedPlayers() {
        lastPosUponUpdatingMap.entrySet().removeIf(
            entry -> entry.getKey().removed
        );
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
    
    public static int getRenderDistanceOnServer() {
        return Helper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
}
