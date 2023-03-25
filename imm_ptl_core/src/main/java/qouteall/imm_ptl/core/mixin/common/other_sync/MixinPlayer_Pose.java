package qouteall.imm_ptl.core.mixin.common.other_sync;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.api.McRemoteProcedureCall;

@Mixin(Player.class)
public class MixinPlayer_Pose {
    // only update player pose on client side
    @Inject(
        method = "updatePlayerPose",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUpdatePlayerPose(CallbackInfo ci) {
        Player this_ = (Player) (Object) this;
        if (!this_.level.isClientSide()) {
            ci.cancel();
        }
    }
    
    // as the pose is not being updated on server side, send packets to sync to server
    @Redirect(
        method = "updatePlayerPose",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;setPose(Lnet/minecraft/world/entity/Pose;)V"
        )
    )
    private void redirectSetPose(Player instance, Pose pose) {
        if (instance.getPose() != pose) {
            instance.setPose(pose);
            McRemoteProcedureCall.tellServerToInvoke(
                "qouteall.imm_ptl.core.network.IPNetworking.RemoteCallables.onClientPlayerUpdatePose",
                pose
            );
        }
    }
}
