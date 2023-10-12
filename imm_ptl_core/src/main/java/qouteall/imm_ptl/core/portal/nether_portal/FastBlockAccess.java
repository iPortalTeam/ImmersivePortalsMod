package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.NotNull;

/**
 * Puts all sections into an array to reduce memory access indirection.
 * Also, it doesn't use BlockPos to avoid object allocation
 * (JVM does not always optimize object allocation out. Waiting for Valhalla).
 * Comparing to {@link WorldGenRegion}, getting block state won't throw exception when out of bound.
 */
public record FastBlockAccess(
    // [dx + dy * lX + dz * lX * lY]
    LevelChunkSection[] sections,
    int lowerCX, int lowerCY, int lowerCZ,
    int lX, int lY, int lZ
) {
    public static FastBlockAccess from(
        Level world,
        ChunkPos centerChunkPos,
        int radiusChunks
    ) {
        int lowerCX = centerChunkPos.x - radiusChunks;
        int lowerCY = world.getMinSection();
        int lowerCZ = centerChunkPos.z - radiusChunks;
        int upperCX = centerChunkPos.x + radiusChunks;
        int upperCY = world.getMaxSection();
        int upperCZ = centerChunkPos.z + radiusChunks;
        
        int lX = upperCX - lowerCX + 1;
        int lY = upperCY - lowerCY + 1;
        int lZ = upperCZ - lowerCZ + 1;
        
        ChunkSource chunkSource = world.getChunkSource();
        LevelChunkSection[] sections = new LevelChunkSection[lX * lY * lZ];
        for (int cx = lowerCX; cx <= upperCX; cx++) {
            for (int cz = lowerCZ; cz <= upperCZ; cz++) {
                LevelChunk chunk = chunkSource.getChunk(cx, cz, false);
                if (chunk != null && !(chunk instanceof EmptyLevelChunk)) {
                    LevelChunkSection[] column = chunk.getSections();
                    for (int cy = lowerCY; cy <= upperCY; cy++) {
                        LevelChunkSection section = column[cy];
                        if (section != null && !section.hasOnlyAir()) {
                            int index = (cx - lowerCX) +
                                (cy - lowerCY) * lX +
                                (cz - lowerCZ) * lX * lY;
                            sections[index] = section;
                        }
                    }
                }
            }
        }
        
        return new FastBlockAccess(
            sections, lowerCX, lowerCY, lowerCZ, lX, lY, lZ
        );
    }
    
    public @NotNull BlockState getBlockState(
        int x, int y, int z
    ) {
        int cx = x >> 4;
        int cy = y >> 4;
        int cz = z >> 4;
        
        if (cx < lowerCX || cx >= lowerCX + lX ||
            cy < lowerCY || cy >= lowerCY + lY ||
            cz < lowerCZ || cz >= lowerCZ + lZ
        ) {
            return Blocks.AIR.defaultBlockState();
        }
        
        int index = (cx - lowerCX) +
            (cy - lowerCY) * lX +
            (cz - lowerCZ) * lX * lY;
        
        LevelChunkSection section = sections[index];
        
        if (section == null) {
            return Blocks.AIR.defaultBlockState();
        }
        
        return section.getBlockState(x & 15, y & 15, z & 15);
    }
}
