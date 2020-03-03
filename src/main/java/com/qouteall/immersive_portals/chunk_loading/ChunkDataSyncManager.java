package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import com.qouteall.modloader_agnostic_api.MyNetwork;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Validate;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
//it is not multi-threaded like vanilla
public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    public ChunkDataSyncManager() {
        NewChunkTrackingGraph.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        NewChunkTrackingGraph.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}
     */
    private void onBeginWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        McHelper.getServer().getProfiler().push("begin_watch");
    
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(chunkPos.dimension);
    
        sendChunkDataPacketNow(player, chunkPos, ieStorage);
    
        McHelper.getServer().getProfiler().pop();
    }
    
    private void sendChunkDataPacketNow(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        ChunkHolder chunkHolder = ieStorage.getChunkHolder_(chunkPos.getChunkPos().toLong());
        if (chunkHolder != null) {
            WorldChunk chunk = chunkHolder.getWorldChunk();
            if (chunk != null) {
                doSendWatchPackets(
                    player,
                    chunkPos,
                    ieStorage,
                    chunk
                );
            }
        }
        //if the chunk is not present then the packet will be sent when chunk is ready
        
    }
    
    public void onChunkProvidedDeferred(WorldChunk chunk) {
        DimensionType dimension = chunk.getWorld().dimension.getType();
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension,
            chunk.getPos().x,
            chunk.getPos().z
        ).forEach(player -> {
            doSendWatchPackets(
                player,
                new DimensionalChunkPos(
                    dimension,
                    chunk.getPos()
                ),
                McHelper.getIEStorage(dimension),
                chunk
            );
        });
        //TODO avoid recreating packet when sending to multiple players
    }
    
    private void doSendWatchPackets(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage,
        Chunk chunk
    ) {
        Validate.notNull(chunk);
        
        McHelper.getServer().getProfiler().push("send_chunk_data");
        
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
        
        //do not send unload packet instantly
        //watch for a period of time.
        //if player still needs the chunk, stop unloading.
    
        sendUnloadPacket(player, chunkPos);
    }
    
    public void sendUnloadPacket(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
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
        NewChunkTrackingGraph.forceRemovePlayer(oldPlayer);
    
        McHelper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.threadedAnvilChunkStorage;
                storage.onPlayerRespawn(oldPlayer);
            });
    }
    
}
