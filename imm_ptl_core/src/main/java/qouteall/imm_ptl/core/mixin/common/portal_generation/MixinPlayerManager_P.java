package qouteall.imm_ptl.core.mixin.common.portal_generation;

import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class MixinPlayerManager_P {
    @Inject(
        method = "onDataPacksReloaded",
        at = @At("RETURN")
    )
    private void onOnDatapackReloaded(CallbackInfo ci) {
        CustomPortalGenManagement.onDatapackReload();
    }
}
