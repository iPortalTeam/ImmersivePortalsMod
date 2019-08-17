package com.qouteall.immersive_portals.exposer;

public interface IEGlFrameBuffer {
    boolean getIsStencilBufferEnabled();
    
    void setIsStencilBufferEnabledAndReload(boolean cond);
}
