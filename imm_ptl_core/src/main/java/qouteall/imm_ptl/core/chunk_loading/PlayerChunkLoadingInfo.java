package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import qouteall.q_misc_util.Helper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Set;

public class PlayerChunkLoadingInfo {
    
    public final Set<ResourceKey<Level>> visibleDimensions = new ObjectOpenHashSet<>();
    public final ArrayList<ChunkLoader> additionalChunkLoaders
        = new ArrayList<>();
    public final ArrayList<ArrayDeque<NewChunkTrackingGraph.PlayerWatchRecord>> distanceToPendingChunks =
        new ArrayList<>();
    
    public int loadedChunks = 0;
    
    // normally chunk loading will update following to an interval
    // but if this is true, it will immediately update next tick
    public boolean shouldUpdateImmediately = false;
    
    public PerformanceLevel performanceLevel = PerformanceLevel.bad;
    
    public PlayerChunkLoadingInfo() {
    }
    
    // one chunk may mark pending loading multiple times with different distanceToSource
    public void markPendingLoading(NewChunkTrackingGraph.PlayerWatchRecord record) {
        Helper.arrayListComputeIfAbsent(
            distanceToPendingChunks,
            record.distanceToSource,
            ArrayDeque::new
        ).add(record);
    }
}
