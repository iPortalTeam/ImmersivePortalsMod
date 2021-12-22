package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
            clientWorld.getDimension().hasSkyLight()
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
                // wrong yarn name, also unloads entities
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
        int x, int z,
        PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer
    ) {
        long chunkPosLong = ChunkPos.toLong(x, z);
        
        WorldChunk worldChunk;
        
        synchronized (chunkMap) {
            worldChunk = chunkMap.get(chunkPosLong);
            ChunkPos chunkPos = new ChunkPos(x, z);
            if (!positionEquals(worldChunk, x, z)) {
                worldChunk = new WorldChunk(this.world, chunkPos);
                worldChunk.loadFromPacket(buf, nbt, consumer);
                chunkMap.put(chunkPosLong, worldChunk);
            }
            else {
                worldChunk.loadFromPacket(buf, nbt, consumer);
            }
        }
        
        // wrong yarn name. it loads entities
        this.world.resetChunkColor(new ChunkPos(x, z));
        
        O_O.postClientChunkLoadEvent(worldChunk);
        clientChunkLoadSignal.emit(worldChunk);
        
        return worldChunk;
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
