package com.qouteall.immersive_portals.chunk_loading;

import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.Objects;


public class DimensionalChunkPos {
    public DimensionType dimensionType;
    public int x;
    public int z;
    
    public DimensionalChunkPos(DimensionType dimensionType, int x, int z) {
        this.dimensionType = dimensionType;
        this.x = x;
        this.z = z;
    }
    
    public DimensionalChunkPos(DimensionType dimensionType, ChunkPos chunkPos) {
        this(dimensionType, chunkPos.x, chunkPos.z);
    }
    
    public ChunkPos getChunkPos() {
        return new ChunkPos(x, z);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionalChunkPos that = (DimensionalChunkPos) o;
        return x == that.x &&
            z == that.z &&
            dimensionType.equals(that.dimensionType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dimensionType.getRawId(), x, z);
    }
}
