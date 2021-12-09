package qouteall.imm_ptl.core.render;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPModMain;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongConsumer;

public class MyBuiltChunkStorage extends BuiltChunkStorage {
    
    public static class Column {
        public long mark = 0;
        public ChunkBuilder.BuiltChunk[] chunks;
        
        public Column(ChunkBuilder.BuiltChunk[] chunks) {
            this.chunks = chunks;
        }
    }
    
    public static class Preset {
        public final ChunkBuilder.BuiltChunk[] data;
        public long lastActiveTime = 0;
        
        public Preset(
            ChunkBuilder.BuiltChunk[] data
        ) {
            this.data = data;
        }
    }
    
    private final ChunkBuilder factory;
    private final Long2ObjectOpenHashMap<Column> columnMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Preset> presets = new Long2ObjectOpenHashMap<>();
    private Preset currentPreset = null;
    
    public final int minSectionY;
    public final int endSectionY;
    
    private boolean isAlive = true;
    
    public MyBuiltChunkStorage(
        ChunkBuilder chunkBuilder,
        World world,
        int r,
        WorldRenderer worldRenderer
    ) {
        super(chunkBuilder, world, r, worldRenderer);
        factory = chunkBuilder;
        
        IPGlobal.postClientTickSignal.connectWithWeakRef(
            this, MyBuiltChunkStorage::tick
        );
        
        int cacheSize = sizeX * sizeY * sizeZ;
        if (IPGlobal.cacheGlBuffer) {
            cacheSize = cacheSize / 10;
        }
        
        minSectionY = McHelper.getMinSectionY(world);
        endSectionY = McHelper.getMaxSectionYExclusive(world);
    }
    
    @Override
    protected void createChunks(ChunkBuilder chunkBuilder_1) {
        // WorldRenderer#reload() reads its size
        chunks = new BuiltChunk[sizeX * sizeY * sizeZ];
    }
    
    @Override
    public void clear() {
        Set<BuiltChunk> allActiveBuiltChunks = getAllActiveBuiltChunks();
        allActiveBuiltChunks.forEach(
            ChunkBuilder.BuiltChunk::delete
        );
        columnMap.clear();
        presets.clear();
//        builtChunkBuffer.destroyAll();
        
        isAlive = false;
    }
    
