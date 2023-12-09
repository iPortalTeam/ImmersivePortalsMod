package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPMcHelper;

@Mixin(ContainerOpenersCounter.class)
public abstract class MixinContainerOpenersCounter {
    @Shadow
    protected abstract boolean isOwnContainer(Player player);
    
    @Inject(method = "getOpenCount", at = @At("HEAD"), cancellable = true)
    private void getOpenCount(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // fixing this issue on only server side seems enough
        if (level instanceof ServerLevel serverWorld) {
            // if found any nearby portal, check all players instead of near players
            boolean noPortalNearby = IPMcHelper.getNearbyPortalList(
                level, Vec3.atCenterOf(pos), 32, p -> true
            ).isEmpty();
            
            if (!noPortalNearby) {
                cir.setReturnValue(ip_getOpenCountCheckingAllPlayer(serverWorld, pos));
            }
        }
    }
    
    private int ip_getOpenCountCheckingAllPlayer(ServerLevel serverWorld, BlockPos pos) {
        int count = 0;
        for (ServerPlayer player : serverWorld.getServer().getPlayerList().getPlayers()) {
            if (isOwnContainer(player)) {
                count++;
            }
        }
        return count;
    }
}
