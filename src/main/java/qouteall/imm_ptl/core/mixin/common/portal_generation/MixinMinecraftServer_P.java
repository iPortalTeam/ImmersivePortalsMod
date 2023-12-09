package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.portal.custom_portal_gen.CustomPortalGenManagement;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_P {
    @Inject(method = "Lnet/minecraft/server/MinecraftServer;loadLevel()V", at = @At("RETURN"))
    private void onLoadWorldFinished(CallbackInfo ci) {
        CustomPortalGenManagement.onDatapackReload();
    }
}
