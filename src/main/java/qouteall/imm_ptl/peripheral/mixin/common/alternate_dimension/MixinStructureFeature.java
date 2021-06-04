package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StructureFeature.class)
public class MixinStructureFeature {
//    @Redirect(
//        method = "method_28657",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/gen/feature/StructureFeature;shouldStartAt(Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/world/biome/source/BiomeSource;JLnet/minecraft/world/gen/ChunkRandom;IILnet/minecraft/world/biome/Biome;Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/gen/feature/FeatureConfig;)Z"
//        )
//    )
//    private boolean redirectShouldStartAt(
//        StructureFeature structureFeature,
//        ChunkGenerator chunkGenerator,
//        BiomeSource biomeSource,
//        long l,
//        ChunkRandom chunkRandom,
//        int i,
//        int j,
//        Biome biome,
//        ChunkPos chunkPos,
//        FeatureConfig featureConfig
//    ) {
//        if(chunkGenerator instanceof ErrorTerrainGenerator){
//
//        }else {
//
//        }
//    }
}
