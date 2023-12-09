package qouteall.imm_ptl.core.mixin.common.collision;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.teleportation.ServerTeleportationManager;
import qouteall.q_misc_util.Helper;

@Mixin(ThrownEnderpearl.class)
public class MixinThrownEnderPearl {
    @Inject(
        method = "onHit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ThrownEnderpearl;discard()V"
        )
    )
    private void onOnHitDiscard(HitResult result, CallbackInfo ci) {
        ThrownEnderpearl this_ = (ThrownEnderpearl) (Object) this;
        
        Entity owner = this_.getOwner();
        
        if (owner instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.connection.isAcceptingMessages()
                && serverPlayer.level() != this_.level()
                && !serverPlayer.isSleeping()
            ) {
                Helper.log("Doing cross-dimensional ender pearl teleportation");
                ServerTeleportationManager.teleportEntityGeneral(
                    serverPlayer,
                    this_.position(),
                    ((ServerLevel) this_.level())
                );
            }
        }
    }
}
