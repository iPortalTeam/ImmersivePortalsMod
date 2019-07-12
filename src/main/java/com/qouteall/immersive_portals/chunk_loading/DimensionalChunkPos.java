package com.qouteall.immersive_portals.chunk_loading;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.Objects;


public class DimensionalChunkPos {
    public DimensionType dimension;
    public int x;
    public int z;
    
    public DimensionalChunkPos(DimensionType dimension, int x, int z) {
        this.dimension = dimension;
        this.x = x;
        this.z = z;
    }
    
    public DimensionalChunkPos(DimensionType dimension, ChunkPos chunkPos) {
        this(dimension, chunkPos.x, chunkPos.z);
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
            dimension.equals(that.dimension);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dimension.getRawId(), x, z);
    }
    
    @Override
    public String toString() {
        return "DimensionalChunkPos{" +
            "dimension=" + dimension +
            ", x=" + x +
            ", z=" + z +
            '}';
    }
}
