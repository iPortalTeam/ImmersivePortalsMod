package qouteall.imm_ptl.core.platform_specific.sodium_compatibility;

import it.unimi.dsi.fastutil.longs.LongCollection;
import me.jellysquid.mods.sodium.client.world.ClientChunkManagerExtended;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.ChunkData;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.chunk_loading.MyClientChunkManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.BitSet;
import java.util.function.Consumer;

public class ClientChunkManagerWithSodium extends MyClientChunkManager implements ClientChunkManagerExtended {
    private ChunkStatusListener listener;
    
    public ClientChunkManagerWithSodium(ClientWorld clientWorld, int loadDistance) {
        super(clientWorld, loadDistance);
    }
    
    @Override
    public void setListener(ChunkStatusListener chunkStatusListener) {
        listener = chunkStatusListener;
    }
    
    @Override
    public LongCollection getLoadedChunks() {
        return chunkMap.keySet();
    }
    
    @Override
    public WorldChunk loadChunkFromPacket(
        int x, int z,
        PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer
    ) {
        Validate.isTrue(MinecraftClient.getInstance().isOnThread());
        
        boolean wasLoaded = chunkMap.containsKey(ChunkPos.toLong(x, z));
        
        WorldChunk worldChunk = super.loadChunkFromPacket(x, z, buf, nbt, consumer);
        
        if (listener != null) {
            if (!wasLoaded) {
                listener.onChunkAdded(worldChunk.getPos().x, worldChunk.getPos().z);
            }
        }
        
        return worldChunk;
    }
    
    @Override
    public void unload(int x, int z) {
        Validate.isTrue(MinecraftClient.getInstance().isOnThread());
        
        boolean wasLoaded = chunkMap.containsKey(ChunkPos.toLong(x, z));
        
        ChunkPos chunkPos = new ChunkPos(x, z);
        WorldChunk worldChunk = chunkMap.get(chunkPos.toLong());
        if (positionEquals(worldChunk, x, z)) {
            chunkMap.remove(chunkPos.toLong());
            
            O_O.postClientChunkUnloadEvent(worldChunk);
            world.unloadBlockEntities(worldChunk);
            clientChunkUnloadSignal.emit(worldChunk);
            
            if (listener != null) {
                if (wasLoaded) {
                    listener.onChunkRemoved(x, z);
                }
            }
        }
        
    }
}
