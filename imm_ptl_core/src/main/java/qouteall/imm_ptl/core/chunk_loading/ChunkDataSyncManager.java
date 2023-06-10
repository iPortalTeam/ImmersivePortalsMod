package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;

import java.util.function.Supplier;

public class ChunkDataSyncManager {
    
    private static final int unloadWaitingTickTime = 20 * 10;
    
    private static final boolean debugLightStatus = true;
    
    public ChunkDataSyncManager() {
        NewChunkTrackingGraph.beginWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onBeginWatch
        );
        NewChunkTrackingGraph.endWatchChunkSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onEndWatch
        );
        
        DynamicDimensionsImpl.beforeRemovingDimensionSignal.connectWithWeakRef(
            this, ChunkDataSyncManager::onDimensionRemove
        );
    }
    
    /**
     * @link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)
     */
    private void onBeginWatch(ServerPlayer player, DimensionalChunkPos chunkPos) {
        MiscHelper.getServer().getProfiler().push("begin_watch");
        
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(chunkPos.dimension);
        
        sendChunkDataPacketNow(player, chunkPos, ieStorage);
        
        MiscHelper.getServer().getProfiler().pop();
    }
    
    private void sendChunkDataPacketNow(
        ServerPlayer player,
        DimensionalChunkPos chunkPos,
        IEThreadedAnvilChunkStorage ieStorage
    ) {
        ChunkHolder chunkHolder = ieStorage.ip_getChunkHolder(chunkPos.getChunkPos().toLong());
        
        ThreadedLevelLightEngine lightingProvider = ieStorage.ip_getLightingProvider();
        
        if (chunkHolder != null) {
            LevelChunk chunk = chunkHolder.getTickingChunk();
            if (chunk != null) {
                MiscHelper.getServer().getProfiler().push("ptl_create_chunk_packet");
                
                PacketRedirection.sendRedirectedMessage(
                    player,
                    chunkPos.dimension,
                    new ClientboundLevelChunkWithLightPacket(((LevelChunk) chunk), lightingProvider, null, null)
                );
                
                ieStorage.ip_updateEntityTrackersAfterSendingChunkPacket(chunk, player);
                
                MiscHelper.getServer().getProfiler().pop();
                
                return;
            }
        }
        //if the chunk is not present then the packet will be sent when chunk is ready
    }
    
    /**
     * @link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)
     */
    public void onChunkProvidedDeferred(LevelChunk chunk) {
        ResourceKey<Level> dimension = chunk.getLevel().dimension();
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(dimension);
        ThreadedLevelLightEngine lightingProvider = ieStorage.ip_getLightingProvider();
        
        MiscHelper.getServer().getProfiler().push("ptl_create_chunk_packet");
        
        Supplier<Packet> chunkDataPacketRedirected = Helper.cached(
            () -> PacketRedirection.createRedirectedMessage(
                dimension,
                new ClientboundLevelChunkWithLightPacket(((LevelChunk) chunk), lightingProvider, null, null)
            )
        );
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunk.getPos().x, chunk.getPos().z
        ).forEach(player -> {
            player.connection.send(chunkDataPacketRedirected.get());
            
            ieStorage.ip_updateEntityTrackersAfterSendingChunkPacket(chunk, player);
        });
        
        MiscHelper.getServer().getProfiler().pop();
    }
    
    private void onEndWatch(ServerPlayer player, DimensionalChunkPos chunkPos) {
        
        player.connection.send(
            PacketRedirection.createRedirectedMessage(
                chunkPos.dimension,
                new ClientboundForgetLevelChunkPacket(
                    chunkPos.x, chunkPos.z
                )
            )
        );
    }
    
    // if the player object is recreated, input the old player
    public void removePlayerFromChunkTrackersAndEntityTrackers(ServerPlayer oldPlayer) {
        MiscHelper.getServer().getAllLevels()
            .forEach(world -> {
                ServerChunkCache chunkManager = (ServerChunkCache) world.getChunkSource();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.chunkMap;
                storage.ip_onPlayerUnload(oldPlayer);
            });
        
        NewChunkTrackingGraph.forceRemovePlayer(oldPlayer);
    }
    
    @Deprecated
    public void removePlayerFromEntityTrackersWithoutSendingPacket(ServerPlayer player) {
        MiscHelper.getServer().getAllLevels()
            .forEach(world -> {
                ServerChunkCache chunkManager = (ServerChunkCache) world.getChunkSource();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.chunkMap;
                storage.ip_onPlayerDisconnected(player);
            });
    }
    
    public void onDimensionRemove(ResourceKey<Level> dimension) {
        ServerLevel world = McHelper.getServerWorld(dimension);
        
        ServerChunkCache chunkManager = (ServerChunkCache) world.getChunkSource();
        IEThreadedAnvilChunkStorage storage =
            (IEThreadedAnvilChunkStorage) chunkManager.chunkMap;
        storage.ip_onDimensionRemove();
        
        NewChunkTrackingGraph.forceRemoveDimension(dimension);
    }
}
