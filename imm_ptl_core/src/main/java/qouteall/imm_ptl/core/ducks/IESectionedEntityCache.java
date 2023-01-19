package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;

import javax.annotation.Nullable;
import java.util.function.Function;

public interface IESectionedEntityCache<T extends EntityAccess> {
    @Nullable
    public <R> R ip_traverseSectionInBox(
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Function<EntitySection<T>, R> function
    );
}
