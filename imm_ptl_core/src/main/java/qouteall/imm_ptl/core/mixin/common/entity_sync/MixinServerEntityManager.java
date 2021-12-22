package qouteall.imm_ptl.core.mixin.common.entity_sync;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityTrackingStatus;
import net.minecraft.world.storage.ChunkDataList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEServerEntityManager;

import java.util.Queue;

@Mixin(ServerEntityManager.class)
public class MixinServerEntityManager implements IEServerEntityManager {
    @Shadow
    @Final
    private Long2ObjectMap<EntityTrackingStatus> trackingStatuses;
    
    @Shadow
    @Final
    private Long2ObjectMap managedStatuses;
    
    @Shadow
    @Final
    private LongSet pendingUnloads;
    
    @Shadow
    @Final
    private Queue loadingQueue;
    
    /**
     * @author qouteall
     * @reason make incompat fail fast
     * This is originally called from
     * {@link net.minecraft.server.world.ThreadedAnvilChunkStorage#onChunkStatusChange(ChunkPos, ChunkHolder.LevelType)}
     * ImmPtl will manage it in {@link qouteall.imm_ptl.core.chunk_loading.ServerEntityStorageManagement}
     */
    @Overwrite
    public void updateTrackingStatus(ChunkPos chunkPos, ChunkHolder.LevelType levelType) {
        throw new RuntimeException("This is possibly a mod compat issue with ImmPtl");
    }
}
