package com.qouteall.hiding_in_the_bushes.mixin.altius_world;

import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    //vanilla feature generation is not thread safe
    
    private static ReentrantLock featureGenLock;
    
    @Redirect(
        method = "method_12151",//method_12151
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;generateFeatures(Lnet/minecraft/world/ChunkRegion;)V"
        )
    )
    private static void redirectGenerateFeatures(ChunkGenerator chunkGenerator, ChunkRegion chunkRegion) {
        featureGenLock.lock();
        chunkGenerator.generateFeatures(chunkRegion);
        featureGenLock.unlock();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
}
