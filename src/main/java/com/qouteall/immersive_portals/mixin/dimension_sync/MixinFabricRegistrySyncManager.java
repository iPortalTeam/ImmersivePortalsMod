package com.qouteall.immersive_portals.mixin.dimension_sync;

import com.qouteall.immersive_portals.dimension_sync.DimensionIdRecord;
import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.fabricmc.fabric.impl.registry.sync.RemappableRegistry;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
