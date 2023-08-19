package qouteall.imm_ptl.core.compat.mixin.sodium;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.Helper;

import static me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists.Builder;

@Mixin(value = RenderRegion.class, remap = false)
public class MixinSodiumRenderRegion {
    @Shadow
    @Final
    private ChunkRenderList renderList;
    
    @Unique
    private @Nullable ObjectArrayList<ChunkRenderList> chunkRenderListsForPortalRendering = null;
    
    /**
     * @author qouteall
     * @reason With ImmPtl, the world rendering process is as follows:
     * 1. render solid things
     * 2. render portal recursively (will increase frame counter)
     * 3. render transparent things
     * When rendering the world in portal (to-same-world portal),
     * the frame counter increases, then in
     * {@link Builder#add(RenderSection)} it will reset the ChunkRenderList,
     * which makes upcoming transparent block rendering in outer world to break.
     * So use separate ChunkRenderList for each portal rendering layer.
     */
    @Overwrite
    public ChunkRenderList getRenderList() {
        if (!PortalRendering.isRendering()) {
            return renderList;
        }
        
        RenderRegion this_ = (RenderRegion) (Object) this;
        
        if (chunkRenderListsForPortalRendering == null) {
            chunkRenderListsForPortalRendering = new ObjectArrayList<>();
        }
        
        int layer = PortalRendering.getPortalLayer();
        int index = layer - 1;
        ChunkRenderList result = Helper.arrayListComputeIfAbsent(
            chunkRenderListsForPortalRendering,
            index,
            () -> new ChunkRenderList(this_)
        );
        
        return result;
    }
}
