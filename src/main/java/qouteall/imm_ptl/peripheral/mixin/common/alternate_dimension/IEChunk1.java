package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Chunk.class)
public interface IEChunk1 {
    @Accessor("chunkNoiseSampler")
    void ip_setChunkNoiseSampler(ChunkNoiseSampler var);
}
