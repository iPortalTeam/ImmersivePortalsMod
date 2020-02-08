package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    //use portal culled collision box
    @Redirect(
        method = "cannotFitAt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;getBoundingBox()Lnet/minecraft/util/math/Box;"
        )
    )
    private Box redirectGetBoundingBox(ClientPlayerEntity clientPlayerEntity) {
        return CollisionHelper.getActiveCollisionBox(clientPlayerEntity);
    }
}
