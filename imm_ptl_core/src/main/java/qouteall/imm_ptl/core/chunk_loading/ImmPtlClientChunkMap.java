package qouteall.imm_ptl.core.chunk_loading;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Vanilla use a 2D array to store the chunk references on client and cannot store the chunks that are far from player.
 * This use a map to store the chunk references.
 */
@Environment(EnvType.CLIENT)
@IPVanillaCopy
public class ImmPtlClientChunkMap extends ClientChunkCache {
    private static final Logger LOGGER = LogManager.getLogger();
    
    protected final Long2ObjectLinkedOpenHashMap<LevelChunk> chunkMap =
        new Long2ObjectLinkedOpenHashMap<>();
    
    public static final SignalArged<LevelChunk> clientChunkLoadSignal = new SignalArged<>();
    public static final SignalArged<LevelChunk> clientChunkUnloadSignal = new SignalArged<>();
    
    public ImmPtlClientChunkMap(ClientLevel clientWorld, int loadDistance) {
        super(clientWorld, 1); // the chunk array is unused. make it small by passing 1 as load distance to super constructor
    }
    
    @Override
    public void drop(int x, int z) {
        synchronized (chunkMap) {
            
            ChunkPos chunkPos = new ChunkPos(x, z);
            LevelChunk chunk = chunkMap.get(chunkPos.toLong());
            if (isValidChunk(chunk, x, z)) {
                chunkMap.remove(chunkPos.toLong());
                O_O.postClientChunkUnloadEvent(chunk);
                this.level.unload(chunk);
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
    
    public boolean isChunkLoaded(int x, int z) {
        synchronized (chunkMap) {
            return chunkMap.containsKey(ChunkPos.asLong(x, z));
        }
    }
    
    @Override
    public void replaceBiomes(int x, int z, FriendlyByteBuf friendlyByteBuf) {
        long chunkPosLong = ChunkPos.asLong(x, z);
        
        LevelChunk worldChunk;
        
        synchronized (chunkMap) {
            worldChunk = chunkMap.get(chunkPosLong);
            ChunkPos chunkPos = new ChunkPos(x, z);
            if (!isValidChunk(worldChunk, x, z)) {
                LOGGER.error("Trying to replace biomes for missing chunk {} {}", x, z);
            }
            else {
                worldChunk.replaceBiomes(friendlyByteBuf);
            }
        }
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
                worldChunk = new LevelChunk(this.level, chunkPos);
                loadChunkDataFromPacket(buf, nbt, worldChunk, consumer);
                chunkMap.put(chunkPosLong, worldChunk);
            }
            else {
                loadChunkDataFromPacket(buf, nbt, worldChunk, consumer);
            }
        }
        
        this.level.onChunkLoaded(new ChunkPos(x, z));
        
        O_O.postClientChunkLoadEvent(worldChunk);
        clientChunkLoadSignal.emit(worldChunk);
        
        return worldChunk;
    }
    
    /**
     * {@link net.minecraft.core.IdMap#byIdOrThrow(int)}
     * {@link net.minecraft.world.level.chunk.LinearPalette#read(FriendlyByteBuf)}
     */
    private void loadChunkDataFromPacket(
        FriendlyByteBuf buf,
        CompoundTag nbt,
        LevelChunk worldChunk,
        Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer
    ) {
        try {
            worldChunk.replaceWithPacketData(buf, nbt, consumer);
        }
        catch (Exception e) {
            LOGGER.error(
                "Error deserializing chunk packet {} {}",
                worldChunk.getLevel().dimension().location(),
                worldChunk.getPos(),
                e
            );
            CHelper.printChat(
                Component
                    .literal("Failed to deserialize chunk packet. %s %s %s".formatted(
                        worldChunk.getLevel().dimension().location(),
                        worldChunk.getPos().x, worldChunk.getPos().z
                    ))
                    .append(Component.literal(" Report issue:"))
                    .append(McHelper.getLinkText(O_O.getIssueLink()))
                    .withStyle(ChatFormatting.RED)
            );
            
            throw new RuntimeException(e);
        }
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
            level.dimension()
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
