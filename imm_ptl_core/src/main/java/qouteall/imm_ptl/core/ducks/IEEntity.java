package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;

public interface IEEntity {
    void notifyCollidingWithPortal(Entity portal);
    
    Portal getCollidingPortal();
    
    void tickCollidingPortal(float tickDelta);
    
    boolean isRecentlyCollidingWithPortal();
    
    void ip_clearCollidingPortal();
    
    void portal_unsetRemoved();
    
    // don't trigger entity section update or other update
    // as we are temporarily switching the position
    void ip_setPositionWithoutTriggeringCallback(Vec3 newPos);
}
