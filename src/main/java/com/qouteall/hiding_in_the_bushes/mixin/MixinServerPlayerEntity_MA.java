package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity_MA {
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void onChangeDimensionByVanilla(
        RegistryKey<World> dimensionType_1,
        CallbackInfoReturnable<Entity> cir
    ) {
        Global.chunkDataSyncManager.onPlayerRespawn((ServerPlayerEntity) (Object) this);
    }
}
