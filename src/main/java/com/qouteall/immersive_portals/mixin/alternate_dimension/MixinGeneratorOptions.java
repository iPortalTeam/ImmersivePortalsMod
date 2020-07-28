package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import com.qouteall.immersive_portals.alternate_dimension.VoidChunkGenerator;
import com.qouteall.immersive_portals.ducks.IESimpleRegistry;
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

// Uses hacky ways to add dimension
// Should be replaced by better ways later
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
        
        if (Global.enableAlternateDimensions) {
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate1Option,
                () -> ModMain.surfaceTypeObject,
                NormalSkylandGenerator::new
            );
            
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate2Option,
                () -> ModMain.surfaceTypeObject,
                NormalSkylandGenerator::new
            );
            
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate3Option,
                () -> ModMain.surfaceTypeObject,
                ErrorTerrainGenerator::new
            );
            
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate4Option,
                () -> ModMain.surfaceTypeObject,
                ErrorTerrainGenerator::new
            );
    
            portal_addCustomDimension(
                seed,
                registry,
                ModMain.alternate5Option,
                () -> ModMain.surfaceTypeObject,
                (s) -> new VoidChunkGenerator()
            );
        }
        
        portal_recoverVanillaDimensions(seed, simpleRegistry);
    }
    
    // DFU may error with alternate dimensions and drop the nether and the end
    // Should be removed in later versions
    private void portal_recoverVanillaDimensions(
        long seed,
        SimpleRegistry<DimensionOptions> simpleRegistry
    ) {
        if (!simpleRegistry.containsId(DimensionOptions.NETHER.getValue())) {
            SimpleRegistry<DimensionOptions> dimensionOptions = DimensionType.method_28517(seed);
            
            simpleRegistry.add(
                DimensionOptions.NETHER,
                dimensionOptions.get(DimensionOptions.NETHER)
            );
            simpleRegistry.markLoaded(DimensionOptions.NETHER);
            
            Helper.err("Missing Nether. Recovered");
        }
        
        if (!simpleRegistry.containsId(DimensionOptions.END.getValue())) {
            SimpleRegistry<DimensionOptions> dimensionOptions = DimensionType.method_28517(seed);
            
            simpleRegistry.add(
                DimensionOptions.END,
                dimensionOptions.get(DimensionOptions.END)
            );
            simpleRegistry.markLoaded(DimensionOptions.END);
            
            Helper.err("Missing The End. Recovered");
        }
        
    }
    
    void portal_addCustomDimension(
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
        }
        //stop the dimension from being saved to level.dat
        //avoids messing with dfu
        ((IESimpleRegistry) registry).markUnloaded(key);
    }
}
