package com.qouteall.immersive_portals.mixin.common.portal_generation;

import com.qouteall.immersive_portals.portal.custom_portal_gen.CustomPortalGenManagement;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_P {
    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void onLoadWorldFinished(CallbackInfo ci) {
        CustomPortalGenManagement.onDatapackReload();
    }
}
