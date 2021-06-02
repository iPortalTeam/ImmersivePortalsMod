package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

// allow storing chunks that are far away from the player
@Environment(EnvType.CLIENT)
public class MyClientChunkManager extends ClientChunkManager {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final WorldChunk emptyChunk;
    protected final LightingProvider lightingProvider;
    protected final ClientWorld world;
    
    protected final Long2ObjectLinkedOpenHashMap<WorldChunk> chunkMap =
        new Long2ObjectLinkedOpenHashMap<>();
    
    public static final SignalArged<WorldChunk> clientChunkLoadSignal = new SignalArged<>();
    public static final SignalArged<WorldChunk> clientChunkUnloadSignal = new SignalArged<>();
    
    public MyClientChunkManager(ClientWorld clientWorld, int loadDistance) {
        super(clientWorld, loadDistance);
        this.world = clientWorld;
        this.emptyChunk = new EmptyChunk(clientWorld, new ChunkPos(0, 0));
        this.lightingProvider = new LightingProvider(
            this,
            true,
            RenderDimensionRedirect.hasSkylight(clientWorld)
        );
        
    }
    
    @Override
    public LightingProvider getLightingProvider() {
        return this.lightingProvider;
    }
    
    @Override
    public void unload(int x, int z) {
        synchronized (chunkMap) {
            
            ChunkPos chunkPos = new ChunkPos(x, z);
            WorldChunk chunk = chunkMap.get(chunkPos.toLong());
            if (positionEquals(chunk, x, z)) {
                chunkMap.remove(chunkPos.toLong());
                O_O.postClientChunkUnloadEvent(chunk);
                world.unloadBlockEntities(chunk);
                clientChunkUnloadSignal.emit(chunk);
            }
        }
    }
    
    @Override
    public WorldChunk getChunk(int x, int z, ChunkStatus chunkStatus, boolean create) {
        // the profiler shows that this is not a hot spot
        synchronized (chunkMap) {
            WorldChunk chunk = chunkMap.get(ChunkPos.toLong(x, z));
            if (positionEquals(chunk, x, z)) {
                return chunk;
            }
            
            return create ? this.emptyChunk : null;
        }
    }
    
    @Override
    public BlockView getWorld() {
        return this.world;
    }
    
    @Override
    public WorldChunk loadChunkFromPacket(
        int x,
        int z,
        BiomeArray biomeArray,
        PacketByteBuf packetByteBuf,
        NbtCompound compoundTag,
        int mask,
        boolean forceCreate
    ) {
        long chunkPosLong = ChunkPos.toLong(x, z);
        
        WorldChunk worldChunk;
        synchronized (chunkMap) {
            worldChunk = (WorldChunk) this.chunkMap.get(chunkPosLong);
            if (!forceCreate && positionEquals(worldChunk, x, z)) {
                worldChunk.loadFromPacket(biomeArray, packetByteBuf, compoundTag, mask);
            }
            else {
                if (biomeArray == null) {
                    LOGGER.error(
                        "Missing Biome Array: {} {} {} Client Biome May be Incorrect",
                        world.getRegistryKey().getValue(), x, z
                    );
                    throw new RuntimeException("Null biome array");
                }
                
                worldChunk = new WorldChunk(this.world, new ChunkPos(x, z), biomeArray);
                worldChunk.loadFromPacket(biomeArray, packetByteBuf, compoundTag, mask);
                chunkMap.put(chunkPosLong, worldChunk);
            }
        }
        
        ChunkSection[] chunkSections = worldChunk.getSectionArray();
        LightingProvider lightingProvider = this.getLightingProvider();
        lightingProvider.setColumnEnabled(new ChunkPos(x, z), true);
        
        for (int cy = 0; cy < chunkSections.length; ++cy) {
            ChunkSection chunkSection = chunkSections[cy];
            lightingProvider.setSectionStatus(
                ChunkSectionPos.from(x, cy, z),
                ChunkSection.isEmpty(chunkSection)
            );
        }
        
        this.world.resetChunkColor(x, z);
        
        O_O.postClientChunkLoadEvent(worldChunk);
        clientChunkLoadSignal.emit(worldChunk);
        
        return worldChunk;
    }
    
    public static void updateLightStatus(WorldChunk chunk) {
        LightingProvider lightingProvider = chunk.getWorld().getLightingProvider();
        ChunkSection[] chunkSections = chunk.getSectionArray();
        for (int cy = 0; cy < chunkSections.length; ++cy) {
            ChunkSection chunkSection = chunkSections[cy];
            lightingProvider.setSectionStatus(
                ChunkSectionPos.from(chunk.getPos().x, cy, chunk.getPos().z),
                ChunkSection.isEmpty(chunkSection)
            );
        }
    }
    
    public List<WorldChunk> getCopiedChunkList() {
        synchronized (chunkMap) {
            return Arrays.asList(chunkMap.values().toArray(new WorldChunk[0]));
        }
    }
    
    @Override
    public void setChunkMapCenter(int x, int z) {
        //do nothing
    }
    
    @Override
    public void updateLoadDistance(int r) {
        //do nothing
    }
    
    @Override
    public String getDebugString() {
        return "Client Chunks (ImmPtl) " + getLoadedChunkCount();
    }
    
    @Override
    public int getLoadedChunkCount() {
        synchronized (chunkMap) {
            return chunkMap.size();
        }
    }
    
    @Override
    public void onLightUpdate(LightType lightType, ChunkSectionPos chunkSectionPos) {
        ClientWorldLoader.getWorldRenderer(
            world.getRegistryKey()
        ).scheduleBlockRender(
            chunkSectionPos.getSectionX(),
            chunkSectionPos.getSectionY(),
            chunkSectionPos.getSectionZ()
        );
    }
    
    @Override
    public boolean shouldTickBlock(BlockPos blockPos) {
        return this.isChunkLoaded(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }
    
    @Override
    public boolean shouldTickChunk(ChunkPos chunkPos) {
        return this.isChunkLoaded(chunkPos.x, chunkPos.z);
    }
    
    @Override
    public boolean shouldTickEntity(Entity entity) {
        return true;// always tick
        // if one living entity teleports from 1 loaded region to another loaded region
        // its position interpolates
        // but if it interpolates into unloaded region
        // does not stop it from ticking
    }
    
    protected static boolean positionEquals(WorldChunk worldChunk, int x, int z) {
        if (worldChunk == null) {
            return false;
        }
        else {
            ChunkPos chunkPos = worldChunk.getPos();
            return chunkPos.x == x && chunkPos.z == z;
        }
    }
    
}
