package com.qouteall.immersive_portals.mixin.alternate_dimension;

import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DimensionType.class)
public class MixinDimensionType {
//
//    @Invoker("<init>")
//    static DimensionType constructor(
//    ) {
//        return null;
//    }
//
//
//    @Inject(
//        method = "addRegistryDefaults",
//        at = @At("RETURN"),
//        cancellable = true
//    )
//    private static void onAddRegistryDefaults(
//        RegistryTracker.Modifiable registryTracker,
//        CallbackInfoReturnable<RegistryTracker.Modifiable> cir
//    ) {
//        registryTracker.addDimensionType(
//            ModMain.surfaceType,
//            ModMain.surfaceTypeObject
//        );
//    }
//
//    static {
//        ModMain.surfaceTypeObject = constructor(
//            OptionalLong.empty(),
//            true,
//            false,
//            false,
//            true,
//            false,
//            false,
//            true,
//            true,
//            true,
//            256,
//            BlockTags.INFINIBURN_OVERWORLD.getId(),
//            0
//        );
//    }
}
