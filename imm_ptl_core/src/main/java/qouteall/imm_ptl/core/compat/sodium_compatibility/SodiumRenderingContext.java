package qouteall.imm_ptl.core.compat.sodium_compatibility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.graph.ChunkGraphIterationQueue;
import net.minecraft.block.entity.BlockEntity;

public class SodiumRenderingContext {
    public ChunkRenderList chunkRenderList = new ChunkRenderList();
    public ChunkGraphIterationQueue iterationQueue = new ChunkGraphIterationQueue();
    
    public ObjectList<RenderSection> tickableChunks = new ObjectArrayList<>();
    public ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();
}
