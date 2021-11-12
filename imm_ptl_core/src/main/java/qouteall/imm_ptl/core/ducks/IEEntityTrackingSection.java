package qouteall.imm_ptl.core.ducks;

import net.minecraft.util.TypeFilter;

import java.util.function.Consumer;

public interface IEEntityTrackingSection {
    void myForeach(TypeFilter type, Consumer action);
}
