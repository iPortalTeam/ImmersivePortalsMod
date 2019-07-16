package com.qouteall.immersive_portals.chunk_loading;

import net.minecraft.client.network.packet.EntitiesDestroyS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Deprecated
public class MyEntityTracker {
    public static class Unit {
        Entity entity;
        EntityTrackerEntry trackerEntry;
        Set<ServerPlayerEntity> viewers = new HashSet<>();
        
        public Unit(
            Entity entity,
            EntityTrackerEntry trackerEntry
        ) {
            this.entity = entity;
            this.trackerEntry = trackerEntry;
        }
    }
    
    private Map<DimensionType, Map<Entity, Unit>> trackers = new HashMap<>();
    
    public MyEntityTracker() {
    }
    
    private Map<Entity, Unit> getTrackerMap(DimensionType dimensionType) {
        return trackers.computeIfAbsent(
            dimensionType,
            k -> new HashMap<>()
        );
    }
    
    private boolean isManagedByVanilla(Entity entity, ServerPlayerEntity player) {
        return entity.dimension == player.dimension &&
            entity.getPos().squaredDistanceTo(player.getPos()) < 64 * 64;
    }
    
    public void onEntityLoad(DimensionType dimension, Entity entity) {
        if (entity instanceof EnderDragonPart || entity instanceof LightningEntity) {
            return;
        }
        
        assert entity.world.dimension.getType() == dimension;
        
        Unit unit = new Unit(
            entity,
            new EntityTrackerEntry(
                ((ServerWorld) entity.world),
                entity,
                entity.getType().getTrackTickInterval(),
                entity.getType().alwaysUpdateVelocity(),
                packet -> onEntityPacketSend(entity, packet)
            )
        );
        
    }
    
    public void onEntityUnload(Entity entity) {
        onEntityPacketSend(
            entity,
            new EntitiesDestroyS2CPacket(entity.getEntityId())
        );
    }
    
    public void onEntityPacketSend(
        Entity entity,
        Packet packet
    ) {
        Unit unit = getTrackerMap(entity.dimension).get(entity);
        if (unit == null) {
            return;
        }
        unit.viewers.forEach(playerEntity -> playerEntity.networkHandler.sendPacket(
            RedirectedMessageManager.createRedirectedMessage(
                entity.dimension,
                packet
            )
        ));
    }
}
