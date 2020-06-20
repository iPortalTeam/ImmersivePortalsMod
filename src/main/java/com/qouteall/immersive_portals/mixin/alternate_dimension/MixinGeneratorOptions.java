package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(GeneratorOptions.class)
public class MixinGeneratorOptions {
    @Inject(
        method = "<init>(JZZLnet/minecraft/util/registry/SimpleRegistry;Ljava/util/Optional;)V",
        at = @At("RETURN")
    )
    private void onInitEnded(
        long seed,
        boolean generateStructures,
        boolean bonusChest,
        SimpleRegistry<DimensionOptions> simpleRegistry,
        Optional<String> legacyCustomOptions,
        CallbackInfo ci
    ) {
        SimpleRegistry<DimensionOptions> registry = simpleRegistry;
        
        portal_addIfMissing(
            seed,
            registry,
            ModMain.alternate1Option,
            () -> ModMain.surfaceTypeObject,
            NormalSkylandGenerator::new
        );
        
        portal_addIfMissing(
            seed,
            registry,
            ModMain.alternate2Option,
            () -> ModMain.surfaceTypeObject,
            NormalSkylandGenerator::new
        );
        
        portal_addIfMissing(
            seed,
            registry,
            ModMain.alternate3Option,
            () -> ModMain.surfaceTypeObject,
            ErrorTerrainGenerator::new
        );
        
        portal_addIfMissing(
            seed,
            registry,
            ModMain.alternate4Option,
            () -> ModMain.surfaceTypeObject,
            ErrorTerrainGenerator::new
        );
    }
    
    void portal_addIfMissing(
        long argSeed,
        SimpleRegistry<DimensionOptions> registry,
        RegistryKey<DimensionOptions> key,
        Supplier<DimensionType> dimensionTypeSupplier,
        Function<Long, ChunkGenerator> chunkGeneratorCreator
    ) {
        if (!registry.containsId(key.getValue())) {
            registry.add(
                key,
                new DimensionOptions(
                    dimensionTypeSupplier,
                    chunkGeneratorCreator.apply(argSeed)
                )
            );
            registry.markLoaded(key);
        }
    }
}
