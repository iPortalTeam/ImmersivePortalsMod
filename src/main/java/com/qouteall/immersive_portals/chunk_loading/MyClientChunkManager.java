package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
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
import java.util.stream.Stream;

//this class is modified based on ClientChunkManager
//re-write this class upon updating mod
@Environment(EnvType.CLIENT)
public class MyClientChunkManager extends ClientChunkManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final WorldChunk emptyChunk;
    private final LightingProvider lightingProvider;
    private final ClientWorld world;
    
    private final Long2ObjectLinkedOpenHashMap<WorldChunk> chunkMapNew = new Long2ObjectLinkedOpenHashMap<>();
    
    public MyClientChunkManager(ClientWorld clientWorld, int int_1) {
        super(clientWorld, int_1);
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
    public void unload(int int_1, int int_2) {
        synchronized (chunkMapNew) {
            
            ChunkPos chunkPos = new ChunkPos(int_1, int_2);
            WorldChunk worldChunk_1 = chunkMapNew.get(chunkPos.toLong());
            if (positionEquals(worldChunk_1, int_1, int_2)) {
                chunkMapNew.remove(chunkPos.toLong());
                O_O.postChunkUnloadEventForge(worldChunk_1);
            }
        }
    }
    
    @Override
    public WorldChunk getChunk(int int_1, int int_2, ChunkStatus chunkStatus_1, boolean boolean_1) {
        synchronized (chunkMapNew) {
            WorldChunk worldChunk_1 = chunkMapNew.get(ChunkPos.toLong(int_1, int_2));
            if (positionEquals(worldChunk_1, int_1, int_2)) {
                return worldChunk_1;
            }
            
            return boolean_1 ? this.emptyChunk : null;
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
        CompoundTag compoundTag,
        int k,
        boolean bl
    ) {
        long chunkPosLong = ChunkPos.toLong(x, z);
        
        WorldChunk worldChunk;
        synchronized (chunkMapNew) {
            worldChunk = (WorldChunk) this.chunkMapNew.get(chunkPosLong);
            if (!bl && positionEquals(worldChunk, x, z)) {
                worldChunk.loadFromPacket(biomeArray, packetByteBuf, compoundTag, k);
            }
            else {
                if (biomeArray == null) {
                    LOGGER.error(
                        "Missing Biome Array: {} {} {} Client Biome May be Incorrect",
                        world.getRegistryKey().getValue(), x, z
                    );
                    biomeArray = new BiomeArray(
                        Stream.generate(() -> Biomes.PLAINS)
                            .limit(BiomeArray.DEFAULT_LENGTH)
                            .toArray(Biome[]::new)
                    );
                }
                
                worldChunk = new WorldChunk(this.world, new ChunkPos(x, z), biomeArray);
                worldChunk.loadFromPacket(biomeArray, packetByteBuf, compoundTag, k);
                chunkMapNew.put(chunkPosLong, worldChunk);
            }
        }
        
        ChunkSection[] chunkSections = worldChunk.getSectionArray();
        LightingProvider lightingProvider = this.getLightingProvider();
        lightingProvider.setLightEnabled(new ChunkPos(x, z), true);
        
        for (int m = 0; m < chunkSections.length; ++m) {
            ChunkSection chunkSection = chunkSections[m];
            lightingProvider.updateSectionStatus(
                ChunkSectionPos.from(x, m, z),
                ChunkSection.isEmpty(chunkSection)
            );
        }
        
        this.world.resetChunkColor(x, z);
        
        O_O.postChunkLoadEventForge(worldChunk);
        
        return worldChunk;
    }
    
    public static void updateLightStatus(WorldChunk chunk) {
        ChunkSection[] chunkSections_1 = chunk.getSectionArray();
        LightingProvider lightingProvider = chunk.getWorld().getLightingProvider();
        for (int int_5 = 0; int_5 < chunkSections_1.length; ++int_5) {
            ChunkSection chunkSection_1 = chunkSections_1[int_5];
            lightingProvider.updateSectionStatus(
                ChunkSectionPos.from(chunk.getPos().x, int_5, chunk.getPos().z),
                ChunkSection.isEmpty(chunkSection_1)
            );
        }
    }
    
    public List<WorldChunk> getCopiedChunkList() {
        synchronized (chunkMapNew) {
            return Arrays.asList(chunkMapNew.values().toArray(new WorldChunk[0]));
        }
    }
    
    @Override
    public void setChunkMapCenter(int int_1, int int_2) {
        //do nothing
    }
    
    @Override
    public void updateLoadDistance(int int_1) {
        //do nothing
    }
    
    @Override
    public String getDebugString() {
        return "Client Chunks (ImmPtl) " + getLoadedChunkCount();
    }
    
    @Override
    public int getLoadedChunkCount() {
        synchronized (chunkMapNew) {
            return chunkMapNew.size();
        }
    }
    
    @Override
    public void onLightUpdate(LightType lightType_1, ChunkSectionPos chunkSectionPos_1) {
        CGlobal.clientWorldLoader.getWorldRenderer(
            world.getRegistryKey()
        ).scheduleBlockRender(
            chunkSectionPos_1.getSectionX(),
            chunkSectionPos_1.getSectionY(),
            chunkSectionPos_1.getSectionZ()
        );
    }
    
    @Override
    public boolean shouldTickBlock(BlockPos blockPos_1) {
        return this.isChunkLoaded(blockPos_1.getX() >> 4, blockPos_1.getZ() >> 4);
    }
    
    @Override
    public boolean shouldTickChunk(ChunkPos chunkPos_1) {
        return this.isChunkLoaded(chunkPos_1.x, chunkPos_1.z);
    }
    
    @Override
    public boolean shouldTickEntity(Entity entity_1) {
        return this.isChunkLoaded(
            MathHelper.floor(entity_1.getX()) >> 4,
            MathHelper.floor(entity_1.getZ()) >> 4
        );
    }
    
    private static boolean positionEquals(WorldChunk worldChunk_1, int int_1, int int_2) {
        if (worldChunk_1 == null) {
            return false;
        }
        else {
            ChunkPos chunkPos_1 = worldChunk_1.getPos();
            return chunkPos_1.x == int_1 && chunkPos_1.z == int_2;
        }
    }
    
}
