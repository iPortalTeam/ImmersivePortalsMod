package qouteall.imm_ptl.core.ducks;

public interface IEBuiltChunk {
    void fullyReset();
    
    long getMark();
    
    void setMark(long arg);
    
    void setIndex(int arg);
}
