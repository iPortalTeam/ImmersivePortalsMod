package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.my_util.Helper;
import com.sun.istack.internal.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

//this class is modified based on ClientChunkManager
//re-write this class upon updating mod
public class MyClientChunkManager extends ClientChunkManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final WorldChunk emptyChunk;
    private final LightingProvider lightingProvider;
    private final ClientWorld world;
    
    //its performance is a little lower than vanilla
    //but this is indispensable
    private ConcurrentHashMap<ChunkPos, WorldChunk> chunkMap = new ConcurrentHashMap<>();
    
    public MyClientChunkManager(ClientWorld clientWorld_1, int int_1) {
        super(clientWorld_1, int_1);
        this.world = clientWorld_1;
        this.emptyChunk = new EmptyChunk(clientWorld_1, new ChunkPos(0, 0));
        this.lightingProvider = new LightingProvider(
            this,
            true,
            clientWorld_1.getDimension().hasSkyLight()
        );
    }
    
    @Override
    public LightingProvider getLightingProvider() {
        return this.lightingProvider;
    }
    
    private static boolean isChunkValid(@Nullable WorldChunk worldChunk_1, int int_1, int int_2) {
        if (worldChunk_1 == null) {
            return false;
        }
        else {
            ChunkPos chunkPos_1 = worldChunk_1.getPos();
            return chunkPos_1.x == int_1 && chunkPos_1.z == int_2;
        }
    }
    
    @Override
    public void unload(int int_1, int int_2) {
        ChunkPos chunkPos = new ChunkPos(int_1, int_2);
        WorldChunk chunk = chunkMap.get(chunkPos);
        if (isChunkValid(chunk, int_1, int_2)) {
            chunkMap.remove(chunkPos);
            world.unloadBlockEntities(chunk);
        }
    }
    
    @Override
    public BlockView getWorld() {
        return this.world;
    }
    
    @Nullable
    public WorldChunk loadChunkFromPacket(
        World world_1,
        int x,
        int z,
        PacketByteBuf packetByteBuf_1,
        CompoundTag compoundTag_1,
        int mask,
        boolean isFullChunk
    ) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        WorldChunk chunk = chunkMap.get(chunkPos);
        if (!isChunkValid(chunk, x, z)) {
            if (!isFullChunk) {
                LOGGER.warn(
                    "Ignoring chunk since we don't have complete data: {}, {}",
                    x,
                    z
                );
                return null;
            }
            
            chunk = new WorldChunk(
                world_1,
                new ChunkPos(x, z),
                new Biome[256]
            );
            chunk.loadFromPacket(packetByteBuf_1, compoundTag_1, mask, isFullChunk);
            chunkMap.put(chunkPos, chunk);
            
            world.unloadBlockEntities(chunk);//TODO wrong?
        }
        else {
            if (isFullChunk) {
                Helper.err(String.format(
                    "received full chunk while chunk is present. entity may duplicate %s %s",
                    chunk.getWorld().dimension.getType(),
                    chunk.getPos()
                ));
            }
            chunk.loadFromPacket(packetByteBuf_1, compoundTag_1, mask, isFullChunk);
        }
        
        ChunkSection[] chunkSections_1 = chunk.getSectionArray();
        LightingProvider lightingProvider_1 = this.getLightingProvider();
        lightingProvider_1.suppressLight(new ChunkPos(x, z), true);
        
        for (int int_5 = 0; int_5 < chunkSections_1.length; ++int_5) {
            ChunkSection chunkSection_1 = chunkSections_1[int_5];
            lightingProvider_1.updateSectionStatus(
                ChunkSectionPos.from(x, int_5, z),
                ChunkSection.isEmpty(chunkSection_1)
            );
        }
        
        return chunk;
    }
    
    @Override
    public void tick(BooleanSupplier booleanSupplier_1) {
    }
    
    @Override
    public void setChunkMapCenter(int int_1, int int_2) {
        //do nothing
    }
    
    @Override
    public void updateLoadDistance(int int_1) {
        //do nothing
    }
    
    private static int getLoadDistance(int int_1) {
        return Math.max(2, int_1) + 3;
    }
    
    @Override
    public String getStatus() {
        return "Client Chunk Manager Hacked by Immersive Portals Mod";
    }
    
    @Override
    public ChunkGenerator<?> getChunkGenerator() {
        return null;
    }
    
    @Override
    public void onLightUpdate(LightType lightType_1, ChunkSectionPos chunkSectionPos_1) {
        MinecraftClient.getInstance().worldRenderer.scheduleBlockRender(
            chunkSectionPos_1.getChunkX(),
            chunkSectionPos_1.getChunkY(),
            chunkSectionPos_1.getChunkZ()
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
            MathHelper.floor(entity_1.x) >> 4,
            MathHelper.floor(entity_1.z) >> 4
        );
    }
    
    // $FF: synthetic method
    @Nullable
    @Override
    public Chunk getChunk(int var1, int var2, ChunkStatus var3, boolean var4) {
        WorldChunk worldChunk_1 = chunkMap.get(new ChunkPos(var1, var2));
        if (isChunkValid(worldChunk_1, var1, var2)) {
            return worldChunk_1;
        }
        
        return var4 ? this.emptyChunk : null;
    }
    
    public int getChunkNum() {
        return chunkMap.size();
    }
    
}
