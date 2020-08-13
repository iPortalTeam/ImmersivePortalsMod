package com.qouteall.immersive_portals.mixin.alternate_dimension;

import net.minecraft.world.gen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(OreFeature.class)
public class MixinOreFeature {
//    @ModifyVariable(
//        method = "Lnet/minecraft/world/gen/feature/OreFeature;generate(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/gen/feature/OreFeatureConfig;)Z",
//        at = @At("HEAD"),
//        argsOnly = true
//    )
//    private OreFeatureConfig modifyOreFeatureConfig(
//        OreFeatureConfig oreFeatureConfig,
//        StructureWorldAccess serverWorldAccess,
//        StructureAccessor structureAccessor,
//        ChunkGenerator chunkGenerator,
//        Random random,
//        BlockPos blockPos,
//        OreFeatureConfig oreFeatureConfig1
//    ) {
//        if (chunkGenerator instanceof ErrorTerrainGenerator) {
//            BlockState state = oreFeatureConfig.state;
//            if (state.getBlock() instanceof OreBlock) {
//                if (state.getBlock() != Blocks.COAL_ORE) {
//                    return new OreFeatureConfig(
//                        oreFeatureConfig.target,
//                        oreFeatureConfig.state,
//                        oreFeatureConfig.size * 3
//                    );
//                }
//            }
//        }
//        return oreFeatureConfig;
//    }
}
