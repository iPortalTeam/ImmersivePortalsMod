package qouteall.imm_ptl.peripheral.dim_stack;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.peripheral.alternate_dimension.AlternateDimensions;
import qouteall.imm_ptl.peripheral.guide.IPOuterClientMisc;

import javax.annotation.Nullable;
import java.util.Map;

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
