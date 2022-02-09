package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;

@Mixin(ServerboundMovePlayerPacket.class)
public class MixinServerboundMovePlayerPacket_S implements IEPlayerMoveC2SPacket {
    private ResourceKey<Level> playerDimension;
    
    @Override
    public ResourceKey<Level> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(ResourceKey<Level> dim) {
        playerDimension = dim;
    }
    
    
}
