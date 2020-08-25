package com.qouteall.hiding_in_the_bushes.mixin;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
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
    @Inject(method = "moveToWorld", at = @At("HEAD"))
    private void onChangeDimensionByVanilla(
        ServerWorld serverWorld,
        CallbackInfoReturnable<Entity> cir
    ) {
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        onBeforeTravel(this_);
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
        ServerPlayerEntity this_ = (ServerPlayerEntity) (Object) this;
        
        if (this_.world != targetWorld) {
            onBeforeTravel(this_);
        }
    }
    
    private static void onBeforeTravel(ServerPlayerEntity this_) {
        CustomPortalGenManagement.onBeforeConventionalDimensionChange(this_);
        Global.chunkDataSyncManager.onPlayerRespawn(this_);
        
        ModMain.serverTaskList.addTask(() -> {
            CustomPortalGenManagement.onAfterConventionalDimensionChange(this_);
            return true;
        });
    }
}
