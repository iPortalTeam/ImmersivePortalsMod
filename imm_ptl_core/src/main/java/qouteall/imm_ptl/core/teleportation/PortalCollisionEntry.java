package qouteall.imm_ptl.core.teleportation;

import qouteall.imm_ptl.core.portal.Portal;

public class PortalCollisionEntry {
    public final Portal portal;
    public long activeTime;
    
    public PortalCollisionEntry(Portal portal, long activeTime) {
        this.portal = portal;
        this.activeTime = activeTime;
    }
    
}
