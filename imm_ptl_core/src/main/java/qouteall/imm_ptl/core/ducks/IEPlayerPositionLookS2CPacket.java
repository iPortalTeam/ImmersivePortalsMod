package qouteall.imm_ptl.core.ducks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IEPlayerPositionLookS2CPacket {
    ResourceKey<Level> ip_getPlayerDimension();
    
    void ip_setPlayerDimension(ResourceKey<Level> dimension);
}
