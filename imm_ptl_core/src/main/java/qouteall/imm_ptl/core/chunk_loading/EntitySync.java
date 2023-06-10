package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntityTracker;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * In 1.18 MC has two separate entity tracking systems. This looks weird.
 * One is in {@link net.minecraft.server.world.ServerEntityManager},
 * one is in {@link net.minecraft.server.world.ThreadedAnvilChunkStorage.EntityTracker}
 *  and {@link net.minecraft.server.network.EntityTrackerEntry}
 * */
public class EntitySync {
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(EntitySync::tick);
        DynamicDimensionsImpl.beforeRemovingDimensionSignal.connect(EntitySync::forceRemoveDimension);
    }
    
    /**
     * Replace ThreadedAnvilChunkStorage#tickEntityMovement()
     * regarding to the players in all dimensions
     */
    private static void tick() {
        MinecraftServer server = MiscHelper.getServer();
        
        server.getProfiler().push("ip_entity_tracking");
        
        List<ServerPlayer> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayer> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayer player : playerList) {
            ChunkMap storage =
                ((ServerLevel) player.level()).getChunkSource().chunkMap;
            Int2ObjectMap<ChunkMap.TrackedEntity> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).ip_getEntityTrackerMap();
            
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
                ((IEThreadedAnvilChunkStorage) storage).ip_getEntityTrackerMap();
            
            PacketRedirection.withForceRedirect(world, () -> {
                for (ChunkMap.TrackedEntity tracker : entityTrackerMap.values()) {
                    ((IEEntityTracker) tracker).tickEntry();
                    
                    boolean dirty = isDirty(tracker);
                    List<ServerPlayer> updatedPlayerList = dirty ? playerList : dirtyPlayers;
                    
                    for (ServerPlayer player : updatedPlayerList) {
                        ((IEEntityTracker) tracker).updateEntityTrackingStatus(player);
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
        SectionPos newPos = SectionPos.of(((IEEntityTracker) tracker).getEntity_());
        return !((IEEntityTracker) tracker).getLastCameraPosition().equals(newPos);
    }
    
    private static void markUnDirty(ChunkMap.TrackedEntity tracker) {
        SectionPos currPos = SectionPos.of(((IEEntityTracker) tracker).getEntity_());
        ((IEEntityTracker) tracker).setLastCameraPosition(currPos);
    }
    
    private static void forceRemoveDimension(ResourceKey<Level> dimension) {
    
    }
    
}
