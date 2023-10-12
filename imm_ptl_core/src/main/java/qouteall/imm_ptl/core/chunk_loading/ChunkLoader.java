package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.MyTaskList;

import java.util.Objects;

public class ChunkLoader {
    // TODO flatten it in 1.20.3
    public DimensionalChunkPos center;
    public int radius;
    // TODO remove in 1.20.3
    @Deprecated
    public boolean isDirectLoader = false;
    
    public ChunkLoader(DimensionalChunkPos center, int radius) {
        this(center, radius, false);
    }
    
    @Deprecated
    public ChunkLoader(DimensionalChunkPos center, int radius, boolean isDirectLoader) {
        this.center = center;
        this.radius = radius;
        this.isDirectLoader = isDirectLoader;
    }
    
    public int getLoadedChunkNum() {
        int[] numBox = {0};
        foreachChunkPos((dim, x, z, dist) -> {
            if (McHelper.isServerChunkFullyLoaded(McHelper.getServerWorld(dim), new ChunkPos(x, z))) {
                numBox[0] += 1;
            }
        });
        return numBox[0];
    }
    
    public int getChunkNum() {
        return (this.radius * 2 + 1) * (this.radius * 2 + 1);
    }
    
    public void foreachChunkPos(ChunkPosConsumer func) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                func.consume(
                    center.dimension,
                    center.x + dx,
                    center.z + dz,
                    Math.max(Math.abs(dx), Math.abs(dz))
                );
            }
        }
    }
    
    public void foreachChunkPosFromInnerToOuter(ChunkPosConsumer func) {
        // case for r == 0
        func.consume(center.dimension, center.x, center.z, 0);
        
        for (int r = 1; r <= radius; r++) {
            // traverse the four sides
            // edge1: x = maxX, y = [minY, maxY)  covers (maxX, minY)
            // edge2: y = maxY, x = [maxX, minX)  covers (maxX, maxY)
            // edge3: x = minX, y = [maxY, minY)  covers (minX, maxY)
            // edge4: y = minY, x = [minX, maxX)  covers (minX, minY)
            
            // x - - - - - x
            // |           |
            // |           |
            // |           |
            // |           |
            // x - - - - - x
            
            int minX = center.x - r;
            int maxX = center.x + r;
            int minY = center.z - r;
            int maxY = center.z + r;
            
            for (int y = minY; y < maxY; y++) {
                func.consume(center.dimension, maxX, y, r);
            }
            
            for (int x = maxX; x > minX; x--) {
                func.consume(center.dimension, x, maxY, r);
            }
            
            for (int y = maxY; y > minY; y--) {
                func.consume(center.dimension, minX, y, r);
            }
            
            for (int x = minX; x < maxX; x++) {
                func.consume(center.dimension, x, minY, r);
            }
        }
    }
    
    public LenientChunkRegion createChunkRegion() {
        ServerLevel world = MiscHelper.getServer().getLevel(center.dimension);
        
        return LenientChunkRegion.createLenientChunkRegion(center, radius, world);
    }
    
    /**
     * Load chunks and execute something when the chunks are loaded, then remove the chunk loader.
     * Note: if the server closes before the chunks load, it won't be executed when server starts again.
     */
    public void loadChunksAndDo(Runnable runnable) {
        ImmPtlChunkTracking.addGlobalAdditionalChunkLoader(this);
        
        IPGlobal.serverTaskList.addTask(MyTaskList.withDelayCondition(
            () -> getLoadedChunkNum() < getChunkNum(),
            MyTaskList.oneShotTask(() -> {
                ImmPtlChunkTracking.removeGlobalAdditionalChunkLoader(this);
                runnable.run();
            })
        ));
    }
    
    @Override
    public String toString() {
        return "{" +
            "center=" + center +
            ", radius=" + radius +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChunkLoader that = (ChunkLoader) o;
        return radius == that.radius &&
            center.equals(that.center);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }
    
    public static interface ChunkPosConsumer {
        void consume(ResourceKey<Level> dimension, int x, int z, int distanceToSource);
    }
}
