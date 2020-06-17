package com.qouteall.immersive_portals.mixin.alternate_dimension;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.NormalSkylandGenerator;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public class MixinDimensionType {
    
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
            ModMain.alternate2Option,
            new DimensionOptions(
                () -> {
                    RegistryTracker.Modifiable dimensionTracker =
                        ((IEMinecraftServer) McHelper.getServer()).portal_getDimensionTracker();

                    return dimensionTracker.getDimensionTypeRegistry().get(ModMain.surfaceType);
                },
                new NormalSkylandGenerator(seed)
            )
        );
        registry.markLoaded(ModMain.alternate2Option);

        registry.add(
            ModMain.alternate4Option,
            new DimensionOptions(
                () -> {
                    RegistryTracker.Modifiable dimensionTracker =
                        ((IEMinecraftServer) McHelper.getServer()).portal_getDimensionTracker();

                    return dimensionTracker.getDimensionTypeRegistry().get(ModMain.surfaceType);
                },
                new ErrorTerrainGenerator(seed)
            )
        );
        registry.markLoaded(ModMain.alternate4Option);
    }
    
}
