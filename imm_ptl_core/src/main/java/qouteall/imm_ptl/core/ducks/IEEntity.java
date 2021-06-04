package qouteall.imm_ptl.core.ducks;

import qouteall.imm_ptl.core.portal.Portal;
import net.minecraft.entity.Entity;

public interface IEEntity {
    void notifyCollidingWithPortal(Entity portal);
    
    Portal getCollidingPortal();
    
    void tickCollidingPortal(float tickDelta);
    
    boolean isRecentlyCollidingWithPortal();
    
    void portal_unsetRemoved();
}
