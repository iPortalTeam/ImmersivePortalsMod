package com.qouteall.immersive_portals.exposer;

import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

import java.util.List;

public interface IEWorldRenderer {
    ChunkRenderDispatcher getChunkRenderDispatcher();
    
    ChunkRendererList getChunkRenderList();
    
    List getChunkInfos();
    
    void setChunkInfos(List list);
    
    EntityRenderDispatcher getEntityRenderDispatcher();
}
