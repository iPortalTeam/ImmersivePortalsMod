package com.qouteall.immersive_portals.teleportation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.portal_entity.Portal;
import javafx.util.Pair;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class PortalCollisionManager {
    public final HashMap<Entity, Portal> entityToCollidingPortal = new HashMap<>();
    public final Multimap<Portal, Entity> portalToCollidingEntity = HashMultimap.create();
    
    public final SignalBiArged<Entity, Portal> startCollidingSignal = new SignalBiArged<>();
    public final SignalBiArged<Entity, Portal> leavePortalSignal = new SignalBiArged<>();
    public final SignalBiArged<Entity, Portal> goInsidePortalSignal = new SignalBiArged<>();
    
    public PortalCollisionManager() {
        ModMain.serverTickSignal.connectWithWeakRef(this, PortalCollisionManager::tick);
    }
    
    public void onPortalTick(Portal portal) {
        Set<Entity> oldCollidingEntity = new HashSet<>(portalToCollidingEntity.get(portal));
        Set<Entity> newCollidingEntity = portal.world.getEntities(
            Entity.class,
            portal.getPortalCollisionBox()
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).collect(Collectors.toSet());
        
        oldCollidingEntity.stream()
            .filter(e -> !newCollidingEntity.contains(e))
            .forEach(e -> stopColliding(e, portal));
        
        newCollidingEntity.stream()
            .filter(e -> !oldCollidingEntity.contains(e))
            .forEach(e -> startColliding(e, portal));
        
        portalToCollidingEntity.get(portal).stream().filter(
            e -> !portal.isInFrontOfPortal(e.getPos())
        ).collect(
            Collectors.toList()
        ).forEach(
            e -> entityGoInsidePortal(e, portal)
        );
    }
    
    private void tick() {
        purge();
    }
    
    private void startColliding(Entity entity, Portal portal) {
        assert !(entity instanceof Portal);
        assert !entityToCollidingPortal.containsKey(portal);
        assert !portalToCollidingEntity.get(portal).contains(entity);
        
        if (portal.isInFrontOfPortal(Helper.lastTickPosOf(entity))) {
            entityToCollidingPortal.put(entity, portal);
            portalToCollidingEntity.put(portal, entity);
            
            startCollidingSignal.emit(entity, portal);
        }
    }
    
    private void stopColliding(Entity entity, Portal portal) {
        assert entityToCollidingPortal.containsKey(entity);
        assert portalToCollidingEntity.get(portal).contains(entity);
        
        entityToCollidingPortal.remove(entity);
        portalToCollidingEntity.remove(portal, entity);
        
        leavePortalSignal.emit(entity, portal);
    }
    
    private void entityGoInsidePortal(Entity entity, Portal portal) {
        assert entityToCollidingPortal.containsKey(entity);
        assert portalToCollidingEntity.get(portal).contains(entity);
        
        entityToCollidingPortal.remove(entity);
        portalToCollidingEntity.remove(portal, entity);
        
        goInsidePortalSignal.emit(entity, portal);
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
}
