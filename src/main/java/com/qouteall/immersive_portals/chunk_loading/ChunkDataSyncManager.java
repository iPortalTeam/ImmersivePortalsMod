package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.client.network.packet.UnloadChunkS2CPacket;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.stream.Stream;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
public class ChunkDataSyncManager {
    
    public ChunkDataSyncManager() {
        Globals.chunkTracker.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        Globals.chunkTracker.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
    }
    
    private void onBeginWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            return;
        }
        
        Chunk chunk = Helper.getServer()
            .getWorld(chunkPos.dimension)
            .getChunk(chunkPos.x, chunkPos.z);
        player.networkHandler.sendPacket(
            RedirectedMessageManager.createRedirectedMessage(
                chunkPos.dimension,
                new ChunkDataS2CPacket(
                    ((WorldChunk) chunk),
                    65535
                )
            )
        );
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            return;
        }
        
        player.networkHandler.sendPacket(
            RedirectedMessageManager.createRedirectedMessage(
                chunkPos.dimension,
                new UnloadChunkS2CPacket(
                    chunkPos.x, chunkPos.z
                )
            )
        );
    }
    
    private boolean isChunkManagedByVanilla(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        if (player.dimension != chunkPos.dimension) {
            return false;
        }
        
        ChunkManager chunkManager = Helper.getServer().getWorld(chunkPos.dimension).getChunkManager();
        Stream<ServerPlayerEntity> playersWatchingChunk =
            ((ServerChunkManager) chunkManager).threadedAnvilChunkStorage.getPlayersWatchingChunk(
                chunkPos.getChunkPos(), true
            );
        return playersWatchingChunk
            .anyMatch(playerEntity -> playerEntity == player);
    }
}
