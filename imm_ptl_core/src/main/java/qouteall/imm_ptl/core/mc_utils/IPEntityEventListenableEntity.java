package qouteall.imm_ptl.core.mc_utils;

import net.minecraft.world.entity.Entity;

// does not use EntityChangeListener to avoid messing with vanilla mechanics
public interface IPEntityEventListenableEntity {
    void ip_onEntityPositionUpdated();
    
    void ip_onRemoved(Entity.RemovalReason reason);
}
