package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import qouteall.dimlib.api.DimensionAPI;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IETrackedEntity;
import qouteall.imm_ptl.core.network.PacketRedirection;

public class EntitySync {
    
    public static void init() {
        DimensionAPI.SERVER_PRE_REMOVE_DIMENSION_EVENT.register(EntitySync::forceRemoveDimension);
    }
    
    /**
     * Replace {@link ChunkMap#tick()}
     * regarding the players in all dimensions
     */
    public static void update(MinecraftServer server) {
        server.getProfiler().push("ip_entity_tracking_update");
        
        for (ServerLevel world : server.getAllLevels()) {
            PacketRedirection.withForceRedirect(
                world,
                () -> {
                    ChunkMap chunkMap = world.getChunkSource().chunkMap;
                    Int2ObjectMap<ChunkMap.TrackedEntity> entityTrackerMap =
                        ((IEChunkMap) chunkMap).ip_getEntityTrackerMap();
                    DistanceManager distanceManager = chunkMap.getDistanceManager();
                    
                    for (ChunkMap.TrackedEntity trackedEntity : entityTrackerMap.values()) {
                        IETrackedEntity ieTrackedEntity = (IETrackedEntity) trackedEntity;
                        ieTrackedEntity.ip_updateEntityTrackingStatus();
                    }
                }
            );
        }
        
        server.getProfiler().pop();
    }
    
    public static void tick(MinecraftServer server) {
        server.getProfiler().push("ip_entity_tracking_tick");
        
        for (ServerLevel world : server.getAllLevels()) {
            PacketRedirection.withForceRedirect(
                world,
                () -> {
                    ChunkMap chunkMap = world.getChunkSource().chunkMap;
                    Int2ObjectMap<ChunkMap.TrackedEntity> entityTrackerMap =
                        ((IEChunkMap) chunkMap).ip_getEntityTrackerMap();
                    DistanceManager distanceManager = chunkMap.getDistanceManager();
                    
                    for (ChunkMap.TrackedEntity trackedEntity : entityTrackerMap.values()) {
                        IETrackedEntity ieTrackedEntity = (IETrackedEntity) trackedEntity;
                        
                        long chunkPos = ieTrackedEntity.ip_getEntity().chunkPosition().toLong();
                        if (distanceManager.inEntityTickingRange(chunkPos)) {
                            ieTrackedEntity.ip_sendChanges();
                        }
                    }
                }
            );
            
            
        }
        
        server.getProfiler().pop();
    }
    
    private static void forceRemoveDimension(ServerLevel world) {
    
    }
    
}
