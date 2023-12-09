package qouteall.imm_ptl.core.ducks;

public interface IEFrameBuffer {
    boolean ip_getIsStencilBufferEnabled();
    
    void ip_setIsStencilBufferEnabledAndReload(boolean cond);
}
