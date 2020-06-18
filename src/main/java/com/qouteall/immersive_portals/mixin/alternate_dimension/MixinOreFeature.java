package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OreBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.OreFeature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(OreFeature.class)
public class MixinOreFeature {
    @ModifyVariable(
        method = "generate",
        at = @At("HEAD"),
        argsOnly = true
    )
    private OreFeatureConfig modifyOreFeatureConfig(
        OreFeatureConfig oreFeatureConfig,
        ServerWorldAccess serverWorldAccess,
        StructureAccessor structureAccessor,
        ChunkGenerator chunkGenerator,
        Random random,
        BlockPos blockPos,
        OreFeatureConfig oreFeatureConfig1
    ) {
        if (chunkGenerator instanceof ErrorTerrainGenerator) {
            BlockState state = oreFeatureConfig.state;
            if (state.getBlock() instanceof OreBlock) {
                if (state.getBlock() != Blocks.COAL_ORE) {
                    return new OreFeatureConfig(
                        oreFeatureConfig.target,
                        oreFeatureConfig.state,
                        oreFeatureConfig.size * 3
                    );
                }
            }
        }
        return oreFeatureConfig;
    }
}
