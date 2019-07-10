package com.qouteall.immersive_portals.teleportation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerTeleportationManager {
    
    public final HashMap<Entity, Portal> entityToCollidingPortal = new HashMap<>();
    public final Multimap<Portal, Entity> portalToCollidingEntity = HashMultimap.create();
    
    public ServerTeleportationManager() {
        ModMain.serverTickSignal.connectWithWeakRef(this, ServerTeleportationManager::tick);
    }
    
    public void onPortalTick(Portal portal) {
        assert !portal.world.isClient;
        
        Set<Entity> oldCollidingEntity = new HashSet<>(portalToCollidingEntity.get(portal));
        Set<Entity> newCollidingEntity = new HashSet<>(
            portal.world.getEntities(
                Entity.class,
                portal.getPortalCollisionBox()
            )
        );
        
        oldCollidingEntity.stream()
            .filter(e -> !newCollidingEntity.contains(e))
            .forEach(e -> stopColliding(e, portal));
        
        newCollidingEntity.stream()
            .filter(e -> !oldCollidingEntity.contains(e))
            .forEach(e -> startColliding(e, portal));
    }
    
    private void tick() {
        purge();
    }
    
    private void startColliding(Entity entity, Portal portal) {
        assert !(entity instanceof Portal);
        assert !entityToCollidingPortal.containsKey(portal);
        assert !portalToCollidingEntity.get(portal).contains(entity);
        
        entityToCollidingPortal.put(entity, portal);
        portalToCollidingEntity.put(portal, entity);
        
    }
    
    private void stopColliding(Entity entity, Portal portal) {
        assert entityToCollidingPortal.containsKey(portal);
        assert portalToCollidingEntity.get(portal).contains(entity);
        
        entityToCollidingPortal.remove(entity);
        portalToCollidingEntity.remove(portal, entity);
        
        if (portal.isInFrontOfPortal(entity.getPos())) {
            //leave portal
            //do nothing
        }
        else {
            //go inside portal
            finishTeleporting(entity, portal);
        }
    }
    
    private void purge() {
        //delete entries about removed entities
        //do not modify it while traversing it
        
        entityToCollidingPortal.keySet().stream()
            .filter(entity -> entity.removed)
            .collect(Collectors.toList())
            .forEach(entityToCollidingPortal::remove);
        
        portalToCollidingEntity.entries().stream()
            .filter(entry -> entry.getValue().removed)
            .collect(Collectors.toList())
            .forEach(entry ->
                portalToCollidingEntity.remove(entry.getKey(), entry.getValue())
            );
    }
    
    private void finishTeleporting(Entity entity, Portal portal) {
        if (entity instanceof ServerPlayerEntity) {
            finishTeleportingPlayer(((ServerPlayerEntity) entity), portal);
        }
        else {
            assert false;
        }
    }
    
    /**
     * {@link ServerPlayerEntity#changeDimension(DimensionType)}
     */
    private void finishTeleportingPlayer(
        ServerPlayerEntity player,
        Portal portal
    ) {
        ServerWorld fromWorld = (ServerWorld) player.world;
        ServerWorld toWorld = Helper.getServer().getWorld(portal.dimensionTo);
        Vec3d newPos = portal.applyTransformationToPoint(player.getPos());
        
        if (fromWorld == toWorld) {
            player.setPosition(
                newPos.x,
                newPos.y,
                newPos.z
            );
            return;
        }
        
        fromWorld.removeEntity(player);
        player.removed = false;
        
        player.x = newPos.x;
        player.y = newPos.y;
        player.z = newPos.z;
        
        player.world = toWorld;
        player.dimension = toWorld.dimension.getType();
        toWorld.respawnPlayer(player);
        
        toWorld.checkChunk(player);
        
        Helper.getServer().getPlayerManager().sendWorldInfo(
            player, toWorld
        );
        
        player.interactionManager.setWorld(toWorld);
        
        int toDimensionId = toWorld.dimension.getType().getRawId();
        player.networkHandler.sendPacket(
            MyNetwork.createCustomPacketStc(
                new ICustomStcPacket() {
                    private static final long serialVersionUID = 2331L;
                    
                    @Override
                    public void handle() {
                        //this is invoked on client
                        Globals.clientTeleportationManager.finishTeleportingPlayer(
                            DimensionType.byRawId(toDimensionId),
                            newPos
                        );
                    }
                }
            )
        );
    }
}
