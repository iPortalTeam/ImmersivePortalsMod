package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import qouteall.imm_ptl.core.McHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A WorldGenRegion wrapper that is tolerant to missing chunks.
 * It will return AIR for block states in chunks that are not present.
 * This class avoids creating/loading chunks when used with non-creating accessors.
 */
public class LenientChunkRegion extends WorldGenRegion {

    private static final int WORLDGEN_REGION_TIMEOUT = 99999;

    public LenientChunkRegion(ServerLevel world, List<ChunkAccess> chunks) {
        super(world, chunks, ChunkStatus.FULL, WORLDGEN_REGION_TIMEOUT);
    }

    /**
     * Create a lenient chunk region around a center chunk using non-creating accessors.
     * Null chunks are simply skipped so the resulting region may contain fewer chunks than the full square.
     */
    public static LenientChunkRegion createLenientChunkRegion(
        DimensionalChunkPos center, int radius, ServerLevel world
    ) {
        List<ChunkAccess> chunks = new ArrayList<>();

        // use non-creating accessor to avoid triggering chunk generation
        for (int z = center.z - radius; z <= center.z + radius; z++) {
            for (int x = center.x - radius; x <= center.x + radius; x++) {
                ChunkAccess c = McHelper.getServerChunkIfPresent(world, x, z);
                if (c != null) {
                    chunks.add(c);
                }
            }
        }

        return new LenientChunkRegion(
            world, chunks
        );
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        final ChunkAccess chunk = this.getChunk(
            pos.getX() >> 4, pos.getZ() >> 4,
            ChunkStatus.FULL, false
        );
        if (chunk == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return chunk.getBlockState(pos);
    }
}
