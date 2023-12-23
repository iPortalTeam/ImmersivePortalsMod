package qouteall.imm_ptl.core.mixin.common.portal_generation;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer_P {
//    @Inject(method = "Lnet/minecraft/server/MinecraftServer;loadLevel()V", at = @At("RETURN"))
//    private void onLoadWorldFinished(CallbackInfo ci) {
//        CustomPortalGenManager.onDatapackReload();
//    }
}
