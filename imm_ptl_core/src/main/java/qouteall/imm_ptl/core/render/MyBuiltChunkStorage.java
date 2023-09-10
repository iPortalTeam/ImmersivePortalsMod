package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlClientChunkMap;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.miscellaneous.GcMonitor;
import qouteall.q_misc_util.Helper;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.LongConsumer;

@Environment(EnvType.CLIENT)
@OnlyIn(Dist.CLIENT)
public class MyBuiltChunkStorage extends ViewArea {
    
    public static class Column {
        public long mark = 0;
        public ChunkRenderDispatcher.RenderChunk[] chunks;
        
        public Column(ChunkRenderDispatcher.RenderChunk[] chunks) {
            this.chunks = chunks;
        }
    }
    
    public static class Preset {
        public final ChunkRenderDispatcher.RenderChunk[] data;
        public long lastActiveTime = 0;
        
        public Preset(
            ChunkRenderDispatcher.RenderChunk[] data
        ) {
            this.data = data;
        }
    }
    
    private final ChunkRenderDispatcher factory;
    private final Long2ObjectOpenHashMap<Column> columnMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Preset> presets = new Long2ObjectOpenHashMap<>();
    private Preset currentPreset = null;
    
    public final int minSectionY;
    public final int endSectionY;
    
    private boolean isAlive = true;
    
    public static void init() {
        ImmPtlClientChunkMap.clientChunkUnloadSignal.connect(chunk -> {
            ResourceKey<Level> dimension = chunk.getLevel().dimension();
            
            LevelRenderer worldRenderer = ClientWorldLoader.worldRendererMap.get(dimension);
            
            if (worldRenderer != null) {
                ViewArea viewArea = ((IEWorldRenderer) worldRenderer).ip_getBuiltChunkStorage();
                if (viewArea instanceof MyBuiltChunkStorage myBuiltChunkStorage) {
                    myBuiltChunkStorage.onChunkUnload(chunk.getPos().x, chunk.getPos().z);
                }
            }
        });
    }
    
    public MyBuiltChunkStorage(
        ChunkRenderDispatcher chunkBuilder,
        Level world,
        int r,
        LevelRenderer worldRenderer
    ) {
        super(chunkBuilder, world, r, worldRenderer);
        factory = chunkBuilder;
        
        IPGlobal.postClientTickSignal.connectWithWeakRef(
            this, MyBuiltChunkStorage::tick
        );
        
        int cacheSize = chunkGridSizeX * chunkGridSizeY * chunkGridSizeZ;
        if (IPGlobal.cacheGlBuffer) {
            cacheSize = cacheSize / 10;
        }
        
        minSectionY = McHelper.getMinSectionY(world);
        endSectionY = McHelper.getMaxSectionYExclusive(world);
    }
    
    @Override
    protected void createChunks(ChunkRenderDispatcher chunkBuilder_1) {
        // WorldRenderer#reload() reads its size
        chunks = new RenderChunk[chunkGridSizeX * chunkGridSizeY * chunkGridSizeZ];
    }
    
    @Override
    public void releaseAllBuffers() {
        Set<RenderChunk> allActiveBuiltChunks = getAllActiveBuiltChunks();
        allActiveBuiltChunks.forEach(
            ChunkRenderDispatcher.RenderChunk::releaseBuffers
        );
        columnMap.clear();
        presets.clear();
        
        isAlive = false;
    }
    
    /**
     * It will only be called during vanilla outer world rendering
     * Won't be called in portal rendering
     * In {@link LevelRenderer#initializeQueueForFullUpdate(Camera, Queue)} it reads the RenderChunks in another thread.
     */
    @Override
    public void repositionCamera(double playerX, double playerZ) {
        Minecraft.getInstance().getProfiler().push("built_chunk_storage");
        
        int cameraBlockX = Mth.floor(playerX);
        int cameraBlockZ = Mth.floor(playerZ);
        
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
        
        Minecraft.getInstance().getProfiler().pop();
    }
    
    @Override
    public void setDirty(int cx, int cy, int cz, boolean isImportant) {
        ChunkRenderDispatcher.RenderChunk builtChunk = provideBuiltChunkByChunkPos(cx, cy, cz);
        builtChunk.setDirty(isImportant);
    }
    
