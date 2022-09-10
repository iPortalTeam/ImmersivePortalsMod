package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkAccess.class)
public interface IEChunkAccess_AlternateDim {
    @Accessor("noiseChunk")
    void ip_setNoiseChunk(NoiseChunk noiseChunk);
}
