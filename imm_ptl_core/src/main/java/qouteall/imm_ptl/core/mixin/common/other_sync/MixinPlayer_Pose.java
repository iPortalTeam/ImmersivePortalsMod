package qouteall.imm_ptl.core.mixin.common.other_sync;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEEntity;

@Mixin(Player.class)
public class MixinPlayer_Pose {
    // on the server side, the player's portal collision status is not accurate
    // so make it to not update on server side when the player is colliding with portal
    @Inject(
        method = "updatePlayerPose",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onUpdatePlayerPose(CallbackInfo ci) {
        Player this_ = (Player) (Object) this;
        if (!this_.level().isClientSide()) {
            if (((IEEntity) this_).ip_isRecentlyCollidingWithPortal()) {
                ci.cancel();
            }
        }
    }
    
}
