package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Supplier;

@Mixin(ChunkGenerator.class)
public interface IEChunkGenerator_AlternateDim {
    @Accessor("featuresPerStep")
    @Mutable
    void ip_setFeaturesPerStep(Supplier<List<FeatureSorter.StepFeatureData>> arg);
}
