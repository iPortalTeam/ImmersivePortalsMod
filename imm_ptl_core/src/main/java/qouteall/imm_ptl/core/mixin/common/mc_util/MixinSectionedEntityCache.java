package qouteall.imm_ptl.core.mixin.common.mc_util;

import qouteall.imm_ptl.core.ducks.IESectionedEntityCache;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongBidirectionalIterator;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

@Mixin(SectionedEntityCache.class)
public class MixinSectionedEntityCache implements IESectionedEntityCache {
    
    @Shadow
    @Final
    private LongSortedSet trackedPositions;
    
    @Shadow
    @Final
    private Long2ObjectMap<EntityTrackingSection> trackingSections;
    
    @Override
    public void forEachSectionInBox(
        int chunkXStart, int chunkXEnd,
        int chunkYStart, int chunkYEnd,
        int chunkZStart, int chunkZEnd,
        Consumer<EntityTrackingSection> action
    ) {
        
        for (int cx = chunkXStart; cx <= chunkXEnd; ++cx) {
            long xIndexStart = ChunkSectionPos.asLong(cx, 0, 0);
            long xIndexEnd = ChunkSectionPos.asLong(cx, -1, -1);
            LongBidirectionalIterator iterator =
                trackedPositions.subSet(xIndexStart, xIndexEnd + 1L).iterator();
            
            while (iterator.hasNext()) {
                long sectionPos = iterator.nextLong();
                int sectionY = ChunkSectionPos.unpackY(sectionPos);
                int sectionZ = ChunkSectionPos.unpackZ(sectionPos);
                if (sectionY >= chunkYStart && sectionY <= chunkYEnd && sectionZ >= chunkZStart && sectionZ <= chunkZEnd) {
                    EntityTrackingSection entityTrackingSection = trackingSections.get(sectionPos);
                    if (entityTrackingSection != null && entityTrackingSection.getStatus().shouldTrack()) {
                        action.accept(entityTrackingSection);
                    }
                }
            }
        }
    }
}
