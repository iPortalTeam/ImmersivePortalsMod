package com.qouteall.hiding_in_the_bushes.mixin.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import net.minecraft.structure.StructureManager;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
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
            featureGenLock.lock();
        }
        try {
            chunkGenerator.generateFeatures(region,accessor);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s %d %d",
                region.getWorld().getRegistryKey(),
                region.getCenterChunkX(),
                region.getCenterChunkZ()
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;setStructureStarts(Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;Lnet/minecraft/structure/StructureManager;J)V"
        )
    )
    private static void redirectSetStructureStarts(
        ChunkGenerator generator,
        StructureAccessor structureAccessor, Chunk chunk, StructureManager structureManager, long l
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            generator.setStructureStarts(structureAccessor, chunk, structureManager, l);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/chunk/ChunkGenerator;addStructureReferences(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/chunk/Chunk;)V"
        )
    )
    private static void redirectAddStructureReference(
        ChunkGenerator chunkGenerator, WorldAccess world, StructureAccessor structureAccessor, Chunk chunk
    ) {
        boolean shouldLock = getShouldLock();
        if (shouldLock) {
            featureGenLock.lock();
        }
        try {
            chunkGenerator.addStructureReferences(world, structureAccessor, chunk);
        }
        catch (Throwable e) {
            Helper.err(String.format(
                "Error when generating terrain %s",
                chunk
            ));
            e.printStackTrace();
        }
        if (shouldLock) {
            featureGenLock.unlock();
        }
    }
    
    private static boolean getShouldLock() {
        return AltiusInfo.isAltius();
    }
    
    static {
        featureGenLock = new ReentrantLock(true);
    }
}
