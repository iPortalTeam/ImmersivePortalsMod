package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qouteall.immersive_portals.exposer.IEServerChunkManager;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;
import java.util.stream.Stream;

public class ChunkSyncingManager {
    
    /**
     * see{@link net.minecraft.server.world.ChunkTicketManager.DistanceFromNearestPlayerTracker#getInitialLevel(long)}
     * see{@link net.minecraft.server.world.ChunkTicketManager#handleChunkEnter(ChunkSectionPos, ServerPlayerEntity)}
     * see{@link net.minecraft.server.world.ChunkTicketManager#handleChunkLeave(ChunkSectionPos, ServerPlayerEntity)}}
     * see{@link net.minecraft.server.world.ThreadedAnvilChunkStorage#getPlayersWatchingChunk(ChunkPos, boolean)}
     * {@link ChunkHolder#sendPacketToPlayersWatching(Packet, boolean)}
     */
    
    /**
     * {@link ThreadedAnvilChunkStorage#updateCameraPosition(ServerPlayerEntity)}
     */
    
    private Map<ServerPlayerEntity, Vec3d> lastPosUponUpdatingMap = new HashMap<>();
    private Multimap<DimensionalChunkPos, ServerPlayerEntity> chunkToWatchingPlayers = HashMultimap.create();
    private Multimap<ServerPlayerEntity, DimensionalChunkPos> playerToWatchedChunks = HashMultimap.create();
    
    private static final ChunkTicketType<ChunkPos> immersiveTicketType =
        ChunkTicketType.create(
            "immersive_portal_ticket",
            Comparator.comparingLong(ChunkPos::toLong)
        );
    
    public static final int portalLoadingRange = 48;
    public static final int portalChunkLoadingRadius = 1;
    
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
    
    private void updatePlayer() {
    
    }
    
    public void tick() {
        Multimap<DimensionalChunkPos, ServerPlayerEntity> oldMap = this.chunkToWatchingPlayers;
        Multimap<DimensionalChunkPos, ServerPlayerEntity> newMap = computeNewVisibilityMap();
        chunkToWatchingPlayers = newMap;
        
        Set<DimensionalChunkPos> oldWatchedChunks = oldMap.keySet();
        Set<DimensionalChunkPos> newWatchedChunks = newMap.keySet();
        
        assert newWatchedChunks.stream().noneMatch(
            dimensionalChunkPos -> newMap.get(dimensionalChunkPos).isEmpty()
        );
        
        Stream<DimensionalChunkPos> chunksToUnload = oldWatchedChunks.stream().filter(
            chunk -> !newWatchedChunks.contains(chunk)
        );
        Stream<DimensionalChunkPos> chunksToLoad = newWatchedChunks.stream().filter(
            chunk -> !oldWatchedChunks.contains(chunk)
        );
        
        chunksToUnload.forEach(
            chunk -> setIsLoadedByPortal(chunk.dimensionType, chunk.getChunkPos(), false)
        );
        
        chunksToLoad.forEach(
            chunk -> setIsLoadedByPortal(chunk.dimensionType, chunk.getChunkPos(), true)
        );
    }
    
    private Multimap<DimensionalChunkPos, ServerPlayerEntity> computeNewVisibilityMap() {
        Multimap<DimensionalChunkPos, ServerPlayerEntity> newMap = HashMultimap.create();
        Helper.getServer().getPlayerManager().getPlayerList().forEach(
            playerEntity -> Helper.getEntitiesNearby(
                playerEntity,
                Portal.entityType,
                portalLoadingRange
            ).forEach(
                portalEntity -> getNearbyChunkPoses(portalEntity).forEach(
                    dimensionalChunkPos -> newMap.put(dimensionalChunkPos, playerEntity)
                )
            )
        );
        return newMap;
    }
    
    private Stream<DimensionalChunkPos> getNearbyChunkPoses(Portal portal) {
        List<DimensionalChunkPos> chunkPoses = new ArrayList<>();
        ChunkPos portalChunkPos = new ChunkPos(portal.getBlockPos());
        for (int dx = -portalChunkLoadingRadius; dx <= portalChunkLoadingRadius; dx++) {
            for (int dz = -portalChunkLoadingRadius; dz <= portalChunkLoadingRadius; dz++) {
                chunkPoses.add(new DimensionalChunkPos(
                    portal.dimension,
                    portalChunkPos.x + dx,
                    portalChunkPos.z + dz
                ));
            }
        }
        return chunkPoses.stream();
    }
    
    public Collection<ServerPlayerEntity> getIndirectViewers(
        DimensionType dimensionType,
        ChunkPos chunkPos
    ) {
        assert dimensionType != null;
        return chunkToWatchingPlayers.get(
            new DimensionalChunkPos(
                dimensionType,
                chunkPos.x,
                chunkPos.z
            )
        );
    }
}
