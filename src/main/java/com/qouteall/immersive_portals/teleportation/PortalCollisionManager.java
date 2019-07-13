package com.qouteall.immersive_portals.teleportation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.Signal;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.portal_entity.Portal;
import net.minecraft.entity.Entity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Deprecated
public class PortalCollisionManager {
    //there is not multi, bi-directional map
    public final HashMap<Entity, Portal> entityToCollidingPortal = new HashMap<>();
    public final Multimap<Portal, Entity> portalToCollidingEntity = HashMultimap.create();
    
    public final SignalBiArged<Entity, Portal> startCollidingSignal = new SignalBiArged<>();
    public final SignalBiArged<Entity, Portal> leavePortalSignal = new SignalBiArged<>();
    public final SignalBiArged<Entity, Portal> goInsidePortalSignal = new SignalBiArged<>();
    
    public PortalCollisionManager(Signal tickSignal) {
        tickSignal.connectWithWeakRef(this, PortalCollisionManager::tick);
    }
    
    public void onPortalTick(Portal portal) {
        Set<Entity> oldCollidingEntity = new HashSet<>(portalToCollidingEntity.get(portal));
        Set<Entity> newCollidingEntity = portal.world.getEntities(
            Entity.class,
            portal.getPortalCollisionBox()
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            e -> portal.isInFrontOfPortal(Helper.lastTickPosOf(e))
        ).collect(Collectors.toSet());
        
        Helper.compareOldAndNew(
            oldCollidingEntity,
            newCollidingEntity,
            e -> stopColliding(e, portal),
            e -> startColliding(e, portal)
        );
        
        portalToCollidingEntity.get(portal).stream().filter(
            e -> !portal.isInFrontOfPortal(e.getPos())
        ).collect(
            Collectors.toCollection(ArrayDeque::new)
        ).forEach(
            e -> entityGoInsidePortal(e, portal)
        );
        
    }
    
    private boolean isValid() {
        return entityToCollidingPortal.entrySet().stream()
            .allMatch(
                entry -> portalToCollidingEntity.containsEntry(
                    entry.getValue(), entry.getKey()
                )
            ) &&
            portalToCollidingEntity.entries().stream()
                .allMatch(
                    entry ->
                        entityToCollidingPortal.get(entry.getValue()) == entry.getKey()
                );
    }
    
    private void validate() {
        if (!isValid()) {
            throw new AssertionError();
        }
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
        
        startCollidingSignal.emit(entity, portal);
    }
    
    private void stopColliding(Entity entity, Portal portal) {
        if (!entityToCollidingPortal.containsKey(entity)) {
            throw new AssertionError();
        }
        if (!portalToCollidingEntity.get(portal).contains(entity)) {
            throw new AssertionError();
        }
        
        entityToCollidingPortal.remove(entity);
        portalToCollidingEntity.remove(portal, entity);
        
        leavePortalSignal.emit(entity, portal);
    }
    
    private void entityGoInsidePortal(Entity entity, Portal portal) {
        if (!entityToCollidingPortal.containsKey(entity)) {
            throw new AssertionError();
        }
        if (!portalToCollidingEntity.get(portal).contains(entity)) {
            throw new AssertionError();
        }
        
        entityToCollidingPortal.remove(entity);
        portalToCollidingEntity.remove(portal, entity);
        
        goInsidePortalSignal.emit(entity, portal);
    }
    
    private void purge() {
        //delete entries about removed entities
        //do not modify it while traversing it
        
        entityToCollidingPortal.keySet().stream()
            .filter(entity -> entity.removed)
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(entityToCollidingPortal::remove);
        
        portalToCollidingEntity.entries().stream()
            .filter(entry -> entry.getValue().removed)
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(entry ->
                portalToCollidingEntity.remove(entry.getKey(), entry.getValue())
            );
    }
}
