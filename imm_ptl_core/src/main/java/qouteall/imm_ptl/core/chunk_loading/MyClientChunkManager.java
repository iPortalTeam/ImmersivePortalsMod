package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

// allow storing chunks that are far away from the player
@Environment(EnvType.CLIENT)
@IPVanillaCopy
public class MyClientChunkManager extends ClientChunkCache {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final LevelChunk emptyChunk;
    protected final LevelLightEngine lightingProvider;
    protected final ClientLevel world;
    
    protected final Long2ObjectLinkedOpenHashMap<LevelChunk> chunkMap =
        new Long2ObjectLinkedOpenHashMap<>();
    
    public static final SignalArged<LevelChunk> clientChunkLoadSignal = new SignalArged<>();
    public static final SignalArged<LevelChunk> clientChunkUnloadSignal = new SignalArged<>();
    
    public MyClientChunkManager(ClientLevel clientWorld, int loadDistance) {
        super(clientWorld, loadDistance);
        this.world = clientWorld;
        this.emptyChunk = new EmptyLevelChunk(
            clientWorld, new ChunkPos(0, 0),
            clientWorld.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.PLAINS)
        );
        this.lightingProvider = new LevelLightEngine(
            this,
            true,
            clientWorld.dimensionType().hasSkyLight()
        );
        
    }
    
    @Override
    public LevelLightEngine getLightEngine() {
        return this.lightingProvider;
    }
    
    @Override
    public void drop(int x, int z) {
        synchronized (chunkMap) {
            
            ChunkPos chunkPos = new ChunkPos(x, z);
            LevelChunk chunk = chunkMap.get(chunkPos.toLong());
            if (isValidChunk(chunk, x, z)) {
                chunkMap.remove(chunkPos.toLong());
                O_O.postClientChunkUnloadEvent(chunk);
                world.unload(chunk);
                clientChunkUnloadSignal.emit(chunk);
            }
        }
    }
    
    @Override
    public LevelChunk getChunk(int x, int z, ChunkStatus chunkStatus, boolean create) {
        // the profiler shows that this is not a hot spot
        synchronized (chunkMap) {
            LevelChunk chunk = chunkMap.get(ChunkPos.asLong(x, z));
            if (isValidChunk(chunk, x, z)) {
                return chunk;
            }
            
            return create ? this.emptyChunk : null;
        }
    }
    
    public boolean isChunkLoaded(int x,int z) {
        synchronized (chunkMap) {
            return chunkMap.containsKey(ChunkPos.asLong(x, z));
        }
    }
    
    @Override
    public BlockGetter getLevel() {
        return this.world;
    }
    
    @Override
    public LevelChunk replaceWithPacketData(
        int x, int z,
        FriendlyByteBuf buf, CompoundTag nbt,
        Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer
    ) {
        long chunkPosLong = ChunkPos.asLong(x, z);
        
        LevelChunk worldChunk;
        
        synchronized (chunkMap) {
            worldChunk = chunkMap.get(chunkPosLong);
            ChunkPos chunkPos = new ChunkPos(x, z);
            if (!isValidChunk(worldChunk, x, z)) {
                worldChunk = new LevelChunk(this.world, chunkPos);
                worldChunk.replaceWithPacketData(buf, nbt, consumer);
                chunkMap.put(chunkPosLong, worldChunk);
            }
            else {
                worldChunk.replaceWithPacketData(buf, nbt, consumer);
            }
        }
        
        this.world.onChunkLoaded(new ChunkPos(x, z));
        
        O_O.postClientChunkLoadEvent(worldChunk);
        clientChunkLoadSignal.emit(worldChunk);
        
        return worldChunk;
    }
    
    public List<LevelChunk> getCopiedChunkList() {
        synchronized (chunkMap) {
            return Arrays.asList(chunkMap.values().toArray(new LevelChunk[0]));
        }
    }
    
    @Override
    public void updateViewCenter(int x, int z) {
        //do nothing
    }
    
    @Override
    public void updateViewRadius(int r) {
        //do nothing
    }
    
    @Override
    public String gatherStats() {
        return "Client Chunks (ImmPtl) " + getLoadedChunksCount();
    }
    
    @Override
    public int getLoadedChunksCount() {
        synchronized (chunkMap) {
            return chunkMap.size();
        }
    }
    
    @Override
    public void onLightUpdate(LightLayer lightType, SectionPos chunkSectionPos) {
        ClientWorldLoader.getWorldRenderer(
            world.dimension()
        ).setSectionDirty(
            chunkSectionPos.x(),
            chunkSectionPos.y(),
            chunkSectionPos.z()
        );
    }
    
    protected static boolean isValidChunk(LevelChunk worldChunk, int x, int z) {
        if (worldChunk == null) {
            return false;
        }
        else {
            ChunkPos chunkPos = worldChunk.getPos();
            return chunkPos.x == x && chunkPos.z == z;
        }
    }
    
}
