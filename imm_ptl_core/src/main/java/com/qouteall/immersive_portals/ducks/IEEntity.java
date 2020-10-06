package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.Entity;

public interface IEEntity {
    void notifyCollidingWithPortal(Entity portal);
    
    Portal getCollidingPortal();
    
    void tickCollidingPortal(float tickDelta);
    
    boolean isRecentlyCollidingWithPortal();
}
