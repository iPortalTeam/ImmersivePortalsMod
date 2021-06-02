package com.qouteall.hiding_in_the_bushes.sodium_compatibility;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.WorldChunk;

import java.util.BitSet;

public class ClientChunkManagerWithSodium extends MyClientChunkManager implements ChunkStatusListenerManager {
    private ChunkStatusListener listener;
    
    public ClientChunkManagerWithSodium(ClientWorld clientWorld, int loadDistance) {
        super(clientWorld, loadDistance);
    }
    
    @Override
    public void setListener(ChunkStatusListener chunkStatusListener) {
        listener = chunkStatusListener;
    }
    
    @Override
    public WorldChunk loadChunkFromPacket(
        int x, int z, BiomeArray biomes,
        PacketByteBuf buf, NbtCompound nbt, BitSet bitSet
    ) {
        WorldChunk worldChunk = super.loadChunkFromPacket(x, z, biomes, buf, nbt, bitSet);
        
        if (listener != null) {
            listener.onChunkAdded(worldChunk.getPos().x, worldChunk.getPos().z);
        }
        
        return worldChunk;
    }
    
    @Override
    public void unload(int x, int z) {
        synchronized (chunkMap) {
            ChunkPos chunkPos = new ChunkPos(x, z);
            WorldChunk worldChunk = chunkMap.get(chunkPos.toLong());
            if (positionEquals(worldChunk, x, z)) {
                chunkMap.remove(chunkPos.toLong());
                
                O_O.postClientChunkUnloadEvent(worldChunk);
                world.unloadBlockEntities(worldChunk);
                clientChunkUnloadSignal.emit(worldChunk);
                
                if (listener != null) {
                    listener.onChunkRemoved(x, z);
                }
            }
        }
    }
}
