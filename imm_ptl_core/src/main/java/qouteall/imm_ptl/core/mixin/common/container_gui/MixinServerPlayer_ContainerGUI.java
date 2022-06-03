package qouteall.imm_ptl.core.mixin.common.container_gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.PehkuiInterface;
import qouteall.imm_ptl.core.commands.PortalCommand;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEServerPlayerEntity;
import qouteall.imm_ptl.core.portal.Portal;

@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayer_ContainerGUI extends Player implements IEServerPlayerEntity {
    public MixinServerPlayer_ContainerGUI(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
        throw new RuntimeException();
    }
    
    private boolean immptl_overrideDistance = false;
    
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
        
        return ip_getRealIsContainerMenuValid(instance);
    }
    
    // overrides vanilla method
    @Override
    public double distanceToSqr(double x, double y, double z) {
        if (immptl_overrideDistance) {
            return 1;
        }
        
        double d = this.getX() - x;
        double e = this.getY() - y;
        double f = this.getZ() - z;
        return d * d + e * e + f * f;
    }
    
    @Override
    public boolean ip_getRealIsContainerMenuValid(AbstractContainerMenu instance) {
        Portal pointingPortal = PortalCommand.getPlayerPointingPortal(
            ((ServerPlayer) (Object) this),
            true
        );
        
        if (pointingPortal == null) {
            return false;
        }
        
        // make distanceToSqr to return 1
        // this will work properly when the block is destroyed or when the portal has scaling
        immptl_overrideDistance = true;
        boolean newResult = instance.stillValid(((ServerPlayer) (Object) this));
        immptl_overrideDistance = false;
        
        return newResult;
    }
}
