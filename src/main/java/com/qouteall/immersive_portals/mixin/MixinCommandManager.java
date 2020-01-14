package com.qouteall.immersive_portals.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.qouteall.immersive_portals.commands.MyCommandClient;
import com.qouteall.immersive_portals.commands.MyCommandServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<ServerCommandSource> dispatcher;
    
    @Inject(
        method = "Lnet/minecraft/server/command/CommandManager;<init>(Z)V",
        at = @At("RETURN")
    )
    private void initCommands(boolean isOnServer, CallbackInfo ci) {
        if (!isOnServer) {
            MyCommandClient.register(dispatcher);
        }
        MyCommandServer.register(dispatcher);
    }
    
}
