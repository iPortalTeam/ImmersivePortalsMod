package com.qouteall.imm_ptl_peripheral.mixin.common.alternate_dimension;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.NoiseSamplingConfig;
import net.minecraft.world.gen.chunk.SlideConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkGeneratorSettings.class)
public interface IEChunkGeneratorSettings {
    @Invoker("createIslandSettings")
    public static ChunkGeneratorSettings ip_createIslandSettings(
        StructuresConfig structuresConfig, BlockState defaultBlock, BlockState defaultFluid,
        boolean bl, boolean bl2
    ) {
        throw new RuntimeException();
    }
}
