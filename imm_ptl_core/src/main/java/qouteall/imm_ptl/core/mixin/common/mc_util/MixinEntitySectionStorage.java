package qouteall.imm_ptl.core.mixin.common.mc_util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IESectionedEntityCache;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;

import java.util.function.Function;

@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage<T extends EntityAccess> implements IESectionedEntityCache<T> {
    
    @Shadow
    @Final
    private LongSortedSet sectionIds;
    
    @Shadow
    @Final
    private Long2ObjectMap<EntitySection<T>> sections;
    
    /**
     * {@link EntitySectionStorage#forEachAccessibleNonEmptySection(AABB, AbortableIterationConsumer)}
     */
    @IPVanillaCopy
    @Nullable
    @Override
    public <R> R ip_traverseSectionInBox(
        int chunkXStart, int chunkXEnd, int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd, Function<EntitySection<T>, R> function
    ) {
        for (int cx = chunkXStart; cx <= chunkXEnd; ++cx) {
            long xIndexStart = SectionPos.asLong(cx, 0, 0);
            long xIndexEnd = SectionPos.asLong(cx, -1, -1);
            LongBidirectionalIterator iterator =
                sectionIds.subSet(xIndexStart, xIndexEnd + 1L).iterator();
            
            while (iterator.hasNext()) {
                long sectionPos = iterator.nextLong();
                int sectionY = SectionPos.y(sectionPos);
                int sectionZ = SectionPos.z(sectionPos);
                if (sectionY >= chunkYStart && sectionY <= chunkYEnd && sectionZ >= chunkZStart && sectionZ <= chunkZEnd) {
                    EntitySection<T> entityTrackingSection = sections.get(sectionPos);
                    if (entityTrackingSection != null && entityTrackingSection.getStatus().isAccessible()) {
                        R result = function.apply(entityTrackingSection);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }
}
