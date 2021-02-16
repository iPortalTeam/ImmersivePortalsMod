package com.qouteall.hiding_in_the_bushes.mixin.common;

import com.qouteall.immersive_portals.commands.PortalCommand;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "virtuoel.pehkui.server.command.ScaleCommand")
public class MixinPehkuiScaleCommand {
    @Redirect(
        method = "*",//lambda
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/command/ServerCommandSource;hasPermissionLevel(I)Z"
        )
    )
    static boolean redirectHasPermissionLevel(ServerCommandSource serverCommandSource) {
        return PortalCommand.canUsePortalCommand(serverCommandSource);
    }
}