    public ChunkRenderDispatcher.RenderChunk provideBuiltChunkByChunkPos(int cx, int cy, int cz) {
        Column column = provideColumn(ChunkPos.asLong(cx, cz));
        int offsetChunkY = Mth.clamp(
            cy - McHelper.getMinSectionY(level), 0, McHelper.getYSectionNumber(level) - 1
        );
        return column.chunks[offsetChunkY];
    }
    
    /**
     * {@link BuiltChunkStorage#updateCameraPosition(double, double)}
     */
    private Preset createPresetByChunkPos(int chunkX, int chunkZ) {
        ChunkRenderDispatcher.RenderChunk[] chunks1 =
            new ChunkRenderDispatcher.RenderChunk[this.chunkGridSizeX * this.chunkGridSizeY * this.chunkGridSizeZ];
        
        for (int cx = 0; cx < this.chunkGridSizeX; ++cx) {
            int xBlockSize = this.chunkGridSizeX * 16;
            int xStart = (chunkX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            
            for (int cz = 0; cz < this.chunkGridSizeZ; ++cz) {
                int zBlockSize = this.chunkGridSizeZ * 16;
                int zStart = (chunkZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                
                Validate.isTrue(px % 16 == 0);
                Validate.isTrue(pz % 16 == 0);
                
                Column column = provideColumn(ChunkPos.asLong(px >> 4, pz >> 4));
                
                for (int offsetCy = 0; offsetCy < this.chunkGridSizeY; ++offsetCy) {
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
        ChunkRenderDispatcher.RenderChunk[] chunks1 =
            new ChunkRenderDispatcher.RenderChunk[this.chunkGridSizeX * this.chunkGridSizeY * this.chunkGridSizeZ];
        
        for (int cx = 0; cx < this.chunkGridSizeX; ++cx) {
            int xBlockSize = this.chunkGridSizeX * 16;
            int xStart = (centerChunkX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            
            for (int cz = 0; cz < this.chunkGridSizeZ; ++cz) {
                int zBlockSize = this.chunkGridSizeZ * 16;
                int zStart = (centerChunkZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                
                Validate.isTrue(px % 16 == 0);
                Validate.isTrue(pz % 16 == 0);
                
                long chunkPos = ChunkPos.asLong(px >> 4, pz >> 4);
                
                func.accept(chunkPos);
            }
        }
    }
    
    //copy because private
    private int getChunkIndex(int x, int y, int z) {
        return (z * this.chunkGridSizeY + y) * this.chunkGridSizeX + x;
    }
    
    public Column provideColumn(long chunkPos) {
        return columnMap.computeIfAbsent(chunkPos, this::createColumn);
    }
    
    private Column createColumn(long chunkPos) {
        ChunkRenderDispatcher.RenderChunk[] array = new ChunkRenderDispatcher.RenderChunk[chunkGridSizeY];
        
        int chunkX = ChunkPos.getX(chunkPos);
        int chunkZ = ChunkPos.getZ(chunkPos);
        
        int minY = McHelper.getMinY(level);
        
        for (int offsetCY = 0; offsetCY < chunkGridSizeY; offsetCY++) {
            ChunkRenderDispatcher.RenderChunk builtChunk = factory.new RenderChunk(
                0,
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
        
        ClientLevel worldClient = Minecraft.getInstance().level;
        if (worldClient != null) {
            if (GcMonitor.isMemoryNotEnough()) {
                if (worldClient.getGameTime() % 3 == 0) {
                    purge();
                }
            }
            else {
                if (worldClient.getGameTime() % 213 == 66) {
                    purge();
                }
            }
        }
    }
    
    private void purge() {
        Minecraft.getInstance().getProfiler().push("my_built_chunk_storage_purge");
        
        long dropTime = Helper.secondToNano(GcMonitor.isMemoryNotEnough() ? 3 : 20);
        
        long currentTime = System.nanoTime();
        
        presets.long2ObjectEntrySet().removeIf(entry -> {
            Preset preset = entry.getValue();
            
            long centerChunkPos = entry.getLongKey();
            
            boolean shouldDropPreset = shouldDropPreset(dropTime, currentTime, preset);
            
            if (!shouldDropPreset) {
                foreachPresetCoveredChunkPoses(
                    ChunkPos.getX(centerChunkPos),
                    ChunkPos.getZ(centerChunkPos),
                    columnChunkPos -> {
                        Column column = columnMap.get(columnChunkPos);
                        column.mark = currentTime;
                    }
                );
            }
            
            return shouldDropPreset;
        });
        
        long timeThreshold = Helper.secondToNano(5);
        
        ArrayDeque<RenderChunk> toDelete = new ArrayDeque<>();
        
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
                    RenderChunk builtChunk = toDelete.poll();
                    builtChunk.releaseBuffers();
                    num++;
                }
                
                return false;
            });
        }
        
        Minecraft.getInstance().getProfiler().pop();
    }
    
    private boolean shouldDropPreset(long dropTime, long currentTime, Preset preset) {
        if (preset.data == this.chunks) {
            return false;
        }
        return currentTime - preset.lastActiveTime > dropTime;
    }
    
    private Set<ChunkRenderDispatcher.RenderChunk> getAllActiveBuiltChunks() {
        HashSet<ChunkRenderDispatcher.RenderChunk> result = new HashSet<>();
        
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
        return columnMap.size() * chunkGridSizeY;
    }
    
    public String getDebugString() {
        return String.format(
            "Built Section Storage Columns:%s",
            columnMap.size()
        );
    }
    
    public int getRadius() {
        return (chunkGridSizeX - 1) / 2;
    }
    
    public boolean isRegionActive(int cxStart, int czStart, int cxEnd, int czEnd) {
        for (int cx = cxStart; cx <= cxEnd; cx++) {
            for (int cz = czStart; cz <= czEnd; cz++) {
                if (columnMap.containsKey(ChunkPos.asLong(cx, cz))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void onChunkUnload(int chunkX, int chunkZ) {
        long chunkPos = ChunkPos.asLong(chunkX, chunkZ);
        Column column = columnMap.get(chunkPos);
        if (column != null) {
            for (ChunkRenderDispatcher.RenderChunk builtChunk : column.chunks) {
                ((IEBuiltChunk) builtChunk).portal_fullyReset();
            }
        }
    }
    
    public ChunkRenderDispatcher.RenderChunk getSectionFromRawArray(
        BlockPos sectionOrigin, ChunkRenderDispatcher.RenderChunk[] chunks
    ) {
        int i = Mth.floorDiv(sectionOrigin.getX(), 16);
        int j = Mth.floorDiv(sectionOrigin.getY() - McHelper.getMinY(level), 16);
        int k = Mth.floorDiv(sectionOrigin.getZ(), 16);
        if (j >= 0 && j < this.chunkGridSizeY) {
            i = Mth.positiveModulo(i, this.chunkGridSizeX);
            k = Mth.positiveModulo(k, this.chunkGridSizeZ);
            return chunks[this.getChunkIndex(i, j, k)];
        }
        else {
            return null;
        }
    }
    
    // NOTE it may be accessed from another thread
    @Nullable
    @Override
    protected RenderChunk getRenderChunkAt(BlockPos pos) {
        int i = Mth.floorDiv(pos.getX(), 16);
        int j = Mth.floorDiv(pos.getY() - McHelper.getMinY(level), 16);
        int k = Mth.floorDiv(pos.getZ(), 16);
        if (j >= 0 && j < this.chunkGridSizeY) {
            i = Mth.positiveModulo(i, this.chunkGridSizeX);
            k = Mth.positiveModulo(k, this.chunkGridSizeZ);
            int chunkIndex = this.getChunkIndex(i, j, k);
            RenderChunk result = this.chunks[chunkIndex];
            
            if (result == null) {
                Helper.err("Null RenderChunk " + pos);
                return null;
            }
            
            ((IEBuiltChunk) result).portal_setIndex(chunkIndex);
            return result;
        }
        else {
            return null;
        }
    }
    
    @Nullable
    public RenderChunk rawFetch(int cx, int cy, int cz, long timeMark) {
        if (cy < minSectionY || cy >= endSectionY) {
            return null;
        }
        
        long l = ChunkPos.asLong(cx, cz);
        Column column = provideColumn(l);
        
        column.mark = timeMark;
        
        int yOffset = cy - minSectionY;
        
        return column.chunks[yOffset];
    }
    
    @Nullable
    public RenderChunk rawGet(int cx, int cy, int cz) {
        if (cy < minSectionY || cy >= endSectionY) {
            return null;
        }
    
        long l = ChunkPos.asLong(cx, cz);
        Column column = columnMap.get(l);
    
        if (column == null) {
            return null;
        }
    
        int yOffset = cy - minSectionY;
    
        return column.chunks[yOffset];
    }
}
