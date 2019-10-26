package com.qouteall.immersive_portals.ducks;

public interface IEChunkRenderDispatcher {
    void tick();
    
    int getEmployedRendererNum();
    
    void rebuildAll();
}
