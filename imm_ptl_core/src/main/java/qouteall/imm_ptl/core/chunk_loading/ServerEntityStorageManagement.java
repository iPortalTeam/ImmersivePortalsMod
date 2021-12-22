package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityTrackingStatus;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEServerWorld;
import qouteall.q_misc_util.MiscHelper;

import java.util.HashMap;
import java.util.Map;

public class ServerEntityStorageManagement {
    
    private static final Map<RegistryKey<World>, LongSet> chunksToUpdate = new HashMap<>();
    
    public static void init() {
        IPGlobal.serverCleanupSignal.connect(() -> {
            chunksToUpdate.clear();
        });
        
        IPGlobal.postServerTickSignal.connect(() -> {
            tick();
        });
    }
    
    private static void tick() {
        MinecraftServer server = MiscHelper.getServer();
        
        int simulationDistance = server.getPlayerManager().getSimulationDistance();
        
        chunksToUpdate.forEach((dimension, chunks) -> {
            ServerWorld world = server.getWorld(dimension);
            if (world == null) {
                return;
            }
            
            ServerEntityManager<Entity> entityManager = ((IEServerWorld) world).ip_getEntityManager();
            
            chunks.forEach((long chunkPos) -> {
                int watchingDistance = NewChunkTrackingGraph.getMinimumWatchingDistance(
                    dimension, chunkPos
                );
                if (watchingDistance == -1) {
                    entityManager.updateTrackingStatus(
                        new ChunkPos(chunkPos), EntityTrackingStatus.HIDDEN
                    );
                }
                else {
                    if (watchingDistance > simulationDistance) {
                        entityManager.updateTrackingStatus(
                            new ChunkPos(chunkPos), EntityTrackingStatus.TRACKED
                        );
                    }
                    else {
                        entityManager.updateTrackingStatus(
                            new ChunkPos(chunkPos), EntityTrackingStatus.TICKING
                        );
                    }
                }
            });
            
        });
    }
    
    public static void onChunkWatchStatusChange(
        RegistryKey<World> dimension,
        long chunkPos
    ) {
        chunksToUpdate.computeIfAbsent(
            dimension,
            d -> new LongAVLTreeSet()
        ).add(chunkPos);
    }
}
