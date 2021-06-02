package com.qouteall.immersive_portals.mc_utils;

import net.minecraft.entity.Entity;

// does not use EntityChangeListener to avoid messing with vanilla mechanics
public interface IPEntityEventListenableEntity {
    void ip_onEntityPositionUpdated();
    
    void ip_onRemoved(Entity.RemovalReason reason);
}
