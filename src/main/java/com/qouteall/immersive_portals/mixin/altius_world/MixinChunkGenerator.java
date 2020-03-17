package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusGeneratorType;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.LevelGeneratorType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
    @Shadow
    @Final
    protected IWorld world;
    private static ReentrantLock featureGenLock;
    
    //Vanilla feature generation is not thread safe
    @Inject(
        method = "generateFeatures",
        at = @At("HEAD")
    )
    private void onStartGeneratingFeatures(ChunkRegion region, CallbackInfo ci) {
        if (shouldLock()) {
            featureGenLock.lock();
        }
    }
    
    @Inject(
        method = "generateFeatures",
        at = @At("RETURN")
    )
    private void onEndGeneratingFeatures(ChunkRegion region, CallbackInfo ci) {
        if (shouldLock()) {
            featureGenLock.unlock();
        }
    }
    
    private boolean shouldLock() {
        LevelGeneratorType genType = world.getLevelProperties().getGeneratorType();
        return genType == AltiusGeneratorType.generatorType;
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
    
}
