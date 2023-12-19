package qouteall.imm_ptl.core.render;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlClientChunkMap;
import qouteall.imm_ptl.core.ducks.IERenderSection;
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
public class ImmPtlViewArea extends ViewArea {
    
    public static class Column {
        public long mark = 0;
        public RenderSection[] sections;
        
        public Column(RenderSection[] sections) {
            this.sections = sections;
        }
    }
    
    public static class Preset {
        public final RenderSection[] data;
        public long lastActiveTime = 0;
        
        public Preset(
            RenderSection[] data
        ) {
            this.data = data;
        }
    }
    
    private final SectionRenderDispatcher factory;
    private final Long2ObjectOpenHashMap<Column> columnMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Preset> presets = new Long2ObjectOpenHashMap<>();
    private Preset currentPreset = null;
    
    public final int minSectionY;
    public final int endSectionY;
    
    private boolean isAlive = true;
    
    public static void init() {
        ImmPtlClientChunkMap.clientChunkUnloadSignal.connect(section -> {
            ResourceKey<Level> dimension = section.getLevel().dimension();
            
            LevelRenderer worldRenderer = ClientWorldLoader.WORLD_RENDERER_MAP.get(dimension);
            
            if (worldRenderer != null) {
                ViewArea viewArea = ((IEWorldRenderer) worldRenderer).ip_getBuiltChunkStorage();
                if (viewArea instanceof ImmPtlViewArea immPtlViewArea) {
                    immPtlViewArea.onChunkUnload(section.getPos().x, section.getPos().z);
                }
            }
        });
        
        IPGlobal.POST_CLIENT_TICK_EVENT.register(() -> {
            if (ClientWorldLoader.getIsInitialized()) {
                for (ClientLevel world : ClientWorldLoader.getClientWorlds()) {
                    LevelRenderer worldRenderer =
                        ClientWorldLoader.getWorldRenderer(world.dimension());
                    ViewArea viewArea = ((IEWorldRenderer) worldRenderer).ip_getBuiltChunkStorage();
                    if (viewArea instanceof ImmPtlViewArea immPtlViewArea) {
                        immPtlViewArea.tick();
                    }
                }
            }
        });
    }
    
    public ImmPtlViewArea(
        SectionRenderDispatcher sectionBuilder,
        Level world,
        int r,
        LevelRenderer worldRenderer
    ) {
        super(sectionBuilder, world, r, worldRenderer);
        factory = sectionBuilder;
        
        int cacheSize = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
        if (IPGlobal.cacheGlBuffer) {
            cacheSize = cacheSize / 10;
        }
        
        minSectionY = McHelper.getMinSectionY(world);
        endSectionY = McHelper.getMaxSectionYExclusive(world);
    }
    
    @Override
    protected void createSections(SectionRenderDispatcher sectionBuilder_1) {
        // WorldRenderer#reload() reads its size
        int num = this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ;
        sections = new RenderSection[num];
    }
    
    @Override
    public void releaseAllBuffers() {
        Set<RenderSection> allActiveBuiltChunks = getAllActiveBuiltChunks();
        allActiveBuiltChunks.forEach(
            RenderSection::releaseBuffers
        );
        columnMap.clear();
        presets.clear();
        
        isAlive = false;
    }
    
    /**
     * It will only be called during vanilla outer world rendering
     * Won't be called in portal rendering
     * In {@link net.minecraft.client.renderer.SectionOcclusionGraph#initializeQueueForFullUpdate(Camera, Queue)} it reads the RenderChunks in another thread.
     */
    @Override
    public void repositionCamera(double playerX, double playerZ) {
        Minecraft.getInstance().getProfiler().push("built_section_storage");
        
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
        
        this.sections = preset.data;
        this.currentPreset = preset;
        
        Minecraft.getInstance().getProfiler().pop();
    }
    
    @Override
    public void setDirty(int cx, int cy, int cz, boolean isImportant) {
        RenderSection builtChunk = provideBuiltChunkByChunkPos(cx, cy, cz);
        builtChunk.setDirty(isImportant);
    }
    
