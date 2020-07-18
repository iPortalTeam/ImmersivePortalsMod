package com.qouteall.immersive_portals.ducks;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public interface IEPlayerMoveC2SPacket {
    RegistryKey<World> getPlayerDimension();
    
    void setPlayerDimension(RegistryKey<World> dim);
}
