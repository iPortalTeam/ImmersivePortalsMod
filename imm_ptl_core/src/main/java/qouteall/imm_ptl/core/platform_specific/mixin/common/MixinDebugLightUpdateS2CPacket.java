package qouteall.imm_ptl.core.platform_specific.mixin.common;

import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientboundLightUpdatePacket.class)
public class MixinDebugLightUpdateS2CPacket {
//    @Shadow
//    private int filledBlockLightMask;
//
//    @Shadow private int blockLightMask;
    
//    //debug
//    @Inject(
//        method = "<init>(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/light/LightingProvider;Z)V",
//        at = @At("RETURN")
//    )
//    private void onInited(ChunkPos chunkPos, LightingProvider lightingProvider, boolean bl, CallbackInfo ci) {
//        Helper.log("light sent" + chunkPos.toString() + blockLightMask+lightingProvider);
//    }
}
