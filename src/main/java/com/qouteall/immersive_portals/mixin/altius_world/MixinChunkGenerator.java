package com.qouteall.immersive_portals.mixin.altius_world;

import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkGenerator.class)
public class MixinChunkGenerator {
//    @Shadow
//    @Final
//    protected IWorld world;
//    private static ReentrantLock featureGenLock;
//
//    //Vanilla feature generation is not thread safe
//    @Inject(
//        method = "generateFeatures",
//        at = @At("HEAD")
//    )
//    private void onStartGeneratingFeatures(ChunkRegion region, CallbackInfo ci) {
//        if (shouldLock()) {
//            featureGenLock.lock();
//        }
//    }
//
//    @Inject(
//        method = "generateFeatures",
//        at = @At("RETURN")
//    )
//    private void onEndGeneratingFeatures(ChunkRegion region, CallbackInfo ci) {
//        if (shouldLock()) {
//            featureGenLock.unlock();
//        }
//    }
//
//    private boolean shouldLock() {
//        return AltiusInfo.isAltius();
//    }
//
//    static {
//        featureGenLock = new ReentrantLock(true);
//    }
    
}
