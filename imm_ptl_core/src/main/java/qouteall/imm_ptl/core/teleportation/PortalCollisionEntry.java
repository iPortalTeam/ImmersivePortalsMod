package qouteall.imm_ptl.core.teleportation;

import qouteall.imm_ptl.core.portal.Portal;

public class PortalCollisionEntry {
    public final Portal portal;
    public long activeTime = 0;
    
    public PortalCollisionEntry(Portal portal) {this.portal = portal;}
}
