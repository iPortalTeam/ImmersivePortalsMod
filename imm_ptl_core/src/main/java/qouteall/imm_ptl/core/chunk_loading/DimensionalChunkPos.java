package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Objects;


public class DimensionalChunkPos {
    public final ResourceKey<Level> dimension;
    public final int x;
    public final int z;
    
    public DimensionalChunkPos(ResourceKey<Level> dimension, int x, int z) {
        this.dimension = dimension;
        this.x = x;
        this.z = z;
    }
    
    public DimensionalChunkPos(ResourceKey<Level> dimension, ChunkPos chunkPos) {
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
        return Objects.hash(dimension, x, z);
    }
    
    @Override
    public String toString() {
        return "DimensionalChunkPos{" +
            dimension +
            "," + x +
            "," + z +
            '}';
    }
}
