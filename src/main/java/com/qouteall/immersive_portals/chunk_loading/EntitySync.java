package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.ArrayList;
import java.util.List;

public class EntitySync {
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    public static void init() {
        ModMain.postServerTickSignal.connect(EntitySync::tick);
    }
    
    /**
     * Replace {@link ThreadedAnvilChunkStorage#tickPlayerMovement()}
     */
    private static void tick() {
        MinecraftServer server = McHelper.getServer();
        
        server.getProfiler().push("ip_entity_tracking");
        
        List<ServerPlayerEntity> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayerEntity> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayerEntity player : playerList) {
            ThreadedAnvilChunkStorage storage =
                ((ServerWorld) player.world).getChunkManager().threadedAnvilChunkStorage;
            ThreadedAnvilChunkStorage.EntityTracker playerItselfTracker =
                storage.entityTrackers.get(player.getEntityId());
            if (playerItselfTracker != null) {
                if (isDirty(playerItselfTracker)) {
                    dirtyPlayers.add(player);
                }
            }
            else {
                limitedLogger.err(
                    "Entity tracker abnormal " + player + player.world.getRegistryKey().getValue()
                );
            }
        }
        
        server.getWorlds().forEach(world -> {
            ThreadedAnvilChunkStorage storage = world.getChunkManager().threadedAnvilChunkStorage;
            
            for (ThreadedAnvilChunkStorage.EntityTracker tracker : storage.entityTrackers.values()) {
                ((IEEntityTracker) tracker).tickEntry();
                
                List<ServerPlayerEntity> updatedPlayerList = isDirty(tracker) ?
                    playerList : dirtyPlayers;
                
                for (ServerPlayerEntity player : updatedPlayerList) {
                    ((IEEntityTracker) tracker).updateEntityTrackingStatus(player);
                }
                
                markUnDirty(tracker);
            }
        });
        
        server.getProfiler().pop();
    }
    
    private static boolean isDirty(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        ChunkSectionPos newPos = ChunkSectionPos.from(tracker.entity);
        return !tracker.lastCameraPosition.equals(newPos);
    }
    
    private static void markUnDirty(ThreadedAnvilChunkStorage.EntityTracker tracker) {
        tracker.lastCameraPosition = ChunkSectionPos.from(tracker.entity);
    }
}
