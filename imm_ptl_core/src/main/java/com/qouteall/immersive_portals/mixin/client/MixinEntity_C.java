package com.qouteall.immersive_portals.mixin.client;

import com.qouteall.immersive_portals.render.context_management.RenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public class MixinEntity_C {
    // avoid invisible armor stands to be visible through portal
    @Redirect(
        method = "isInvisibleTo",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;isSpectator()Z"
        )
    )
    private boolean redirectIsSpectator(PlayerEntity playerEntity) {
        if (RenderInfo.isRendering()) {
            return false;
        }
        return playerEntity.isSpectator();
    }
}
