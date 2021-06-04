package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.List;

// Lenient means getBlockState does not crash if out of bound
public class LenientChunkRegion extends ChunkRegion {
    
    public LenientChunkRegion(ServerWorld world, List<Chunk> chunks) {
        super(world, chunks, null, 99999);
    }
    
    static LenientChunkRegion createLenientChunkRegion(
        DimensionalChunkPos center, int radius, ServerWorld world
    ) {
        List<Chunk> chunks = new ArrayList<>();
    
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
        final Chunk chunk = this.getChunk(
            pos.getX() >> 4, pos.getZ() >> 4,
            ChunkStatus.FULL, false
        );
        if (chunk == null) {
            return Blocks.AIR.getDefaultState();
        }
        return chunk.getBlockState(pos);
    }
}
