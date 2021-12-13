package qouteall.imm_ptl.core.ducks;

import net.minecraft.entity.Entity;
import qouteall.imm_ptl.core.portal.Portal;

public interface IEEntity {
    void notifyCollidingWithPortal(Entity portal);
    
    Portal getCollidingPortal();
    
    void tickCollidingPortal(float tickDelta);
    
    boolean isRecentlyCollidingWithPortal();
    
    void portal_unsetRemoved();
}
