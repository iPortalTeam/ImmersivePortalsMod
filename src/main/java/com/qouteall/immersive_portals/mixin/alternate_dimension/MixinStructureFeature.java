package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
