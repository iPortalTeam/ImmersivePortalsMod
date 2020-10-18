package com.qouteall.imm_ptl_peripheral.mixin.common.altius_world;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    //vanilla feature generation is not thread safe
    
    private static ReentrantLock featureGenLock;
    
    private static void lockFeatureGen() {
        try {
            featureGenLock.tryLock(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private static void unlockFeatureGen() {
        if (featureGenLock.isHeldByCurrentThread()) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;generateFeatures(Lnet/minecraft/world/ChunkRegion;Lnet/minecraft/world/gen/StructureAccessor;)V"
        )
    )
    private static void redirectGenerateFeatures(
        ChunkGenerator chunkGenerator, ChunkRegion region, StructureAccessor accessor
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            lockFeatureGen();
        }
        try {
            chunkGenerator.generateFeatures(region, accessor);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s %d %d",
                ((ServerWorld) region.getChunkManager().getWorld()).getRegistryKey(),
                region.getCenterChunkX(),
                region.getCenterChunkZ()
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            unlockFeatureGen();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;setStructureStarts(Lnet/minecraft/util/registry/DynamicRegistryManager;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/structure/StructureManager;J)V"
        )
    )
    private static void redirectSetStructureStarts(
        ChunkGenerator generator,
        DynamicRegistryManager dynamicRegistryManager, StructureAccessor structureAccessor, Chunk chunk, StructureManager structureManager, long worldSeed
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            lockFeatureGen();
        }
        try {
            generator.setStructureStarts(dynamicRegistryManager, structureAccessor, chunk, structureManager, worldSeed);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            unlockFeatureGen();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;addStructureReferences(Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V"
        )
    )
    private static void redirectAddStructureReference(
        ChunkGenerator chunkGenerator,
        StructureWorldAccess structureWorldAccess, StructureAccessor accessor, Chunk chunk
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            lockFeatureGen();
        }
        try {
            chunkGenerator.addStructureReferences(structureWorldAccess, accessor, chunk);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            unlockFeatureGen();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;carve(JLnet/minecraft/world/biome/source/BiomeAccess;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/world/gen/GenerationStep$Carver;)V"
        )
    )
    private static void redirectCarve(
        ChunkGenerator generator, long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            lockFeatureGen();
        }
        try {
            generator.carve(seed, access, chunk, carver);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            unlockFeatureGen();
        }
    }
    
    // seems that the feature generation is thread safe now
    // no need to lock
    private static boolean getShouldLock() {
        return false;
//        return AltiusGameRule.getIsDimensionStackCache();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
}
