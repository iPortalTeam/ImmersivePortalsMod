package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(NoiseChunkGenerator.class)
public abstract class MixinNoiseChunkGenerator extends ChunkGenerator {
    
    @Shadow
    @Final
    public Supplier<ChunkGeneratorSettings> settings;
    
    @Shadow
    @Final
    private int worldHeight;
    
    public MixinNoiseChunkGenerator(BiomeSource biomeSource, StructuresConfig arg) {
        super(biomeSource, arg);
    }
//
//    @Inject(
//        method = "buildBedrock",
//        at = @At("HEAD"),
//        cancellable = true
//    )
//    private void onBuildBedrock(Chunk chunk, Random random, CallbackInfo ci) {
//        if (AltiusGameRule.getIsDimensionStack()) {
//            buildAltiusBedrock(chunk, random);
//            ci.cancel();
//        }
//    }
//
//    private void buildAltiusBedrock(Chunk chunk, Random random) {
//        BlockPos.Mutable mutable = new BlockPos.Mutable();
//        int i = chunk.getPos().getStartX();
//        int j = chunk.getPos().getStartZ();
//        ChunkGeneratorSettings chunkGeneratorSettings = (ChunkGeneratorSettings) this.settings.get();
//        int k = chunkGeneratorSettings.getBedrockFloorY();
//        int l = this.worldHeight - 1 - chunkGeneratorSettings.getBedrockCeilingY();
//
//        boolean bl = l + 4 >= 0 && l < this.worldHeight;
//        boolean bl2 = k + 4 >= 0 && k < this.worldHeight;
//        if (bl || bl2) {
//            Iterator var12 = BlockPos.iterate(i, 0, j, i + 15, 0, j + 15).iterator();
//
//            while (true) {
//                BlockPos blockPos;
//                int o;
//                do {
//                    if (!var12.hasNext()) {
//                        return;
//                    }
//
//                    blockPos = (BlockPos) var12.next();
//                    if (bl) {
//                        for (o = 0; o < 5; ++o) {
//                            if (o <= random.nextInt(5)) {
//                                chunk.setBlockState(mutable.set(blockPos.getX(), l - o, blockPos.getZ()),
//                                    Blocks.OBSIDIAN.getDefaultState(), false);
//                            }
//                        }
//                    }
//                } while (!bl2);
//
//                for (o = 4; o >= 0; --o) {
//                    if (o <= random.nextInt(5)) {
//                        chunk.setBlockState(mutable.set(blockPos.getX(), k + o, blockPos.getZ()),
//                            Blocks.OBSIDIAN.getDefaultState(), false);
//                    }
//                }
//            }
//        }
//    }
    
}
