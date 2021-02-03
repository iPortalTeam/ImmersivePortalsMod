package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import com.qouteall.immersive_portals.my_util.ObjectBuffer;
import com.qouteall.immersive_portals.optifine_compatibility.OFBuiltChunkStorageFix;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

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
    
    private final ChunkBuilder factory;
    private final Map<BlockPos, ChunkBuilder.BuiltChunk> builtChunkMap = new HashMap<>();
    private final Map<ChunkPos, Preset> presets = new HashMap<>();
    private boolean shouldUpdateMainPresetNeighbor = true;
    private final ObjectBuffer<ChunkBuilder.BuiltChunk> builtChunkBuffer;
    
    public MyBuiltChunkStorage(
        ChunkBuilder chunkBuilder,
        World world,
        int r,
        WorldRenderer worldRenderer
    ) {
        super(chunkBuilder, world, r, worldRenderer);
        factory = chunkBuilder;
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, MyBuiltChunkStorage::tick
        );
        
        int cacheSize = sizeX * sizeY * sizeZ;
        if (Global.cacheGlBuffer) {
            cacheSize = cacheSize / 10;
        }
        
        builtChunkBuffer = new ObjectBuffer<>(
            cacheSize,
            () -> factory.new BuiltChunk(),
            ChunkBuilder.BuiltChunk::delete
        );
        
        ModMain.preGameRenderSignal.connectWithWeakRef(this, (this_) -> {
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
        boolean isRenderingPortal = PortalRendering.isRendering();
        if (!isRenderingPortal) {
            if (shouldUpdateMainPresetNeighbor) {
                shouldUpdateMainPresetNeighbor = false;
                OFBuiltChunkStorageFix.updateNeighbor(this, preset.data);
                preset.isNeighborUpdated = true;
            }
        }
        
        if (!preset.isNeighborUpdated) {
            OFBuiltChunkStorageFix.updateNeighbor(this, preset.data);
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
    private int getChunkIndex(int x, int y, int z) {
        return (z * this.sizeY + y) * this.sizeX + x;
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
                
                OFBuiltChunkStorageFix.onBuiltChunkCreated(
                    this, builtChunk
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
        
        foreachActiveBuiltChunks(builtChunk -> {
            ((IEBuiltChunk) builtChunk).setMark(currentTime);
        });
        
        builtChunkMap.entrySet().removeIf(entry -> {
            ChunkBuilder.BuiltChunk chunk = entry.getValue();
            if (((IEBuiltChunk) chunk).getMark() != currentTime) {
                builtChunkBuffer.returnObject(chunk);
                return true;
            }
            else {
                return false;
            }
        });
        
        OFBuiltChunkStorageFix.purgeRenderRegions(this);
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private Set<ChunkBuilder.BuiltChunk> getAllActiveBuiltChunks() {
        HashSet<ChunkBuilder.BuiltChunk> result = new HashSet<>();
        
        presets.forEach((key, preset) -> {
            result.addAll(Arrays.asList(preset.data));
        });
        
        if (chunks != null) {
            result.addAll(Arrays.asList(chunks));
        }
        
        return result;
    }
    
    private void foreachActiveBuiltChunks(Consumer<ChunkBuilder.BuiltChunk> func) {
        if (chunks != null) {
            for (ChunkBuilder.BuiltChunk chunk : chunks) {
                func.accept(chunk);
            }
        }
        
        for (Preset value : presets.values()) {
            for (ChunkBuilder.BuiltChunk chunk : value.data) {
                func.accept(chunk);
            }
        }
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
    
    public String getDebugString() {
        return String.format(
            "All:%s Needs Rebuild:%s",
            builtChunkMap.size(),
            builtChunkMap.values().stream()
                .filter(
                    builtChunk -> builtChunk.needsRebuild()
                ).count()
        );
    }
    
    public int getRadius() {
        return (sizeX - 1) / 2;
    }
    
    public boolean isRegionActive(int cxStart, int czStart, int cxEnd, int czEnd) {
        for (int cx = cxStart; cx < cxEnd; cx++) {
            for (int cz = czStart; cz < czEnd; cz++) {
                if (builtChunkMap.containsKey(new BlockPos(cx * 16, 0, cz * 16))) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
