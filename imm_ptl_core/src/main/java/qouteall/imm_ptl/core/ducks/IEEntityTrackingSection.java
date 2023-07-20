package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;

import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

public interface IEEntityTrackingSection<T extends EntityAccess> {
    /**
     * Easier to use than AbortableIterationConsumer
     */
    @Nullable
    public <Sub extends T, R> R ip_traverse(EntityTypeTest<T, Sub> type, Function<Sub, R> func);
}
