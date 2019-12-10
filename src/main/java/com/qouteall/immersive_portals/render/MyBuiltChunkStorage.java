package com.qouteall.immersive_portals.render;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.TriConsumer;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyBuiltChunkStorage extends BuiltChunkStorage {
    
    
    public static class Preset {
        public ChunkBuilder.BuiltChunk[] data;
        public boolean isNeighborUpdated;
        
        public Preset(ChunkBuilder.BuiltChunk[] data, boolean isNeighborUpdated) {
            this.data = data;
            this.isNeighborUpdated = isNeighborUpdated;
        }
        
        public void updateNeighbor(
            TriConsumer<ChunkBuilder.BuiltChunk, Direction, ChunkBuilder.BuiltChunk> func
        ) {
            //for optifine
        }
    }
    
    private ChunkBuilder factory;
    private Map<BlockPos, ChunkBuilder.BuiltChunk> builtChunkMap = new HashMap<>();
    private Map<ChunkPos, Preset> presets = new HashMap<>();
    private WeakReference<Preset> mainPreset;
    
    public MyBuiltChunkStorage(
        ChunkBuilder chunkBuilder_1,
        World world_1,
        int int_1,
        WorldRenderer worldRenderer_1
    ) {
        super(chunkBuilder_1, world_1, int_1, worldRenderer_1);
        factory = chunkBuilder_1;
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, MyBuiltChunkStorage::tick
        );
    }
    
    @Override
    protected void createChunks(ChunkBuilder chunkBuilder_1) {
        //nothing
    }
    
    @Override
    public void clear() {
        getAllActiveBuiltChunks().forEach(
            ChunkBuilder.BuiltChunk::delete
        );
        builtChunkMap.clear();
        presets.clear();
    }
    
    @Override
    public void updateCameraPosition(double playerX, double playerZ) {
        ChunkPos cameraChunkPos = new ChunkPos(
            MathHelper.floorDiv((int) playerX, 16),
            MathHelper.floorDiv((int) playerZ, 16)
        );
        
        Preset preset = presets.computeIfAbsent(
            cameraChunkPos,
            whatever -> myCreatePreset(playerX, playerZ)
            //whatever -> createPreset(cameraChunkPos.x, cameraChunkPos.z)
        );
        
        this.chunks = preset.data;
    }
    
    @Override
    public void scheduleRebuild(int int_1, int int_2, int int_3, boolean boolean_1) {
        ChunkBuilder.BuiltChunk builtChunk = provideBuiltChunk(
            new BlockPos(int_1 * 16, int_2 * 16, int_3 * 16)
        );
        builtChunk.scheduleRebuild(boolean_1);
    }
    
    @Deprecated
    private Preset createPreset(int centerChunkX, int centerChunkZ) {
        ChunkBuilder.BuiltChunk[] chunks1 =
            new ChunkBuilder.BuiltChunk[this.sizeX * this.sizeY * this.sizeZ];
        
        int radiusX = sizeX / 2;
        int radiusZ = sizeZ / 2;
        
        for (int cx = -radiusX; cx <= radiusX; cx++) {
            for (int cz = -radiusX; cz <= radiusZ; cz++) {
                for (int cy = 0; cy < sizeY; cy++) {
                    int index = this.getChunkIndex(
                        cx + radiusX,
                        cy,
                        cz + radiusZ
                    );
                    BlockPos origin = new BlockPos(
                        (cx + centerChunkX) * 16,
                        cy * 16,
                        (cz + centerChunkZ) * 16
                    );
                    
                    chunks1[index] = provideBuiltChunk(origin);
                }
            }
        }
        
        return new Preset(chunks1, false);
    }
    
    private Preset myCreatePreset(double playerXCoord, double playerZCoord) {
        ChunkBuilder.BuiltChunk[] chunks =
            new ChunkBuilder.BuiltChunk[this.sizeX * this.sizeY * this.sizeZ];
        
        int int_1 = MathHelper.floor(playerXCoord);
        int int_2 = MathHelper.floor(playerZCoord);
        
        for (int cx = 0; cx < this.sizeX; ++cx) {
            int int_4 = this.sizeX * 16;
            int int_5 = int_1 - 8 - int_4 / 2;
            int px = int_5 + Math.floorMod(cx * 16 - int_5, int_4);
            
            for (int cz = 0; cz < this.sizeZ; ++cz) {
                int int_8 = this.sizeZ * 16;
                int int_9 = int_2 - 8 - int_8 / 2;
                int pz = int_9 + Math.floorMod(cz * 16 - int_9, int_8);
                
                for (int cy = 0; cy < this.sizeY; ++cy) {
                    int py = cy * 16;
                    
                    int index = this.getChunkIndex(
                        cx,
                        cy,
                        cz
                    );
                    Validate.isTrue(px % 16 == 0);
                    Validate.isTrue(py % 16 == 0);
                    Validate.isTrue(pz % 16 == 0);
                    chunks[index] = provideBuiltChunk(
                        new BlockPos(px, py, pz)
                    );
                }
            }
        }
        
        return new Preset(chunks, false);
    }
    
    //copy because private
    private int getChunkIndex(int int_1, int int_2, int int_3) {
        return (int_3 * this.sizeY + int_2) * this.sizeX + int_1;
    }
    
    private static BlockPos getBasePos(BlockPos blockPos) {
        return new BlockPos(
            MathHelper.floorDiv(blockPos.getX(), 16) * 16,
            MathHelper.floorDiv(MathHelper.clamp(blockPos.getY(), 0, 255), 16) * 16,
            MathHelper.floorDiv(blockPos.getZ(), 16) * 16
        );
    }
    
    public ChunkBuilder.BuiltChunk provideBuiltChunk(BlockPos blockPos) {
        return provideBuiltChunkWithAlignedPos(getBasePos(blockPos));
    }
    
    private ChunkBuilder.BuiltChunk provideBuiltChunkWithAlignedPos(BlockPos basePos) {
        assert basePos.getX() % 16 == 0;
        assert basePos.getY() % 16 == 0;
        assert basePos.getZ() % 16 == 0;
        if (basePos.getY() < 0 || basePos.getY() >= 256) {
            return null;
        }
        
        return builtChunkMap.computeIfAbsent(
            basePos.toImmutable(),
            whatever -> {
                ChunkBuilder.BuiltChunk builtChunk = factory.new BuiltChunk();
                builtChunk.setOrigin(
                    basePos.getX(),
                    basePos.getY(),
                    basePos.getZ()
                );
                return builtChunk;
            }
        );
    }
    
    private Stream<ChunkBuilder.BuiltChunk> getAllActiveBuiltChunks() {
        Stream<ChunkBuilder.BuiltChunk> chunksFromPresets = presets.values().stream()
            .flatMap(
                preset -> Arrays.stream(preset.data)
            );
        if (chunks == null) {
            return chunksFromPresets.distinct();
        }
        else {
            return Streams.concat(
                Arrays.stream(chunks),
                chunksFromPresets
            ).distinct();
        }
    }
    
    private void tick() {
        ClientWorld worldClient = MinecraftClient.getInstance().world;
        if (worldClient != null) {
            if (worldClient.getTime() % 537 == 66) {
                purge();
            }
        }
    }
    
    private void purge() {
        Set<ChunkBuilder.BuiltChunk> activeBuiltChunks =
            getAllActiveBuiltChunks().collect(Collectors.toSet());
    
        List<ChunkBuilder.BuiltChunk> chunksToDelete = builtChunkMap
            .values().stream().filter(
                builtChunk -> !activeBuiltChunks.contains(builtChunk)
            ).collect(Collectors.toList());
        
        chunksToDelete.forEach(
            builtChunk -> {
                builtChunk.delete();
                ChunkBuilder.BuiltChunk removed =
                    builtChunkMap.remove(builtChunk.getOrigin());
                if (removed == null) {
                    Helper.err("Chunk Renderer Abnormal " + builtChunk.getOrigin());
                }
            }
        );
        
        presets.clear();
    }
    
    public int getManagedChunkNum() {
        return builtChunkMap.size();
    }
}
