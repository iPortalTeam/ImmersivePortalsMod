package qouteall.imm_ptl.core.mixin.common.container_gui;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ContainerOpenersCounter.class)
public abstract class MixinContainerOpenersCounter {
    @Shadow
    protected abstract boolean isOwnContainer(Player player);
    
    // the container could be opened via portal. the player could be anywhere in any dimension
    // check all players
    @Inject(method = "getEntitiesWithContainerOpen", at = @At("HEAD"), cancellable = true)
    private void getOpenCount(Level level, BlockPos pos, CallbackInfoReturnable<List<ContainerUser>> cir) {
        List<ContainerUser> list = new ArrayList<>();
        
        MinecraftServer server = level.getServer();
        assert server != null;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isOwnContainer(player)) {
                list.add(player);
            }
        }
        
        cir.setReturnValue(list);
    }
}
