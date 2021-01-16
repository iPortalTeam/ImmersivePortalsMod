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
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.function.Supplier;

//the chunks near player are managed by vanilla
//we only manage the chunks that's seen by portal and not near player
//it is not multi-threaded like vanilla
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
                
                checkLight(chunk);
                
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
        
        //test
//        Helper.log("chunk not ready" + chunkPos);
    }
    
    private void checkLight(WorldChunk chunk) {
        if (Global.flushLightTasksBeforeSendingPacket) {
            ((ServerLightingProvider) ((ServerWorld) chunk.getWorld()).getLightingProvider()).tick();
        }

//        if (!debugLightStatus) {
//            return;
//        }
//
//        if (!chunk.isLightOn()) {
//            Helper.err("Sending light update when the light is not on " + chunk.getPos());
//            new Throwable().printStackTrace();
//        }
//
//        ChunkLightingView chunkLightingView =
//            ((ServerLightingProvider) chunk.getWorld().getLightingProvider()).get(LightType.BLOCK);
//
//        ChunkNibbleArray lightSection = ((ChunkBlockLightProvider) chunkLightingView).getLightSection(
//            ChunkSectionPos.from(chunk.getPos(), 0)
//        );
//
//        if (lightSection == null) {
//            Helper.err("Missing light " + chunk.getPos());
//            new Throwable().printStackTrace();
//        }
//
////        if (lightSection.isUninitialized()) {
////            Helper.err("light uninitialized " + chunk.getPos() + chunk.getWorld().getRegistryKey());
////            new Throwable().printStackTrace();
////        }
    }
    
    /**
     * {@link ThreadedAnvilChunkStorage#sendChunkDataPackets(ServerPlayerEntity, Packet[], WorldChunk)}r
     */
    public void onChunkProvidedDeferred(WorldChunk chunk) {
        RegistryKey<World> dimension = chunk.getWorld().getRegistryKey();
        IEThreadedAnvilChunkStorage ieStorage = McHelper.getIEStorage(dimension);
        
        checkLight(chunk);
        
        //test
//        Helper.log("deferred chunk " + chunk.getPos() + chunk.getWorld());
        
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
