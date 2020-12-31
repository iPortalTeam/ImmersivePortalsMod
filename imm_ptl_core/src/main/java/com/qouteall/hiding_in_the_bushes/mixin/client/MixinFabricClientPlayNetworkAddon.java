package com.qouteall.hiding_in_the_bushes.mixin.client;

import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayNetworkAddon.class)
public class MixinFabricClientPlayNetworkAddon {
    // The new Fabric API ignores all mod packets on render thread
    // but this mod's packet redirect hack requires handling portal spawn packet on render thread
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;isOnThread()Z"
        )
    )
    private boolean redirectIsOnThread(MinecraftClient minecraftClient) {
        return false;
    }
}
