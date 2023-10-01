package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEChunkMap;
import qouteall.imm_ptl.core.ducks.IETrackedEntity;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.util.ArrayList;
import java.util.List;

public class EntitySync {
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            EntitySync.tick(server);
        });
        
        DynamicDimensionsImpl.beforeRemovingDimensionEvent.register(EntitySync::forceRemoveDimension);
    }
    
    /**
     * Replace {@link ChunkMap#tick()}
     * regarding to the players in all dimensions
     */
    private static void tick(MinecraftServer server) {
        server.getProfiler().push("ip_entity_tracking");
        
        List<ServerPlayer> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayer> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayer player : playerList) {
            ChunkMap storage =
                ((ServerLevel) player.level()).getChunkSource().chunkMap;
            Int2ObjectMap<ChunkMap.TrackedEntity> entityTrackerMap =
                ((IEChunkMap) storage).ip_getEntityTrackerMap();
            
            ChunkMap.TrackedEntity playerItselfTracker =
                entityTrackerMap.get(player.getId());
            if (playerItselfTracker != null) {
                if (isDirty(playerItselfTracker)) {
                    dirtyPlayers.add(player);
                }
            }
            else {
//                limitedLogger.err(
//                    "Entity tracker abnormal " + player + player.world.getRegistryKey().getValue()
//                );
            }
        }
        
        server.getAllLevels().forEach(world -> {
            ChunkMap storage = world.getChunkSource().chunkMap;
            Int2ObjectMap<ChunkMap.TrackedEntity> entityTrackerMap =
                ((IEChunkMap) storage).ip_getEntityTrackerMap();
            
            PacketRedirection.withForceRedirect(world, () -> {
                for (ChunkMap.TrackedEntity tracker : entityTrackerMap.values()) {
                    ((IETrackedEntity) tracker).ip_tickEntry();
                    
                    boolean dirty = isDirty(tracker);
                    List<ServerPlayer> updatedPlayerList = dirty ? playerList : dirtyPlayers;
                    
                    for (ServerPlayer player : updatedPlayerList) {
                        ((IETrackedEntity) tracker).ip_updateEntityTrackingStatus(player);
                    }
                    
                    if (dirty) {
                        markUnDirty(tracker);
                    }
                }
            });
        });
        
        server.getProfiler().pop();
    }
    
    private static boolean isDirty(ChunkMap.TrackedEntity tracker) {
        SectionPos newPos = SectionPos.of(((IETrackedEntity) tracker).ip_getEntity());
        return !((IETrackedEntity) tracker).ip_getLastCameraPosition().equals(newPos);
    }
    
    private static void markUnDirty(ChunkMap.TrackedEntity tracker) {
        SectionPos currPos = SectionPos.of(((IETrackedEntity) tracker).ip_getEntity());
        ((IETrackedEntity) tracker).ip_setLastCameraPosition(currPos);
    }
    
    private static void forceRemoveDimension(ResourceKey<Level> dimension) {
    
    }
    
}
