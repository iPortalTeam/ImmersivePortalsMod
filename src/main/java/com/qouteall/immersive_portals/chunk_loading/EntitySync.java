package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEEntityTracker;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EntitySync {
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    /**
     *  If it's not null, all sent packets will be wrapped into redirected packet
     *  {@link com.qouteall.immersive_portals.mixin.entity_sync.MixinServerPlayNetworkHandler_E}
     */
    @Nullable
    public static RegistryKey<World> forceRedirect = null;
    
    public static void init() {
        ModMain.postServerTickSignal.connect(EntitySync::tick);
    }
    
    /**
     * Replace ThreadedAnvilChunkStorage#tickPlayerMovement()
     */
    private static void tick() {
        MinecraftServer server = McHelper.getServer();
        
        server.getProfiler().push("ip_entity_tracking");
        
        List<ServerPlayerEntity> playerList = McHelper.getRawPlayerList();
        
        List<ServerPlayerEntity> dirtyPlayers = new ArrayList<>();
        
        for (ServerPlayerEntity player : playerList) {
            ThreadedAnvilChunkStorage storage =
                ((ServerWorld) player.world).getChunkManager().threadedAnvilChunkStorage;
            Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).getEntityTrackerMap();
            
            ThreadedAnvilChunkStorage.EntityTracker playerItselfTracker =
                entityTrackerMap.get(player.getEntityId());
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
            Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackerMap =
                ((IEThreadedAnvilChunkStorage) storage).getEntityTrackerMap();
    
            forceRedirect = world.getRegistryKey();
            
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
    
            forceRedirect = null;
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
    
    public static void withForceRedirect(RegistryKey<World> dimension, Runnable func) {
        RegistryKey<World> oldForceRedirect = EntitySync.forceRedirect;
        forceRedirect = dimension;
        func.run();
        forceRedirect = oldForceRedirect;
    }
}
