package qouteall.imm_ptl.core.chunk_loading;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.ducks.IEThreadedAnvilChunkStorage;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import qouteall.q_misc_util.MiscHelper;

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
     * @link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)
     */
    private void onBeginWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        MiscHelper.getServer().getProfiler().push("begin_watch");
        
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(chunkPos.dimension);
        
        sendChunkDataPacketNow(player, chunkPos, ieStorage);
        
        MiscHelper.getServer().getProfiler().pop();
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
                MiscHelper.getServer().getProfiler().push("ptl_create_chunk_packet");
                
                player.networkHandler.sendPacket(
                    IPNetworking.createRedirectedMessage(
                        chunkPos.dimension,
                        new ChunkDataS2CPacket(((WorldChunk) chunk))
                    )
                );
                
                LightUpdateS2CPacket lightPacket = new LightUpdateS2CPacket(
                    chunkPos.getChunkPos(),
                    ieStorage.getLightingProvider(),
                    null, null,
                    true
                );
                player.networkHandler.sendPacket(
                    IPNetworking.createRedirectedMessage(
                        chunkPos.dimension,
                        lightPacket
                    )
                );
                if (IPGlobal.lightLogging) {
                    Helper.log(String.format(
                        "light sent immediately %s %d %d %d %d",
                        chunk.getWorld().getRegistryKey().getValue(),
                        chunk.getPos().x, chunk.getPos().z,
                        lightPacket.getBlockLightMask(), lightPacket.getFilledBlockLightMask())
                    );
                }
                
                ieStorage.updateEntityTrackersAfterSendingChunkPacket(chunk, player);
                
                MiscHelper.getServer().getProfiler().pop();
                
                return;
            }
        }
        //if the chunk is not present then the packet will be sent when chunk is ready
    }
    
    /**
     * @link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)
     */
    public void onChunkProvidedDeferred(WorldChunk chunk) {
        RegistryKey<World> dimension = chunk.getWorld().getRegistryKey();
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(dimension);
        
        MiscHelper.getServer().getProfiler().push("ptl_create_chunk_packet");
        
        Supplier<Packet> chunkDataPacketRedirected = Helper.cached(
            () -> IPNetworking.createRedirectedMessage(
                dimension,
                new ChunkDataS2CPacket(((WorldChunk) chunk))
            )
        );
        
        Supplier<Packet> lightPacketRedirected = Helper.cached(
            () -> {
                LightUpdateS2CPacket lightPacket = new LightUpdateS2CPacket(
                    chunk.getPos(), ieStorage.getLightingProvider(),
                    null,null,
                    true
                );
                if (IPGlobal.lightLogging) {
                    Helper.log(String.format(
                        "light sent deferred %s %d %d %d %d",
                        chunk.getWorld().getRegistryKey().getValue(),
                        chunk.getPos().x, chunk.getPos().z,
                        lightPacket.getBlockLightMask(), lightPacket.getFilledBlockLightMask())
                    );
                }
                return IPNetworking.createRedirectedMessage(
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
        
        MiscHelper.getServer().getProfiler().pop();
    }
    
    private void onEndWatch(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        
        player.networkHandler.sendPacket(
            IPNetworking.createRedirectedMessage(
                chunkPos.dimension,
                new UnloadChunkS2CPacket(
                    chunkPos.x, chunkPos.z
                )
            )
        );
    }
    
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        MiscHelper.getServer().getWorlds()
            .forEach(world -> {
                ServerChunkManager chunkManager = (ServerChunkManager) world.getChunkManager();
                IEThreadedAnvilChunkStorage storage =
                    (IEThreadedAnvilChunkStorage) chunkManager.threadedAnvilChunkStorage;
                storage.onPlayerRespawn(oldPlayer);
            });
        
        NewChunkTrackingGraph.forceRemovePlayer(oldPlayer);
    }
    
}