    @Override
    public void updateCameraPosition(double playerX, double playerZ) {
        MinecraftClient.getInstance().getProfiler().push("built_chunk_storage");
        
        int cameraBlockX = MathHelper.floor(playerX);
        int cameraBlockZ = MathHelper.floor(playerZ);
        
        int cameraChunkX = cameraBlockX >> 4;
        int cameraChunkZ = cameraBlockZ >> 4;
        ChunkPos cameraChunkPos = new ChunkPos(
            cameraChunkX, cameraChunkZ
        );
        
        Preset preset = presets.computeIfAbsent(
            cameraChunkPos.toLong(),
            whatever -> {
                return createPresetByChunkPos(cameraChunkX, cameraChunkZ);
            }
        );
        preset.lastActiveTime = System.nanoTime();
        
        this.chunks = preset.data;
        this.currentPreset = preset;
        
        MinecraftClient.getInstance().getProfiler().push("neighbor");
        MinecraftClient.getInstance().getProfiler().pop();
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    @Override
    public void scheduleRebuild(int cx, int cy, int cz, boolean isImportant) {
        ChunkBuilder.BuiltChunk builtChunk = provideBuiltChunkByChunkPos(cx, cy, cz);
        builtChunk.scheduleRebuild(isImportant);
    }
    
    public ChunkBuilder.BuiltChunk provideBuiltChunkByChunkPos(int cx, int cy, int cz) {
        Column column = provideColumn(ChunkPos.toLong(cx, cz));
        int offsetChunkY = MathHelper.clamp(
            cy - McHelper.getMinSectionY(world), 0, McHelper.getYSectionNumber(world) - 1
        );
        return column.chunks[offsetChunkY];
    }
    
    /**
     * {@link BuiltChunkStorage#updateCameraPosition(double, double)}
     */
    private Preset createPresetByChunkPos(int chunkX, int chunkZ) {
        ChunkBuilder.BuiltChunk[] chunks1 =
            new ChunkBuilder.BuiltChunk[this.sizeX * this.sizeY * this.sizeZ];
        
        for (int cx = 0; cx < this.sizeX; ++cx) {
            int xBlockSize = this.sizeX * 16;
            int xStart = (chunkX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            
            for (int cz = 0; cz < this.sizeZ; ++cz) {
                int zBlockSize = this.sizeZ * 16;
                int zStart = (chunkZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                
                Validate.isTrue(px % 16 == 0);
                Validate.isTrue(pz % 16 == 0);
                
                Column column = provideColumn(ChunkPos.toLong(px >> 4, pz >> 4));
                
                for (int offsetCy = 0; offsetCy < this.sizeY; ++offsetCy) {
                    int index = this.getChunkIndex(cx, offsetCy, cz);
                    chunks1[index] = column.chunks[offsetCy];
                }
            }
        }
        
        return new Preset(chunks1);
    }
    
    /**
     * {@link BuiltChunkStorage#updateCameraPosition(double, double)}
     */
    private void foreachPresetCoveredChunkPoses(
        int centerChunkX, int centerChunkZ,
        LongConsumer func
    ) {
        ChunkBuilder.BuiltChunk[] chunks1 =
            new ChunkBuilder.BuiltChunk[this.sizeX * this.sizeY * this.sizeZ];
        
        for (int cx = 0; cx < this.sizeX; ++cx) {
            int xBlockSize = this.sizeX * 16;
            int xStart = (centerChunkX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            
            for (int cz = 0; cz < this.sizeZ; ++cz) {
                int zBlockSize = this.sizeZ * 16;
                int zStart = (centerChunkZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                
                Validate.isTrue(px % 16 == 0);
                Validate.isTrue(pz % 16 == 0);
                
                long chunkPos = ChunkPos.toLong(px >> 4, pz >> 4);
                
                func.accept(chunkPos);
            }
        }
    }
    
    //copy because private
    private int getChunkIndex(int x, int y, int z) {
        return (z * this.sizeY + y) * this.sizeX + x;
    }
    
    public Column provideColumn(long chunkPos) {
        return columnMap.computeIfAbsent(chunkPos, this::createColumn);
    }
    
    private Column createColumn(long chunkPos) {
        ChunkBuilder.BuiltChunk[] array = new ChunkBuilder.BuiltChunk[sizeY];
        
        int chunkX = ChunkPos.getPackedX(chunkPos);
        int chunkZ = ChunkPos.getPackedZ(chunkPos);
        
        int minY = McHelper.getMinY(world);
        
        for (int offsetCY = 0; offsetCY < sizeY; offsetCY++) {
            ChunkBuilder.BuiltChunk builtChunk = factory.new BuiltChunk(0);
            
            builtChunk.setOrigin(
                chunkX << 4, (offsetCY << 4) + minY, chunkZ << 4
            );
            
            array[offsetCY] = builtChunk;
        }
        
        return new Column(array);
    }
    
    private void tick() {
        if (!isAlive) {
            return;
        }
        
        ClientWorld worldClient = MinecraftClient.getInstance().world;
        if (worldClient != null) {
            if (GcMonitor.isMemoryNotEnough()) {
                if (worldClient.getTime() % 3 == 0) {
                    purge();
                }
            }
            else {
                if (worldClient.getTime() % 213 == 66) {
                    purge();
                }
            }
        }
    }
    
    private void purge() {
        MinecraftClient.getInstance().getProfiler().push("my_built_chunk_storage_purge");
        
        long dropTime = Helper.secondToNano(GcMonitor.isMemoryNotEnough() ? 3 : 20);
        
        long currentTime = System.nanoTime();
        
        presets.long2ObjectEntrySet().removeIf(entry -> {
            Preset preset = entry.getValue();
            
            long centerChunkPos = entry.getLongKey();
            
            boolean shouldDropPreset = shouldDropPreset(dropTime, currentTime, preset);
            
            if (!shouldDropPreset) {
                foreachPresetCoveredChunkPoses(
                    ChunkPos.getPackedX(centerChunkPos),
                    ChunkPos.getPackedZ(centerChunkPos),
                    columnChunkPos -> {
                        Column column = columnMap.get(columnChunkPos);
                        column.mark = currentTime;
                    }
                );
            }
            
            return shouldDropPreset;
        });
        
        long timeThreshold = Helper.secondToNano(5);
        
        ArrayDeque<BuiltChunk> toDelete = new ArrayDeque<>();
        
        columnMap.long2ObjectEntrySet().removeIf(entry -> {
            Column column = entry.getValue();
            
            boolean shouldRemove = currentTime - column.mark > timeThreshold;
            if (shouldRemove) {
                toDelete.addAll(Arrays.asList(column.chunks));
            }
            
            return shouldRemove;
        });
        
        if (!toDelete.isEmpty()) {
            IPGlobal.preGameRenderTaskList.addTask(() -> {
                if (toDelete.isEmpty()) {
                    return true;
                }
                
                int num = 0;
                while (!toDelete.isEmpty() && num < 100) {
                    BuiltChunk builtChunk = toDelete.poll();
                    builtChunk.delete();
                    num++;
                }
                
                return false;
            });
        }
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private boolean shouldDropPreset(long dropTime, long currentTime, Preset preset) {
        if (preset.data == this.chunks) {
            return false;
        }
        return currentTime - preset.lastActiveTime > dropTime;
    }
    
    private Set<ChunkBuilder.BuiltChunk> getAllActiveBuiltChunks() {
        HashSet<ChunkBuilder.BuiltChunk> result = new HashSet<>();
        
        presets.forEach((key, preset) -> {
            result.addAll(Arrays.asList(preset.data));
        });
        
        if (chunks != null) {
            result.addAll(Arrays.asList(chunks));
        }
        
        // if this.chunks are all null, it will have a null
        result.remove(null);
        
        return result;
    }
    
    public int getManagedSectionNum() {
        return columnMap.size() * sizeY;
    }
    
    public String getDebugString() {
        return String.format(
            "Built Section Storage Columns:%s",
            columnMap.size()
        );
    }
    
    public int getRadius() {
        return (sizeX - 1) / 2;
    }
    
    public boolean isRegionActive(int cxStart, int czStart, int cxEnd, int czEnd) {
        for (int cx = cxStart; cx <= cxEnd; cx++) {
            for (int cz = czStart; cz <= czEnd; cz++) {
                if (columnMap.containsKey(ChunkPos.toLong(cx, cz))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void onChunkUnload(int chunkX, int chunkZ) {
        long chunkPos = ChunkPos.toLong(chunkX, chunkZ);
        Column column = columnMap.get(chunkPos);
        if (column != null) {
            for (ChunkBuilder.BuiltChunk builtChunk : column.chunks) {
                ((IEBuiltChunk) builtChunk).portal_fullyReset();
            }
        }
    }
    
    public ChunkBuilder.BuiltChunk getSectionFromRawArray(
        BlockPos sectionOrigin, ChunkBuilder.BuiltChunk[] chunks
    ) {
        int i = MathHelper.floorDiv(sectionOrigin.getX(), 16);
        int j = MathHelper.floorDiv(sectionOrigin.getY() - McHelper.getMinY(world), 16);
        int k = MathHelper.floorDiv(sectionOrigin.getZ(), 16);
        if (j >= 0 && j < this.sizeY) {
            i = MathHelper.floorMod(i, this.sizeX);
            k = MathHelper.floorMod(k, this.sizeZ);
            return chunks[this.getChunkIndex(i, j, k)];
        }
        else {
            return null;
        }
    }
    
    @Nullable
    @Override
    protected BuiltChunk getRenderedChunk(BlockPos pos) {
        int i = MathHelper.floorDiv(pos.getX(), 16);
        int j = MathHelper.floorDiv(pos.getY() - McHelper.getMinY(world), 16);
        int k = MathHelper.floorDiv(pos.getZ(), 16);
        if (j >= 0 && j < this.sizeY) {
            i = MathHelper.floorMod(i, this.sizeX);
            k = MathHelper.floorMod(k, this.sizeZ);
            int chunkIndex = this.getChunkIndex(i, j, k);
            BuiltChunk result = this.chunks[chunkIndex];
            ((IEBuiltChunk) result).portal_setIndex(chunkIndex);
            return result;
        }
        else {
            return null;
        }
    }
    
    @Nullable
    public BuiltChunk rawFetch(int cx, int cy, int cz, long timeMark) {
        if (cy < minSectionY || cy >= endSectionY) {
            return null;
        }
        
        long l = ChunkPos.toLong(cx, cz);
        Column column = provideColumn(l);
        
        column.mark = timeMark;
        
        int yOffset = cy - minSectionY;
        
        return column.chunks[yOffset];
    }
}
