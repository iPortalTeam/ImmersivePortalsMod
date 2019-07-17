package com.qouteall.immersive_portals.mixin;

import com.google.common.collect.Lists;
import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import com.qouteall.immersive_portals.exposer.IEEntityTracker;
import com.qouteall.immersive_portals.exposer.IEThreadedAnvilChunkStorage;
import com.qouteall.immersive_portals.my_util.Helper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.network.DebugRendererInfoManager;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.client.network.packet.EntityAttachS2CPacket;
import net.minecraft.client.network.packet.EntityPassengersSetS2CPacket;
import net.minecraft.client.network.packet.LightUpdateS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilChunkStorage implements IEThreadedAnvilChunkStorage {
    @Shadow
    int watchDistance;
    
    @Shadow
    @Final
    private ServerLightingProvider serverLightingProvider;
    
    @Shadow
    @Final
    private ServerWorld world;
    
    @Shadow
    protected abstract ChunkHolder getChunkHolder(long long_1);
    
    @Shadow
    @Final
    private Int2ObjectMap entityTrackers;
    
    @Override
    public int getWatchDistance() {
        Helper.log(entityTrackers.size());
        return watchDistance;
    }
    
    @Override
    public ServerWorld getWorld() {
        return world;
    }
    
    @Override
    public ServerLightingProvider getLightingProvider() {
        return serverLightingProvider;
    }
    
    @Override
    public ChunkHolder getChunkHolder_(long long_1) {
        return getChunkHolder(long_1);
    }
    
    /**
     * @author qouteall
     */
    @Overwrite
    private void sendChunkDataPackets(
        ServerPlayerEntity serverPlayerEntity_1,
        Packet<?>[] packets_1,
        WorldChunk worldChunk_1
    ) {
        if (packets_1[0] == null) {
            packets_1[0] = RedirectedMessageManager.createRedirectedMessage(
                world.dimension.getType(),
                new ChunkDataS2CPacket(worldChunk_1, 65535)
            );
            packets_1[1] = RedirectedMessageManager.createRedirectedMessage(
                world.dimension.getType(),
                new LightUpdateS2CPacket(
                    worldChunk_1.getPos(),
                    this.serverLightingProvider
                )
            );
        }
        
        serverPlayerEntity_1.sendInitialChunkPackets(
            worldChunk_1.getPos(),
            packets_1[0],
            packets_1[1]
        );
        
        DebugRendererInfoManager.method_19775(this.world, worldChunk_1.getPos());
        List<Entity> list_1 = Lists.newArrayList();
        List<Entity> list_2 = Lists.newArrayList();
        ObjectIterator var6 = this.entityTrackers.values().iterator();
        
        while (var6.hasNext()) {
            IEEntityTracker threadedAnvilChunkStorage$EntityTracker_1 = (IEEntityTracker) var6.next();
            Entity entity_1 = threadedAnvilChunkStorage$EntityTracker_1.getEntity_();
            if (entity_1 != serverPlayerEntity_1 && entity_1.chunkX == worldChunk_1.getPos().x && entity_1.chunkZ == worldChunk_1.getPos().z) {
                threadedAnvilChunkStorage$EntityTracker_1.updateCameraPosition_(serverPlayerEntity_1);
                if (entity_1 instanceof MobEntity && ((MobEntity) entity_1).getHoldingEntity() != null) {
                    list_1.add(entity_1);
                }
                
                if (!entity_1.getPassengerList().isEmpty()) {
                    list_2.add(entity_1);
                }
            }
        }
        
        Iterator var9;
        Entity entity_3;
        if (!list_1.isEmpty()) {
            var9 = list_1.iterator();
            
            while (var9.hasNext()) {
                entity_3 = (Entity) var9.next();
                serverPlayerEntity_1.networkHandler.sendPacket(
                    RedirectedMessageManager.createRedirectedMessage(
                        world.getDimension().getType(),
                        new EntityAttachS2CPacket(
                            entity_3,
                            ((MobEntity) entity_3).getHoldingEntity()
                        )
                    )
                );
            }
        }
        
        if (!list_2.isEmpty()) {
            var9 = list_2.iterator();
            
            while (var9.hasNext()) {
                entity_3 = (Entity) var9.next();
                serverPlayerEntity_1.networkHandler.sendPacket(
                    RedirectedMessageManager.createRedirectedMessage(
                        world.getDimension().getType(),
                        new EntityPassengersSetS2CPacket(entity_3)
                    )
                );
            }
        }
    }
}
