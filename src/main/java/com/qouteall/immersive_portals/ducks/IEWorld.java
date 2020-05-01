package com.qouteall.immersive_portals.ducks;

import net.minecraft.class_5269;
import net.minecraft.world.chunk.ChunkManager;

public interface IEWorld {
    void setChunkManager(ChunkManager manager);
    
    class_5269 myGetProperties();
}
