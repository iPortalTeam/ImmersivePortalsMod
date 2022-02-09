package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManagement;

@Mixin(PlayerList.class)
public class MixinPlayerList_P {
    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;reloadResources()V",
        at = @At("RETURN")
    )
    private void onOnDatapackReloaded(CallbackInfo ci) {
        CustomPortalGenManagement.onDatapackReload();
    }
}
