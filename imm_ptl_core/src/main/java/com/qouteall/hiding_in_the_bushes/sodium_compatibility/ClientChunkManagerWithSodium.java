package com.qouteall.hiding_in_the_bushes.sodium_compatibility;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.WorldChunk;

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
    public WorldChunk loadChunkFromPacket(int x, int z, BiomeArray biomeArray, PacketByteBuf packetByteBuf, CompoundTag compoundTag, int k, boolean forceCreate) {
        // No need to dispatch chunk load event here as it is called in `super`
        WorldChunk worldChunk = super.loadChunkFromPacket(x, z, biomeArray, packetByteBuf, compoundTag, k, forceCreate);
        
        if (listener != null) {
            listener.onChunkAdded(worldChunk.getPos().x, worldChunk.getPos().z);
        }
        
        return worldChunk;
    }
    
    @Override
    public void unload(int x, int z) {
        synchronized (chunkMap) {
            ChunkPos chunkPos = new ChunkPos(x, z);
            WorldChunk worldChunk_1 = chunkMap.get(chunkPos.toLong());
            if (positionEquals(worldChunk_1, x, z)) {
                chunkMap.remove(chunkPos.toLong());
                
                O_O.postClientChunkUnloadEvent(this.world, worldChunk_1);
                
                if (listener != null) {
                    listener.onChunkRemoved(x, z);
                }
            }
        }
    }
}
