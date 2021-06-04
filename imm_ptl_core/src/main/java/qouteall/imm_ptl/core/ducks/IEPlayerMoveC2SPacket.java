package qouteall.imm_ptl.core.ducks;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public interface IEPlayerMoveC2SPacket {
    RegistryKey<World> getPlayerDimension();
    
    void setPlayerDimension(RegistryKey<World> dim);
}
