package qouteall.imm_ptl.core.platform_specific.mixin.common;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinDebugClientPlayNetworkHandler {
//    @Shadow
//    private ClientWorld world;
//
//    @Inject(
//        method = "updateLighting",
//        at = @At("HEAD")
//    )
//    private void onUpdateLighting(
//        int chunkX, int chunkZ, LightingProvider provider, LightType type,
//        int mask, int filledMask, Iterator<byte[]> updates, boolean bl, CallbackInfo ci
//    ) {
//        if (Global.lightLogging) {
//            if (type == LightType.BLOCK) {
//                Helper.log(String.format(
//                    "light received %s %d %d %d %d", world.getRegistryKey().getValue(),
//                    chunkX, chunkZ, mask, filledMask
//                ));
//            }
//        }
//    }
}
