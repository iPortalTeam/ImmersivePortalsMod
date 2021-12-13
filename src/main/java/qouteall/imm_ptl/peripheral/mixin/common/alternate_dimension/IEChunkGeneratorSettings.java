package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkGeneratorSettings.class)
public interface IEChunkGeneratorSettings {
    @Invoker("<init>")
    public static ChunkGeneratorSettings construct(
        StructuresConfig structuresConfig, GenerationShapeConfig generationShapeConfig,
        BlockState defaultBlock, BlockState defaultFluid, MaterialRules.MaterialRule surfaceRule,
        int bedrockCeilingY, boolean mobGenerationDisabled, boolean aquifers, boolean noiseCaves,
        boolean oreVeins, boolean noodleCaves, boolean useLegacyRandom
    ) {
        throw new RuntimeException();
    }
    
}
