package com.qouteall.immersive_portals.ducks;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

public interface IEWorldRenderer {
    EntityRenderDispatcher getEntityRenderDispatcher();
    
    BuiltChunkStorage getBuiltChunkStorage();
    
    ObjectList getVisibleChunks();
    
    void setVisibleChunks(ObjectList l);
    
    ChunkBuilder getChunkBuilder();
}
