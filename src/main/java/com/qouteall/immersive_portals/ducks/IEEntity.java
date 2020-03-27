package com.qouteall.immersive_portals.ducks;

import com.qouteall.immersive_portals.portal.Portal;

public interface IEEntity {
    Portal getCollidingPortal();
    
    void tickCollidingPortal();
}
