package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

public interface IEBuiltChunk {
    void portal_fullyReset();
    
    long portal_getMark();
    
    void portal_setMark(long arg);
    
    void portal_setIndex(int arg);
    
    SectionRenderDispatcher.RenderSection portal_getDummyChunkInfo();
}
