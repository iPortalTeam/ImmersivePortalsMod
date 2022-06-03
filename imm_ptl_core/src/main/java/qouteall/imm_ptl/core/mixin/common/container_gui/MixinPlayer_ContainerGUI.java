package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.ducks.IEServerPlayerEntity;

@Mixin(Player.class)
public class MixinPlayer_ContainerGUI {
    
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;stillValid(Lnet/minecraft/world/entity/player/Player;)Z"
        )
    )
    private boolean redirectStillValid(AbstractContainerMenu instance, Player player) {
        boolean result1 = instance.stillValid(player);
        
        if (result1) {
            return true;
        }
        
        if (!(((Object) this) instanceof ServerPlayer)) {
            return false;
        }
        
        return ((IEServerPlayerEntity) this).ip_getRealIsContainerMenuValid(instance);
    }
}
