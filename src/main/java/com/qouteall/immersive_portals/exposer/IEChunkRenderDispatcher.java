package com.qouteall.immersive_portals.exposer;

public interface IEChunkRenderDispatcher {
    void tick();
    
    int getEmployedRendererNum();
    
    void rebuildAll();
}
