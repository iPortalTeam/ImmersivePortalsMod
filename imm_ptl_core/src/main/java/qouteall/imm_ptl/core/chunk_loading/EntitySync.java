package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IETrackedEntity;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.my_util.LimitedLogger;

public class EntitySync {
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    public static void init() {
        DynamicDimensionsImpl.beforeRemovingDimensionEvent.register(EntitySync::forceRemoveDimension);
    }
    
    /**
     * Replace {@link ChunkMap#tick()}
     * regarding to the players in all dimensions
     */
    public static void update(MinecraftServer server) {
        server.getProfiler().push("ip_entity_tracking");
        
        for (ServerLevel world : server.getAllLevels()) {
            ChunkMap chunkMap = world.getChunkSource().chunkMap;
            Int2ObjectMap<ChunkMap.TrackedEntity> entityTrackerMap =
                ((IEChunkMap) chunkMap).ip_getEntityTrackerMap();
            
            for (ChunkMap.TrackedEntity trackedEntity : entityTrackerMap.values()) {
                ((IETrackedEntity) trackedEntity).ip_updateEntityTrackingStatus();
            }
        }
        
        server.getProfiler().pop();
    }
    
    private static void forceRemoveDimension(ResourceKey<Level> dimension) {
    
    }
    
}
