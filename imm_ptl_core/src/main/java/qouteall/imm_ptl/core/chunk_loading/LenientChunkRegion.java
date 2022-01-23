package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

// Lenient means getBlockState does not crash if out of bound
public class LenientChunkRegion extends WorldGenRegion {
    
    public LenientChunkRegion(ServerLevel world, List<ChunkAccess> chunks) {
        super(world, chunks, null, 99999);
    }
    
    static LenientChunkRegion createLenientChunkRegion(
        DimensionalChunkPos center, int radius, ServerLevel world
    ) {
        List<ChunkAccess> chunks = new ArrayList<>();
    
        for (int z = center.z - radius; z <= center.z + radius; z++) {
            for (int x = center.x - radius; x <= center.x + radius; x++) {
                chunks.add(world.getChunk(x, z));
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
