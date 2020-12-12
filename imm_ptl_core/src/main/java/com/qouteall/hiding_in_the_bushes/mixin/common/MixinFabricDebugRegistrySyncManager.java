package com.qouteall.hiding_in_the_bushes.mixin.common;

import net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(RegistrySyncManager.class)
public class MixinFabricDebugRegistrySyncManager {
    @Inject(method = "receivePacket", at = @At("HEAD"), cancellable = true)
    private static void onReceivePacket(
        ThreadExecutor<?> executor, PacketByteBuf buf, boolean accept,
        Consumer<Exception> errorHandler, CallbackInfo ci
    ) {
        ci.cancel();
    }
}
