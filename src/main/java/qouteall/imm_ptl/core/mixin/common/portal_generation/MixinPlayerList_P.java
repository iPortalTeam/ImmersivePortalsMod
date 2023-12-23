package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerList.class)
public class MixinPlayerList_P {
//    @Inject(
//        method = "Lnet/minecraft/server/players/PlayerList;reloadResources()V",
//        at = @At("RETURN")
//    )
//    private void onOnDatapackReloaded(CallbackInfo ci) {
//        CustomPortalGenManager.onDatapackReload();
//    }
}
