package com.qouteall.immersive_portals.ducks;

public interface IEFrameBuffer {
    boolean getIsStencilBufferEnabled();
    
    void setIsStencilBufferEnabledAndReload(boolean cond);
}
