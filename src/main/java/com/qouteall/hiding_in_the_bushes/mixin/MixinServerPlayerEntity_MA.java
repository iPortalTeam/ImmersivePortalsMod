package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity_MA {
    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void onChangeDimensionByVanilla(
        ServerWorld serverWorld,
        CallbackInfoReturnable<Entity> cir
    ) {
        Global.chunkDataSyncManager.onPlayerRespawn((ServerPlayerEntity) (Object) this);
    }
    
    // update chunk visibility data
    @Inject(method = "teleport", at = @At("HEAD"))
    private void onTeleported(
        ServerWorld targetWorld,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        CallbackInfo ci
    ) {
        Global.chunkDataSyncManager.onPlayerRespawn(((ServerPlayerEntity)(Object) this));
    }
}
