package com.qouteall.hiding_in_the_bushes.mixin.common;

import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightUpdateS2CPacket.class)
public class MixinDebugLightUpdateS2CPacket {
    @Shadow
    private int filledBlockLightMask;
    
    @Shadow private int blockLightMask;
    
//    //debug
//    @Inject(
//        method = "<init>(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/chunk/light/LightingProvider;Z)V",
//        at = @At("RETURN")
//    )
//    private void onInited(ChunkPos chunkPos, LightingProvider lightingProvider, boolean bl, CallbackInfo ci) {
//        Helper.log("light sent" + chunkPos.toString() + blockLightMask+lightingProvider);
//    }
}
