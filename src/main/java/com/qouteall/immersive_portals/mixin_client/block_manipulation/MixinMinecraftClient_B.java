package com.qouteall.immersive_portals.mixin_client.block_manipulation;

import com.qouteall.immersive_portals.BlockManipulationClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient_B {
    @Inject(
        method = "handleBlockBreaking",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onHandleBlockBreaking(boolean isKeyPressed, CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToRemoteBlock()) {
            BlockManipulationClient.myHandleBlockBreaking(isKeyPressed);
            ci.cancel();
        }
    }
    
    @Inject(
        method = "doAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/hit/HitResult;getType()Lnet/minecraft/util/hit/HitResult$Type;"
        ),
        cancellable = true
    )
    private void onDoAttack(CallbackInfo ci) {
        if (BlockManipulationClient.isPointingToRemoteBlock()) {
            BlockManipulationClient.myAttackBlock();
            ci.cancel();
        }
    }
}
