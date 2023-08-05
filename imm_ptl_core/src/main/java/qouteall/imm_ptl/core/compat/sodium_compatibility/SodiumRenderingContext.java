package qouteall.imm_ptl.core.compat.sodium_compatibility;

import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderListBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;

public class SodiumRenderingContext {
    public SortedRenderListBuilder renderListBuilder;
    public SortedRenderLists renderLists;
    
    public int renderDistance;
    
    public SodiumRenderingContext(int renderDistance) {
        this.renderDistance = renderDistance;
        this.renderListBuilder = new SortedRenderListBuilder();
        this.renderLists = SortedRenderLists.empty();
    }
}
