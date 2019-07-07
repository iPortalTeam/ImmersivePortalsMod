package com.qouteall.immersive_portals.world_syncing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qouteall.immersive_portals.exposer.IEServerChunkManager;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal_entity.PortalEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChunkSyncingManager {
    /**
     * see{@link net.minecraft.server.world.ChunkTicketManager.DistanceFromNearestPlayerTracker#getInitialLevel(long)}
     * see{@link net.minecraft.server.world.ChunkTicketManager#handleChunkEnter(ChunkSectionPos, ServerPlayerEntity)}
     * see{@link net.minecraft.server.world.ChunkTicketManager#handleChunkLeave(ChunkSectionPos, ServerPlayerEntity)}}
     * see{@link net.minecraft.server.world.ThreadedAnvilChunkStorage#getPlayersWatchingChunk(ChunkPos, boolean)}
     * {@link ChunkHolder#sendPacketToPlayersWatching(Packet, boolean)}
     */
    public static class DimensionalChunkPos {
        public DimensionType dimensionType;
        public int x;
        public int z;
        
        public DimensionalChunkPos(DimensionType dimensionType, int x, int z) {
            this.dimensionType = dimensionType;
            this.x = x;
            this.z = z;
        }
        
        public DimensionalChunkPos(DimensionType dimensionType, ChunkPos chunkPos) {
            this(dimensionType, chunkPos.x, chunkPos.z);
        }
        
        public ChunkPos getChunkPos() {
            return new ChunkPos(x, z);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DimensionalChunkPos)) {
                return false;
            }
            DimensionalChunkPos obj1 = (DimensionalChunkPos) obj;
            return (dimensionType == obj1.dimensionType) &&
                (x == obj1.x) &&
                (z == obj1.z);
        }
    }
    
    
    private Multimap<DimensionalChunkPos, ServerPlayerEntity> indirectVisibilityMap;
    
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
    
    public void tick() {
        Multimap<DimensionalChunkPos, ServerPlayerEntity> oldMap = this.indirectVisibilityMap;
        Multimap<DimensionalChunkPos, ServerPlayerEntity> newMap = computeNewIndirectVisibilityMap();
        indirectVisibilityMap = newMap;
        
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
    
    private Multimap<DimensionalChunkPos, ServerPlayerEntity> computeNewIndirectVisibilityMap() {
        Multimap<DimensionalChunkPos, ServerPlayerEntity> newMap = new HashMultimap<>();
        Helper.getServer().getPlayerManager().getPlayerList().forEach(
            playerEntity -> Helper.getEntitiesNearby(
                playerEntity,
                PortalEntity.entityType,
                portalLoadingRange
            ).forEach(
                portalEntity -> getNearbyChunkPoses(portalEntity).forEach(
                    dimensionalChunkPos -> newMap.put(dimensionalChunkPos, playerEntity)
                )
            )
        );
        return newMap;
    }
    
    private Stream<DimensionalChunkPos> getNearbyChunkPoses(PortalEntity portal) {
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
        return indirectVisibilityMap.get(
            new DimensionalChunkPos(
                dimensionType,
                chunkPos.x,
                chunkPos.z
            )
        );
    }
}
