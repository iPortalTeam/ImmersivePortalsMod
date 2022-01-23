package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.function.Consumer;

public interface IEEntityTrackingSection {
    void myForeach(EntityTypeTest type, Consumer action);
}
