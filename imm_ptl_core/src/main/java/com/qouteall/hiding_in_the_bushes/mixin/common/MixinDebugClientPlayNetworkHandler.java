package com.qouteall.hiding_in_the_bushes.mixin.common;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;

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
