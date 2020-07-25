package com.qouteall.hiding_in_the_bushes.sodium_compatibility;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
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
    public WorldChunk loadChunkFromPacket(int x, int z, BiomeArray biomeArray, PacketByteBuf packetByteBuf, CompoundTag compoundTag, int k, boolean bl) {
        WorldChunk worldChunk = super.loadChunkFromPacket(x, z, biomeArray, packetByteBuf, compoundTag, k, bl);
    
        if(listener!=null) {
            listener.onChunkAdded(worldChunk.getPos().x, worldChunk.getPos().z);
        }
        
        return worldChunk;
    }
    
    @Override
    public void unload(int x, int z) {
        super.unload(x, z);
    
        if (listener != null) {
            listener.onChunkRemoved(x, z);
        }
    }
}
