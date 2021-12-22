package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEEntityTracker;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.network.IPCommonNetwork;
import qouteall.q_misc_util.MiscHelper;
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
    }
    
    /**
     * Replace ThreadedAnvilChunkStorage#tickEntityMovement()
     * regarding to the players in all dimensions
     */
    private static void tick() {
        MinecraftServer server = MiscHelper.getServer();
        
        server.getProfiler().push("ip_entity_tracking");
        
        List<ServerPlayerEntity> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayerEntity> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayerEntity player : playerList) {
            ThreadedAnvilChunkStorage storage =
                ((ServerWorld) player.world).getChunkManager().threadedAnvilChunkStorage;
            Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).getEntityTrackerMap();
            
            ThreadedAnvilChunkStorage.EntityTracker playerItselfTracker =
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
        
        server.getWorlds().forEach(world -> {
            ThreadedAnvilChunkStorage storage = world.getChunkManager().threadedAnvilChunkStorage;
            Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).getEntityTrackerMap();
            
            IPCommonNetwork.withForceRedirect(world, () -> {
                for (ThreadedAnvilChunkStorage.EntityTracker tracker : entityTrackerMap.values()) {
                    ((IEEntityTracker) tracker).tickEntry();
                    
                    boolean dirty = isDirty(tracker);
                    List<ServerPlayerEntity> updatedPlayerList = dirty ? playerList : dirtyPlayers;
                    
                    for (ServerPlayerEntity player : updatedPlayerList) {
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
    
    private static boolean isDirty(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        ChunkSectionPos newPos = ChunkSectionPos.from(((IEEntityTracker) tracker).getEntity_());
        return !((IEEntityTracker) tracker).getLastCameraPosition().equals(newPos);
    }
    
    private static void markUnDirty(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        ChunkSectionPos currPos = ChunkSectionPos.from(((IEEntityTracker) tracker).getEntity_());
        ((IEEntityTracker) tracker).setLastCameraPosition(currPos);
    }
    
}
