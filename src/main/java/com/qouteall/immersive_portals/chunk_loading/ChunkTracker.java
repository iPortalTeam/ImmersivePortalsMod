package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
    private Set<DimensionalChunkPos> portalLoadedChunks = new HashSet<>();
    private Multimap<DimensionalChunkPos, ServerPlayerEntity> chunkToWatchingPlayers = HashMultimap.create();
    private Multimap<ServerPlayerEntity, DimensionalChunkPos> playerToWatchedChunks = HashMultimap.create();
    
    private static final ChunkTicketType<ChunkPos> immersiveTicketType =
        ChunkTicketType.create(
            "immersive_portal_ticket",
            Comparator.comparingLong(ChunkPos::toLong)
        );
    
    public static final int portalLoadingRange = 48;
    public static final int portalChunkLoadingRadius = 1;
    
    public SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    public ChunkTracker() {
        ModMain.clientTickSignal.connectWithWeakRef(this, ChunkTracker::tick);
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
    
    private void updatePlayer(ServerPlayerEntity playerEntity) {
        HashSet<DimensionalChunkPos> oldPlayerViewingChunks =
            new HashSet<>(playerToWatchedChunks.get(playerEntity));
        Set<DimensionalChunkPos> newPlayerViewingChunks = getPlayerViewingChunks(
            playerEntity
        );
        playerToWatchedChunks.replaceValues(playerEntity, newPlayerViewingChunks);
        
        Helper.compareOldAndNew(
            oldPlayerViewingChunks,
            newPlayerViewingChunks,
            chunkPos -> {
                chunkToWatchingPlayers.remove(chunkPos, playerEntity);
                endWatchChunkSignal.emit(playerEntity, chunkPos);
            },
            chunkPos -> {
                chunkToWatchingPlayers.put(chunkPos, playerEntity);
                beginWatchChunkSignal.emit(playerEntity, chunkPos);
            }
        );
    }
    
    private Set<DimensionalChunkPos> getPlayerViewingChunks(
        ServerPlayerEntity playerEntity
    ) {
        return Helper.getEntitiesNearby(
            playerEntity,
            Portal.entityType,
            portalLoadingRange
        ).flatMap(
            portalEntity -> getNearbyChunkPoses(
                portalEntity.dimensionTo,
                new BlockPos(portalEntity.destination),
                ChunkTracker.portalChunkLoadingRadius
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
            updateChunkTickets();
            
            purge();
        }
    }
    
    private void updateChunkTickets() {
        Set<DimensionalChunkPos> oldPortalLoadedChunks = this.portalLoadedChunks;
        Set<DimensionalChunkPos> newPortalLoadedChunks = chunkToWatchingPlayers.keySet();
        
        portalLoadedChunks = newPortalLoadedChunks;
        
        assert newPortalLoadedChunks.stream().noneMatch(
            dimensionalChunkPos -> chunkToWatchingPlayers.get(dimensionalChunkPos).isEmpty()
        );
        
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
    
    private void purge() {
        //TODO delete entries about removed players
    }
    
    private Stream<DimensionalChunkPos> getNearbyChunkPoses(
        DimensionType dimension,
        BlockPos pos, int radius
    ) {
        List<DimensionalChunkPos> chunkPoses = new ArrayList<>();
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
    
    public Collection<ServerPlayerEntity> getPlayersViewingChunk(
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
    
    public boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        return chunkToWatchingPlayers.containsEntry(chunkPos, player);
    }
}
