package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;

@Mixin(PlayerMoveC2SPacket.class)
public class MixinPlayerMoveC2SPacket_S implements IEPlayerMoveC2SPacket {
    private RegistryKey<World> playerDimension;
    
    @Override
    public RegistryKey<World> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(RegistryKey<World> dim) {
        playerDimension = dim;
    }
    
    
}
