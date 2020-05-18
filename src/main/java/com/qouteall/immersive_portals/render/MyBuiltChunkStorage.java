package com.qouteall.immersive_portals.render;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.ObjectBuffer;
import com.qouteall.immersive_portals.optifine_compatibility.OFBuiltChunkNeighborFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyBuiltChunkStorage extends BuiltChunkStorage {
    
    
    public static class Preset {
        public ChunkBuilder.BuiltChunk[] data;
        public long lastActiveTime;
        public boolean isNeighborUpdated;
        
        public Preset(ChunkBuilder.BuiltChunk[] data, boolean isNeighborUpdated) {
            this.data = data;
            this.isNeighborUpdated = isNeighborUpdated;
        }
    }
    
    private ChunkBuilder factory;
    private Map<BlockPos, ChunkBuilder.BuiltChunk> builtChunkMap = new HashMap<>();
    private Map<ChunkPos, Preset> presets = new HashMap<>();
    private boolean shouldUpdateMainPresetNeighbor = true;
    private ObjectBuffer<ChunkBuilder.BuiltChunk> builtChunkBuffer;
    
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
        
        builtChunkBuffer = new ObjectBuffer<>(
            sizeX * sizeY * sizeZ,
            () -> factory.new BuiltChunk(),
            ChunkBuilder.BuiltChunk::delete
        );
        
        ModMain.preRenderSignal.connectWithWeakRef(this, (this_) -> {
            MinecraftClient.getInstance().getProfiler().push("reserve");
            this_.builtChunkBuffer.reserveObjects(sizeX * sizeY * sizeZ / 100);
            MinecraftClient.getInstance().getProfiler().pop();
        });
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
        builtChunkBuffer.destroyAll();
    }
    
    @Override
    public void updateCameraPosition(double playerX, double playerZ) {
        MinecraftClient.getInstance().getProfiler().push("built_chunk_storage");
        
        ChunkPos cameraChunkPos = new ChunkPos(
            MathHelper.floorDiv((int) playerX, 16),
            MathHelper.floorDiv((int) playerZ, 16)
        );
        
        Preset preset = presets.computeIfAbsent(
            cameraChunkPos,
            whatever -> myCreatePreset(playerX, playerZ)
        );
        preset.lastActiveTime = System.nanoTime();
        
        this.chunks = preset.data;
        
        MinecraftClient.getInstance().getProfiler().push("neighbor");
        manageNeighbor(preset);
        MinecraftClient.getInstance().getProfiler().pop();
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private void manageNeighbor(Preset preset) {
        boolean isRenderingPortal = CGlobal.renderer.isRendering();
        if (!isRenderingPortal) {
            if (shouldUpdateMainPresetNeighbor) {
                shouldUpdateMainPresetNeighbor = false;
                OFBuiltChunkNeighborFix.updateNeighbor(this, preset.data);
                preset.isNeighborUpdated = true;
            }
        }
        
        if (!preset.isNeighborUpdated) {
            OFBuiltChunkNeighborFix.updateNeighbor(this, preset.data);
            preset.isNeighborUpdated = true;
            if (isRenderingPortal) {
                shouldUpdateMainPresetNeighbor = true;
            }
        }
    }
    
    @Override
    public void scheduleRebuild(int cx, int cy, int cz, boolean isImportant) {
        //TODO change it
        ChunkBuilder.BuiltChunk builtChunk = provideBuiltChunk(
            new BlockPos(cx * 16, cy * 16, cz * 16)
        );
        builtChunk.scheduleRebuild(isImportant);
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
                    
                    int index = this.getChunkIndex(cx, cy, cz);
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
                ChunkBuilder.BuiltChunk builtChunk = builtChunkBuffer.takeObject();
                
                builtChunk.setOrigin(
                    basePos.getX(), basePos.getY(), basePos.getZ()
                );
                
                return builtChunk;
            }
        );
    }
    
    private void tick() {
        ClientWorld worldClient = MinecraftClient.getInstance().world;
        if (worldClient != null) {
            if (worldClient.getTime() % 213 == 66) {
                purge();
            }
        }
    }
    
    private void purge() {
        MinecraftClient.getInstance().getProfiler().push("my_built_chunk_storage_purge");
        
        long currentTime = System.nanoTime();
        presets.entrySet().removeIf(entry -> {
            Preset preset = entry.getValue();
            if (preset.data == this.chunks) {
                return false;
            }
            return currentTime - preset.lastActiveTime > Helper.secondToNano(20);
        });
        
        Set<ChunkBuilder.BuiltChunk> activeBuiltChunks = getAllActiveBuiltChunks();
        
        List<ChunkBuilder.BuiltChunk> chunksToDelete = builtChunkMap
            .values().stream().filter(
                builtChunk -> !activeBuiltChunks.contains(builtChunk)
            ).collect(Collectors.toList());
        
        chunksToDelete.forEach(
            builtChunk -> {
                builtChunkBuffer.returnObject(builtChunk);
                //builtChunk.delete();
                ChunkBuilder.BuiltChunk removed =
                    builtChunkMap.remove(builtChunk.getOrigin());
                if (removed == null) {
                    Helper.err("Chunk Renderer Abnormal " + builtChunk.getOrigin());
                }
            }
        );
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private Set<ChunkBuilder.BuiltChunk> getAllActiveBuiltChunks() {
        Stream<ChunkBuilder.BuiltChunk> result;
        Stream<ChunkBuilder.BuiltChunk> chunksFromPresets = presets.values().stream()
            .flatMap(
                preset -> Arrays.stream(preset.data)
            );
        if (chunks == null) {
            result = chunksFromPresets;
        }
        else {
            result = Streams.concat(
                Arrays.stream(chunks),
                chunksFromPresets
            );
        }
        return result.collect(Collectors.toSet());
    }
    
    public int getManagedChunkNum() {
        return builtChunkMap.size();
    }
    
    public ChunkBuilder.BuiltChunk myGetRenderChunkRaw(
        BlockPos pos, ChunkBuilder.BuiltChunk[] chunks
    ) {
        int i = MathHelper.floorDiv(pos.getX(), 16);
        int j = MathHelper.floorDiv(pos.getY(), 16);
        int k = MathHelper.floorDiv(pos.getZ(), 16);
        if (j >= 0 && j < this.sizeY) {
            i = MathHelper.floorMod(i, this.sizeX);
            k = MathHelper.floorMod(k, this.sizeZ);
            return chunks[this.getChunkIndex(i, j, k)];
        }
        else {
            return null;
        }
    }
}
