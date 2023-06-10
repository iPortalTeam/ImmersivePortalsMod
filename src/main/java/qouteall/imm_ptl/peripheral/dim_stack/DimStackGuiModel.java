package qouteall.imm_ptl.peripheral.dim_stack;

import javax.annotation.Nullable;

public class DimStackGuiModel {
    public boolean isEnabled = false;
    public final DimStackInfo dimStackInfo = new DimStackInfo();
    
    @Nullable
    public DimStackInfo getResult() {
        if (!isEnabled) {
            return null;
        }
        return dimStackInfo;
    }
}
