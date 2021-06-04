package qouteall.imm_ptl.core.ducks;

public interface IEFrameBuffer {
    boolean getIsStencilBufferEnabled();
    
    void setIsStencilBufferEnabledAndReload(boolean cond);
}
