package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.q_misc_util.my_util.MyTaskList;

// TODO considering adding a boolean telling whether to do chunk tick
public final record ChunkLoader(
    ResourceKey<Level> dimension,
    int x,
    int z,
    int radius
) {
    public ChunkLoader(DimensionalChunkPos center, int radius) {
        this(center.dimension, center.x, center.z, radius);
    }
    
    public DimensionalChunkPos getCenter() {
        return new DimensionalChunkPos(dimension, x, z);
    }
    
    public int getLoadedChunkNum(MinecraftServer server) {
        int[] numBox = {0};
        
        ServerLevel serverWorld = McHelper.getServerWorld(server, dimension);
        
        foreachChunkPos((dim, x, z, dist) -> {
            if (McHelper.isServerChunkFullyLoaded(serverWorld, new ChunkPos(x, z))) {
                numBox[0] += 1;
            }
        });
        return numBox[0];
    }
    
    public int getChunkNum() {
        return (this.radius * 2 + 1) * (this.radius * 2 + 1);
    }
    
    public boolean isFullyLoaded(MinecraftServer server) {
        return getLoadedChunkNum(server) >= getChunkNum();
    }
    
    public void foreachChunkPos(ChunkPosConsumer func) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                func.consume(
                    dimension,
                    x + dx,
                    z + dz,
                    Math.max(Math.abs(dx), Math.abs(dz))
                );
            }
        }
    }
    
    public void foreachChunkPosFromInnerToOuter(ChunkPosConsumer func) {
        // case for r == 0
        func.consume(dimension, x, z, 0);
        
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
            
            int minX = x - r;
            int maxX = x + r;
            int minY = z - r;
            int maxY = z + r;
            
            for (int y = minY; y < maxY; y++) {
                func.consume(dimension, maxX, y, r);
            }
            
            for (int x = maxX; x > minX; x--) {
                func.consume(dimension, x, maxY, r);
            }
            
            for (int y = maxY; y > minY; y--) {
                func.consume(dimension, minX, y, r);
            }
            
            for (int x = minX; x < maxX; x++) {
                func.consume(dimension, x, minY, r);
            }
        }
    }
    
    public LenientChunkRegion createChunkRegion(MinecraftServer server) {
        ServerLevel world = server.getLevel(dimension);
        
        return LenientChunkRegion.createLenientChunkRegion(getCenter(), radius, world);
    }
    
    /**
     * Load chunks and execute something when the chunks are loaded, then remove the chunk loader.
     * Note: if the server closes before the chunks load, it won't be executed when server starts again.
     */
    public void loadChunksAndDo(MinecraftServer server, Runnable runnable) {
        ImmPtlChunkTracking.addGlobalAdditionalChunkLoader(server, this);
        
        ServerTaskList.of(server).addTask(MyTaskList.withDelayCondition(
            () -> getLoadedChunkNum(server) < getChunkNum(),
            MyTaskList.oneShotTask(() -> {
                ImmPtlChunkTracking.removeGlobalAdditionalChunkLoader(server, this);
                runnable.run();
            })
        ));
    }
    
    @Override
    public String toString() {
        return "(%s %d %d %d)".formatted(dimension.location(), x, z, radius);
    }
    
    public static interface ChunkPosConsumer {
        void consume(ResourceKey<Level> dimension, int x, int z, int distanceToSource);
    }
}