    public RenderSection provideBuiltChunkByChunkPos(int cx, int cy, int cz) {
        Column column = provideColumn(ChunkPos.asLong(cx, cz));
        int offsetChunkY = Mth.clamp(
            cy - McHelper.getMinSectionY(level), 0, McHelper.getYSectionNumber(level) - 1
        );
        return column.sections[offsetChunkY];
    }
    
    /**
     * {@link ViewArea#repositionCamera(double, double)}
     */
    private Preset createPresetByChunkPos(int sectionX, int sectionZ) {
        RenderSection[] sections1 =
            new RenderSection[this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ];
        
        for (int cx = 0; cx < this.sectionGridSizeX; ++cx) {
            int xBlockSize = this.sectionGridSizeX * 16;
            int xStart = (sectionX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            
            for (int cz = 0; cz < this.sectionGridSizeZ; ++cz) {
                int zBlockSize = this.sectionGridSizeZ * 16;
                int zStart = (sectionZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                
                Validate.isTrue(px % 16 == 0);
                Validate.isTrue(pz % 16 == 0);
                
                Column column = provideColumn(ChunkPos.asLong(px >> 4, pz >> 4));
                
                for (int offsetCy = 0; offsetCy < this.sectionGridSizeY; ++offsetCy) {
                    int index = this.getChunkIndex(cx, offsetCy, cz);
                    sections1[index] = column.sections[offsetCy];
                }
            }
        }
        
        return new Preset(sections1);
    }
    
    /**
     * {@link ViewArea#repositionCamera(double, double)}
     */
    private void foreachPresetCoveredChunkPoses(
        int centerChunkX, int centerChunkZ,
        LongConsumer func
    ) {
        RenderSection[] sections1 =
            new RenderSection[this.sectionGridSizeX * this.sectionGridSizeY * this.sectionGridSizeZ];
        
        for (int cx = 0; cx < this.sectionGridSizeX; ++cx) {
            int xBlockSize = this.sectionGridSizeX * 16;
            int xStart = (centerChunkX << 4) - xBlockSize / 2;
            int px = xStart + Math.floorMod(cx * 16 - xStart, xBlockSize);
            
            for (int cz = 0; cz < this.sectionGridSizeZ; ++cz) {
                int zBlockSize = this.sectionGridSizeZ * 16;
                int zStart = (centerChunkZ << 4) - zBlockSize / 2;
                int pz = zStart + Math.floorMod(cz * 16 - zStart, zBlockSize);
                
                Validate.isTrue(px % 16 == 0);
                Validate.isTrue(pz % 16 == 0);
                
                long sectionPos = ChunkPos.asLong(px >> 4, pz >> 4);
                
                func.accept(sectionPos);
            }
        }
    }
    
    //copy because private
    private int getChunkIndex(int x, int y, int z) {
        return (z * this.sectionGridSizeY + y) * this.sectionGridSizeX + x;
    }
    
    public Column provideColumn(long sectionPos) {
        return columnMap.computeIfAbsent(sectionPos, this::createColumn);
    }
    
    private Column createColumn(long sectionPos) {
        RenderSection[] array = new RenderSection[sectionGridSizeY];
        
        int sectionX = ChunkPos.getX(sectionPos);
        int sectionZ = ChunkPos.getZ(sectionPos);
        
        int minY = McHelper.getMinY(level);
        
        for (int offsetCY = 0; offsetCY < sectionGridSizeY; offsetCY++) {
            RenderSection builtChunk = factory.new RenderSection(
                0,
                sectionX << 4, (offsetCY << 4) + minY, sectionZ << 4
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
        Minecraft.getInstance().getProfiler().push("my_built_section_storage_purge");
        
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
        
        ArrayDeque<RenderSection> toDelete = new ArrayDeque<>();
        
        columnMap.long2ObjectEntrySet().removeIf(entry -> {
            Column column = entry.getValue();
            
            boolean shouldRemove = currentTime - column.mark > timeThreshold;
            if (shouldRemove) {
                toDelete.addAll(Arrays.asList(column.sections));
            }
            
            return shouldRemove;
        });
        
        if (!toDelete.isEmpty()) {
            IPGlobal.PRE_GAME_RENDER_TASK_LIST.addTask(() -> {
                if (toDelete.isEmpty()) {
                    return true;
                }
                
                int num = 0;
                while (!toDelete.isEmpty() && num < 100) {
                    RenderSection builtChunk = toDelete.poll();
                    builtChunk.releaseBuffers();
                    num++;
                }
                
                return false;
            });
        }
        
        Minecraft.getInstance().getProfiler().pop();
    }
    
    private boolean shouldDropPreset(long dropTime, long currentTime, Preset preset) {
        if (preset.data == this.sections) {
            return false;
        }
        return currentTime - preset.lastActiveTime > dropTime;
    }
    
    private Set<RenderSection> getAllActiveBuiltChunks() {
        HashSet<RenderSection> result = new HashSet<>();
        
        presets.forEach((key, preset) -> {
            result.addAll(Arrays.asList(preset.data));
        });
        
        if (sections != null) {
            result.addAll(Arrays.asList(sections));
        }
        
        // if this.sections are all null, it will have a null
        result.remove(null);
        
        return result;
    }
    
    public int getManagedSectionNum() {
        return columnMap.size() * sectionGridSizeY;
    }
    
    public String getDebugString() {
        return String.format(
            "Built Section Storage Columns:%s",
            columnMap.size()
        );
    }
    
    public int getRadius() {
        return (sectionGridSizeX - 1) / 2;
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
    
    public void onChunkUnload(int sectionX, int sectionZ) {
        long sectionPos = ChunkPos.asLong(sectionX, sectionZ);
        Column column = columnMap.get(sectionPos);
        if (column != null) {
            for (RenderSection builtChunk : column.sections) {
                ((IERenderSection) builtChunk).portal_fullyReset();
            }
        }
    }
    
    public RenderSection getSectionFromRawArray(
        BlockPos sectionOrigin, RenderSection[] sections
    ) {
        int i = Mth.floorDiv(sectionOrigin.getX(), 16);
        int j = Mth.floorDiv(sectionOrigin.getY() - McHelper.getMinY(level), 16);
        int k = Mth.floorDiv(sectionOrigin.getZ(), 16);
        if (j >= 0 && j < this.sectionGridSizeY) {
            i = Mth.positiveModulo(i, this.sectionGridSizeX);
            k = Mth.positiveModulo(k, this.sectionGridSizeZ);
            return sections[this.getChunkIndex(i, j, k)];
        }
        else {
            return null;
        }
    }
    
    // NOTE it may be accessed from another thread
    @Nullable
    @Override
    protected RenderSection getRenderSectionAt(BlockPos pos) {
        int i = Mth.floorDiv(pos.getX(), 16);
        int j = Mth.floorDiv(pos.getY() - McHelper.getMinY(level), 16);
        int k = Mth.floorDiv(pos.getZ(), 16);
        if (j >= 0 && j < this.sectionGridSizeY) {
            i = Mth.positiveModulo(i, this.sectionGridSizeX);
            k = Mth.positiveModulo(k, this.sectionGridSizeZ);
            int sectionIndex = this.getChunkIndex(i, j, k);
            RenderSection result = this.sections[sectionIndex];
            
            if (result == null) {
                Helper.err("Null RenderChunk " + pos);
                return null;
            }
            
            ((IERenderSection) result).portal_setIndex(sectionIndex);
            return result;
        }
        else {
            return null;
        }
    }
    
    @Nullable
    public RenderSection rawFetch(int cx, int cy, int cz, long timeMark) {
        if (cy < minSectionY || cy >= endSectionY) {
            return null;
        }
        
        long l = ChunkPos.asLong(cx, cz);
        Column column = provideColumn(l);
        
        column.mark = timeMark;
        
        int yOffset = cy - minSectionY;
        
        return column.sections[yOffset];
    }
    
    @Nullable
    public RenderSection rawGet(int cx, int cy, int cz) {
        if (cy < minSectionY || cy >= endSectionY) {
            return null;
        }
        
        long l = ChunkPos.asLong(cx, cz);
        Column column = columnMap.get(l);
        
        if (column == null) {
            return null;
        }
        
        int yOffset = cy - minSectionY;
        
        return column.sections[yOffset];
    }
}
