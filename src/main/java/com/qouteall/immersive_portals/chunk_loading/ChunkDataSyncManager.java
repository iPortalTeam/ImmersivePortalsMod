package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.client.network.packet.LightUpdateS2CPacket;
import net.minecraft.client.network.packet.UnloadChunkS2CPacket;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    public ChunkDataSyncManager() {
        Globals.chunkTracker.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        Globals.chunkTracker.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}
     */
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
        
        player.networkHandler.sendPacket(
            RedirectedMessageManager.createRedirectedMessage(
                chunkPos.dimension,
                new LightUpdateS2CPacket(
                    chunkPos.getChunkPos(),
                    Helper.getIEStorage(chunkPos.dimension).getLightingProvider()
                )
            )
        );
        
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            return;
        }
        
        //do not send unload packet instantly
        //watch for a period of time.
        //if player still needs the chunk, stop unloading.
    
        manageToSendUnloadPacket(player, chunkPos);
    }
    
    public void manageToSendUnloadPacket(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        long startTime = Helper.getServerGameTime();
        
        ModMain.serverTaskList.addTask(() -> {
            if (isChunkManagedByVanilla(player, chunkPos)) {
                //give up unloading
                return true;
            }
            
            if (Globals.chunkTracker.isPlayerWatchingChunk(player, chunkPos)) {
                //give up unloading
                return true;
            }
    
            if (Helper.getServerGameTime() - startTime < unloadWaitingTickTime) {
                //retry at the next tick
                return false;
            }
            
            player.networkHandler.sendPacket(
                RedirectedMessageManager.createRedirectedMessage(
                    chunkPos.dimension,
                    new UnloadChunkS2CPacket(
                        chunkPos.x, chunkPos.z
                    )
                )
            );
            
            return true;
        });
    }
    
    private boolean isChunkManagedByVanilla(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        if (player.dimension != chunkPos.dimension) {
            return false;
        }
    
        int watchDistance = ChunkTracker.getRenderDistanceOnServer();
        
        int chebyshevDistance = Math.max(
            Math.abs(player.chunkX - chunkPos.x),
            Math.abs(player.chunkZ - chunkPos.z)
        );
    
        return chebyshevDistance <= watchDistance;
    }
}
