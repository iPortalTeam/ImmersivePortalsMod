package qouteall.imm_ptl.core.compat.sodium_compatibility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SodiumRenderingContext {
    public ChunkRenderList chunkRenderList = new ChunkRenderList();
    public ObjectList<RenderSection> tickableChunks = new ObjectArrayList<>();
    public ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();
    
    public int renderDistance;
    
    public SodiumRenderingContext(int renderDistance) {
        this.renderDistance = renderDistance;
    }
}
