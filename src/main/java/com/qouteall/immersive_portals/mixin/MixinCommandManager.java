package com.qouteall.immersive_portals.mixin;

import net.minecraft.server.command.CommandManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class MixinCommandManager {
    @Inject(
        method = "Lnet/minecraft/server/command/CommandManager;<init>(Z)V",
        at = @At("RETURN")
    )
    static private void initCommands(boolean isOnServer, CallbackInfo ci) {
    
    }
}
