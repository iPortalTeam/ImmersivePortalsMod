package qouteall.imm_ptl.core.mixin.common.entity_sync;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityTrackingStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ServerEntityManager.class)
public class MixinServerEntityManager {
    /**
     * @author qouteall
     * @reason make incompat fail fast
     * This is originally called from
     *     {@link net.minecraft.server.world.ThreadedAnvilChunkStorage#onChunkStatusChange(ChunkPos, ChunkHolder.LevelType)}
     * ImmPtl will manage it in {@link qouteall.imm_ptl.core.chunk_loading.ServerEntityStorageManagement}
     */
    @Overwrite
    public void updateTrackingStatus(ChunkPos chunkPos, ChunkHolder.LevelType levelType) {
        throw new RuntimeException("This is possibly a mod compat issue with ImmPtl");
    }
}
