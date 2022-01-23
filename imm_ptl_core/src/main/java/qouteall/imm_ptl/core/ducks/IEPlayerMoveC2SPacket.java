package qouteall.imm_ptl.core.ducks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IEPlayerMoveC2SPacket {
    ResourceKey<Level> getPlayerDimension();
    
    void setPlayerDimension(ResourceKey<Level> dim);
}
