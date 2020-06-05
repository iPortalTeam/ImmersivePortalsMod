package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.chunk.SurfaceChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Random;

@Mixin(SurfaceChunkGenerator.class)
public abstract class MixinSurfaceChunkGenerator<T extends ChunkGeneratorConfig> extends ChunkGenerator {
    
    
    @Shadow @Final private int bedrockCeilingHeight;
    
    @Shadow @Final private int bedrockFloorHeight;
    
    public MixinSurfaceChunkGenerator(
        BiomeSource biomeSource,
        ChunkGeneratorConfig config
    ) {
        super(biomeSource, config);
    }
    
    @Inject(
        method = "buildBedrock",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onBuildBedrock(Chunk chunk, Random random, CallbackInfo ci) {
        if (AltiusInfo.isAltius()) {
            buildAltiusBedrock(chunk, random);
            ci.cancel();
        }
    }
    
    private void buildAltiusBedrock(Chunk chunk, Random random) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int i = chunk.getPos().getStartX();
        int j = chunk.getPos().getStartZ();
        int k = this.bedrockFloorHeight;
        int l = this.bedrockCeilingHeight;
        Iterator var8 = BlockPos.iterate(i, 0, j, i + 15, 0, j + 15).iterator();
    
        while(true) {
            BlockPos blockPos;
            int n;
            do {
                if (!var8.hasNext()) {
                    return;
                }
            
                blockPos = (BlockPos)var8.next();
                if (l > 0) {
                    for(n = l; n >= l - 4; --n) {
                        if (n >= l - random.nextInt(5)) {
                            chunk.setBlockState(mutable.set(blockPos.getX(), n, blockPos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
                        }
                    }
                }
            } while(k >= 256);
        
            for(n = k + 4; n >= k; --n) {
                if (n <= k + random.nextInt(5)) {
                    chunk.setBlockState(mutable.set(blockPos.getX(), n, blockPos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
                }
            }
        }
    }
    
}
