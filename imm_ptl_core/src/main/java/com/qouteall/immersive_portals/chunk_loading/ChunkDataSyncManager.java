package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

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
                McHelper.getServer().getProfiler().push("ptl_create_chunk_packet");
                
                player.networkHandler.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        chunkPos.dimension,
                        new ChunkDataS2CPacket(((WorldChunk) chunk), 65535)
                    )
                );
                
                LightUpdateS2CPacket lightPacket = new LightUpdateS2CPacket(
                    chunkPos.getChunkPos(),
                    ieStorage.getLightingProvider(),
                    true
                );
                player.networkHandler.sendPacket(
                    MyNetwork.createRedirectedMessage(
                        chunkPos.dimension,
                        lightPacket
                    )
                );
                if (Global.lightLogging) {
                    Helper.log(String.format(
                        "light sent immediately %s %d %d %d %d",
                        chunk.getWorld().getRegistryKey().getValue(),
                        chunk.getPos().x, chunk.getPos().z,
                        lightPacket.getBlockLightMask(), lightPacket.getFilledBlockLightMask())
                    );
                }
                
                ieStorage.updateEntityTrackersAfterSendingChunkPacket(chunk, player);
                
                McHelper.getServer().getProfiler().pop();
                
                return;
            }
        }
        //if the chunk is not present then the packet will be sent when chunk is ready
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}r
     */
    public void onChunkProvidedDeferred(WorldChunk chunk) {
        RegistryKey<World> dimension = chunk.getWorld().getRegistryKey();
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(dimension);
        
        McHelper.getServer().getProfiler().push("ptl_create_chunk_packet");
        
        Supplier<Packet> chunkDataPacketRedirected = Helper.cached(
            () -> MyNetwork.createRedirectedMessage(
                dimension,
                new ChunkDataS2CPacket(((WorldChunk) chunk), 65535)
            )
        );
        
        Supplier<Packet> lightPacketRedirected = Helper.cached(
            () -> {
                LightUpdateS2CPacket lightPacket = new LightUpdateS2CPacket(chunk.getPos(), ieStorage.getLightingProvider(), true);
                if (Global.lightLogging) {
                    Helper.log(String.format(
                        "light sent deferred %s %d %d %d %d",
                        chunk.getWorld().getRegistryKey().getValue(),
                        chunk.getPos().x, chunk.getPos().z,
                        lightPacket.getBlockLightMask(), lightPacket.getFilledBlockLightMask())
                    );
                }
                return MyNetwork.createRedirectedMessage(
                    dimension,
                    lightPacket
                );
            }
        );
        
        NewChunkTrackingGraph.getPlayersViewingChunk(
            dimension, chunk.getPos().x, chunk.getPos().z
        ).forEach(player -> {
            player.networkHandler.sendPacket(chunkDataPacketRedirected.get());
            
            player.networkHandler.sendPacket(lightPacketRedirected.get());
            
            ieStorage.updateEntityTrackersAfterSendingChunkPacket(chunk, player);
        });
        
        McHelper.getServer().getProfiler().pop();
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        
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
        McHelper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.threadedAnvilChunkStorage;
                storage.onPlayerRespawn(oldPlayer);
            });
        
        NewChunkTrackingGraph.forceRemovePlayer(oldPlayer);
    }
    
}
