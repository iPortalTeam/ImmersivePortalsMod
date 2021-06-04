package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.entity.EntityTrackingSection;

import java.util.function.Consumer;

public interface IESectionedEntityCache {
    public void forEachSectionInBox(
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Consumer<EntityTrackingSection> action
    );
}
