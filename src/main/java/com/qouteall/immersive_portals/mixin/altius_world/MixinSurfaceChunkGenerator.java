package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.class_5311;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorType;
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
public abstract class MixinSurfaceChunkGenerator extends ChunkGenerator {
    
    
    @Shadow @Final protected ChunkGeneratorType field_24774;
    
    @Shadow @Final private int field_24779;
    
    public MixinSurfaceChunkGenerator(BiomeSource biomeSource, class_5311 arg) {
        super(biomeSource, arg);
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
        int k = this.field_24774.getBedrockFloorY();
        int l = this.field_24779 - 1 - this.field_24774.getBedrockCeilingY();
        boolean bl = l + 4 >= 0 && l < this.field_24779;
        boolean bl2 = k + 4 >= 0 && k < this.field_24779;
        if (bl || bl2) {
            Iterator var11 = BlockPos.iterate(i, 0, j, i + 15, 0, j + 15).iterator();
        
            while(true) {
                BlockPos blockPos;
                int o;
                do {
                    if (!var11.hasNext()) {
                        return;
                    }
                
                    blockPos = (BlockPos)var11.next();
                    if (bl) {
                        for(o = 0; o < 5; ++o) {
                            if (o <= random.nextInt(5)) {
                                chunk.setBlockState(mutable.set(blockPos.getX(), l - o, blockPos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
                            }
                        }
                    }
                } while(!bl2);
            
                for(o = 4; o >= 0; --o) {
                    if (o <= random.nextInt(5)) {
                        chunk.setBlockState(mutable.set(blockPos.getX(), k + o, blockPos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
                    }
                }
            }
        }
    }
    
}
