package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalLong;

@Mixin(DimensionType.class)
public class MixinDimensionType {
    
    @Invoker("<init>")
    static DimensionType constructor(
        OptionalLong fixedTime,
        boolean hasSkylight,
        boolean hasCeiling,
        boolean ultrawarm,
        boolean natural,
        boolean shrunk,
        boolean piglinSafe,
        boolean bedWorks,
        boolean respawnAnchorWorks,
        boolean hasRaids,
        int logicalHeight,
        Identifier infiniburn,
        float ambientLight
    ) {
        return null;
    }
    
    @Inject(
        method = "method_28517",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onInitDimensionOptions(
        long seed,
        CallbackInfoReturnable<SimpleRegistry<DimensionOptions>> cir
    ) {
        SimpleRegistry<DimensionOptions> registry = cir.getReturnValue();
    
        registry.add(
            ModMain.alternate1Option,
            new DimensionOptions(
                () -> ModMain.surfaceTypeObject,
                new NormalSkylandGenerator(seed)
            )
        );
        registry.markLoaded(ModMain.alternate1Option);
        
        registry.add(
            ModMain.alternate2Option,
            new DimensionOptions(
                () -> ModMain.surfaceTypeObject,
                new NormalSkylandGenerator(seed)
            )
        );
        registry.markLoaded(ModMain.alternate2Option);
    
        registry.add(
            ModMain.alternate3Option,
            new DimensionOptions(
                () -> ModMain.surfaceTypeObject,
                new ErrorTerrainGenerator(seed)
            )
        );
        registry.markLoaded(ModMain.alternate3Option);
        
        registry.add(
            ModMain.alternate4Option,
            new DimensionOptions(
                () -> ModMain.surfaceTypeObject,
                new ErrorTerrainGenerator(seed)
            )
        );
        registry.markLoaded(ModMain.alternate4Option);
    }
    
    @Inject(
        method = "addRegistryDefaults",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onAddRegistryDefaults(
        RegistryTracker.Modifiable registryTracker,
        CallbackInfoReturnable<RegistryTracker.Modifiable> cir
    ) {
        registryTracker.addDimensionType(
            ModMain.surfaceType,
            ModMain.surfaceTypeObject
        );
    }
    
    static {
        ModMain.surfaceTypeObject = constructor(
            OptionalLong.empty(),
            true,
            false,
            false,
            true,
            false,
            false,
            true,
            true,
            true,
            256,
            BlockTags.INFINIBURN_OVERWORLD.getId(),
            0
        );
    }
}
