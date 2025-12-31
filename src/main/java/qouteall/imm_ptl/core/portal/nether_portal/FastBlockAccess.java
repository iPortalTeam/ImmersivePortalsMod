package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    int lX, int lY, int lZ,
    Level world
) {

    public static FastBlockAccess from(
        Level world,
        ChunkPos centerChunkPos,
        int radiusChunks
    ) {
        // chunk bounds
        int lowerCX = centerChunkPos.x - radiusChunks;
        int lowerCZ = centerChunkPos.z - radiusChunks;
        int upperCXExclusive = centerChunkPos.x + radiusChunks + 1;
        int upperCZExclusive = centerChunkPos.z + radiusChunks + 1;

        // section bounds (convert block coords → section coords)
        int minSectionY = SectionPos.blockToSectionCoord(world.getMinY());
        int maxSectionYExclusive =
            SectionPos.blockToSectionCoord(world.getMinY() + world.getHeight() - 1) + 1;

        return from(world, lowerCX, upperCXExclusive,
            minSectionY, maxSectionYExclusive,
            lowerCZ, upperCZExclusive);
    }

    /**
     * Note: the upper chunk coord is exclusive.
     */
    @NotNull
    public static FastBlockAccess from(
        Level world,
        int lowerCX, int upperCXExclusive,
        int lowerCY, int upperCYExclusive,
        int lowerCZ, int upperCZExclusive
    ) {
        int lX = upperCXExclusive - lowerCX;
        int lY = upperCYExclusive - lowerCY;
        int lZ = upperCZExclusive - lowerCZ;

        int minSectionY = SectionPos.blockToSectionCoord(world.getMinY());
        int maxSectionYExclusive =
            SectionPos.blockToSectionCoord(world.getMinY() + world.getHeight() - 1) + 1;

        Validate.isTrue(lowerCY >= minSectionY, "Min section Y out of range");
        Validate.isTrue(upperCYExclusive <= maxSectionYExclusive, "Max section Y out of range");

        ChunkSource chunkSource = world.getChunkSource();
        LevelChunkSection[] sections = new LevelChunkSection[lX * lY * lZ];

        for (int cx = lowerCX; cx < upperCXExclusive; cx++) {
            for (int cz = lowerCZ; cz < upperCZExclusive; cz++) {
                LevelChunk chunk = chunkSource.getChunk(cx, cz, false);
                if (chunk != null && !(chunk instanceof EmptyLevelChunk)) {
                    LevelChunkSection[] column = chunk.getSections();
                    for (int cy = lowerCY; cy < upperCYExclusive; cy++) {
                        int sectionIndex = cy - minSectionY;
                        if (sectionIndex < 0 || sectionIndex >= column.length) continue;
                        LevelChunkSection section = column[sectionIndex];
                        if (section != null && !section.hasOnlyAir()) {
                            int index = (cx - lowerCX)
                                + (cy - lowerCY) * lX
                                + (cz - lowerCZ) * lX * lY;
                            sections[index] = section;
                        }
                    }
                }
            }
        }

        return new FastBlockAccess(
            sections, lowerCX, lowerCY, lowerCZ, lX, lY, lZ, world
        );
    }

    public @NotNull BlockState getBlockState(int x, int y, int z) {
        int cx = x >> 4;
        int cy = y >> 4;
        int cz = z >> 4;

        LevelChunkSection section = getSection(cx, cy, cz);
        if (section == null) {
            return Blocks.AIR.defaultBlockState();
        }

        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    public @Nullable LevelChunkSection getSection(int cx, int cy, int cz) {
        if (cx < lowerCX || cx >= lowerCX + lX ||
            cy < lowerCY || cy >= lowerCY + lY ||
            cz < lowerCZ || cz >= lowerCZ + lZ) {
            return null;
        }

        int index = (cx - lowerCX)
            + (cy - lowerCY) * lX
            + (cz - lowerCZ) * lX * lY;

        return sections[index];
    }

    public Stream<SectionPos> sectionPoses() {
        return IntStream.range(0, lY).boxed()
            .flatMap(y -> IntStream.range(0, lZ).boxed()
                .flatMap(z -> IntStream.range(0, lX)
                    .mapToObj(x -> SectionPos.of(
                        x + lowerCX, y + lowerCY, z + lowerCZ
                    ))
                )
            );
    }

    public Stream<ChunkPos> chunkPoses() {
        return IntStream.range(0, lZ).boxed()
            .flatMap(z -> IntStream.range(0, lX)
                .mapToObj(x -> new ChunkPos(
                    x + lowerCX, z + lowerCZ
                ))
            );
    }

    public int minSectionX() { return lowerCX; }
    public int minSectionY() { return lowerCY; }
    public int minSectionZ() { return lowerCZ; }

    public int maxSectionXInclusive() { return lowerCX + lX - 1; }
    public int maxSectionYInclusive() { return lowerCY + lY - 1; }
    public int maxSectionZInclusive() { return lowerCZ + lZ - 1; }

    public int maxSectionXExclusive() { return lowerCX + lX; }
    public int maxSectionYExclusive() { return lowerCY + lY; }
    public int maxSectionZExclusive() { return lowerCZ + lZ; }
}
