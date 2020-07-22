package com.qouteall.immersive_portals.mixin.dimension_sync;

import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RegistrySyncManager.class)
public class MixinFabricRegistrySyncManager {
//    @Inject(method = "apply", at = @At("HEAD"))
//    private static void onApply(
//        CompoundTag tag,
//        RemappableRegistry.RemapMode mode,
//        CallbackInfoReturnable<CompoundTag> cir
//    ) {
//        if (mode == RemappableRegistry.RemapMode.AUTHORITATIVE) {
//            DimensionIdRecord.onReadFabricRegistryServerSide(tag);
//        }
//    }
}
