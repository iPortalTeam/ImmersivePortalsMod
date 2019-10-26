package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.render.chunk.ChunkRenderer;

import java.util.List;

public interface IEChunkRenderList {
    public void setCameraPos(double x, double y, double z);
    
    List<ChunkRenderer> getChunkRenderers();
    
    void setChunkRenderers(List<ChunkRenderer> arg);
}
