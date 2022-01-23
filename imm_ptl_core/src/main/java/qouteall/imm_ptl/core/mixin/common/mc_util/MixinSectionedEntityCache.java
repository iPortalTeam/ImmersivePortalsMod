package qouteall.imm_ptl.core.mixin.common.mc_util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IESectionedEntityCache;

import java.util.function.Consumer;

@Mixin(EntitySectionStorage.class)
public class MixinSectionedEntityCache implements IESectionedEntityCache {
    
    @Shadow
    @Final
    private LongSortedSet sectionIds;
    
    @Shadow
    @Final
    private Long2ObjectMap<EntitySection> sections;
    
    @Override
    public void forEachSectionInBox(
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Consumer<EntitySection> action
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
                    EntitySection entityTrackingSection = sections.get(sectionPos);
                    if (entityTrackingSection != null && entityTrackingSection.getStatus().isAccessible()) {
                        action.accept(entityTrackingSection);
                    }
                }
            }
        }
    }
}
