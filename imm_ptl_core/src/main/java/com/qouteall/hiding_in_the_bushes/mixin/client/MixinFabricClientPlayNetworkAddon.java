package com.qouteall.hiding_in_the_bushes.mixin.client;

import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayNetworkAddon.class)
public class MixinFabricClientPlayNetworkAddon {
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
