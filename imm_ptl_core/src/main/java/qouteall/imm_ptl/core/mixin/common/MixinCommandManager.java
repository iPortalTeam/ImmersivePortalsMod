package qouteall.imm_ptl.core.mixin.common;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.commands.PortalCommand;

@Mixin(Commands.class)
public class MixinCommandManager {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void initCommands(Commands.CommandSelection environment, CallbackInfo ci) {
        PortalCommand.register(dispatcher);
    }
    
}
