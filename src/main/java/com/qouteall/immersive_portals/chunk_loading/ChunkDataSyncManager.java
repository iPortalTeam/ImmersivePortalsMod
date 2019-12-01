package com.qouteall.immersive_portals.chunk_loading;

import com.mojang.datafixers.util.Either;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.client.network.packet.LightUpdateS2CPacket;
import net.minecraft.client.network.packet.UnloadChunkS2CPacket;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.concurrent.CompletableFuture;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
//it is not multi-threaded like vanilla
public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    public ChunkDataSyncManager() {
        SGlobal.chunkTrackingGraph.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        SGlobal.chunkTrackingGraph.endWatchChunkSignal.connectWithWeakRef(
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
    
        if (SGlobal.chunkTrackingGraph.isChunkDataSent(player, chunkPos)) {
            return;
        }
    
        McHelper.getServer().getProfiler().push("begin_watch");
    
        SGlobal.chunkTrackingGraph.onChunkDataSent(player, chunkPos);
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(chunkPos.dimension);
    
        if (SGlobal.isChunkLoadingMultiThreaded) {
            sendPacketMultiThreaded(player, chunkPos, ieStorage);
        }
        else {
            sendPacketNormally(player, chunkPos, ieStorage);
        }
    
        McHelper.getServer().getProfiler().pop();
    }
    
    private void sendPacketMultiThreaded(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        ModMain.serverTaskList.addTask(() -> {
            ChunkHolder chunkHolder = ieStorage.getChunkHolder_(chunkPos.getChunkPos().toLong());
            if (chunkHolder == null) {
                //TODO cleanup it
                SGlobal.chunkTrackingGraph.setIsLoadedByPortal(
                    chunkPos.dimension,
                    chunkPos.getChunkPos(),
                    true
                );
                return false;
            }
            
            CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future = chunkHolder.createFuture(
                ChunkStatus.FULL,
                ((ThreadedAnvilChunkStorage) ieStorage)
            );
            
            future.thenAcceptAsync(either -> {
                ModMain.serverTaskList.addTask(() -> {
                    sendWatchPackets(player, chunkPos, ieStorage);
                    return true;
                });
            });
            
            return true;
        });
    }
    
    private void sendPacketNormally(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        sendWatchPackets(player, chunkPos, ieStorage);
    }
    
    private void sendWatchPackets(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        McHelper.getServer().getProfiler().push("send_chunk_data");
    
        Chunk chunk = McHelper.getServer()
            .getWorld(chunkPos.dimension)
            .getChunk(chunkPos.x, chunkPos.z);
        assert chunk != null;
        assert !(chunk instanceof EmptyChunk);
        player.networkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                chunkPos.dimension,
                new ChunkDataS2CPacket(
                    ((WorldChunk) chunk),
                    65535
                )
            )
        );
        
        player.networkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                chunkPos.dimension,
                new LightUpdateS2CPacket(
                    chunkPos.getChunkPos(),
                    ieStorage.getLightingProvider()
                )
            )
        );
    
        //update the entity trackers
        ((ThreadedAnvilChunkStorage) ieStorage).updateCameraPosition(player);
    
        McHelper.getServer().getProfiler().pop();
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            return;
        }
        
        //do not send unload packet instantly
        //watch for a period of time.
        //if player still needs the chunk, stop unloading.
    
        sendUnloadPacket(player, chunkPos);
    }
    
    public void sendUnloadPacket(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        if (isChunkManagedByVanilla(player, chunkPos)) {
            //give up unloading
            return;
        }
    
        if (SGlobal.chunkTrackingGraph.isPlayerWatchingChunk(player, chunkPos)) {
            //give up unloading
            return;
        }
        
        player.networkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                chunkPos.dimension,
                new UnloadChunkS2CPacket(
                    chunkPos.x, chunkPos.z
                )
            )
        );
    }
    
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        SGlobal.chunkTrackingGraph.onPlayerRespawn(oldPlayer);
    
        McHelper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.threadedAnvilChunkStorage;
                storage.onPlayerRespawn(oldPlayer);
            });
    }
    
    public static boolean isChunkManagedByVanilla(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        if (player.dimension != chunkPos.dimension) {
            return false;
        }
    
        int watchDistance = ChunkTrackingGraph.getRenderDistanceOnServer();
        
        //NOTE do not use entity.chunkX
        //it's not updated
        
        ChunkPos playerChunkPos = new ChunkPos(player.getBlockPos());
        
        int chebyshevDistance = Math.max(
            Math.abs(playerChunkPos.x - chunkPos.x),
            Math.abs(playerChunkPos.z - chunkPos.z)
        );
        
        return chebyshevDistance <= watchDistance;
    }
}
