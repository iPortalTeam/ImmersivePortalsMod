package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.teleportation.PortalCollisionEntry;

import javax.annotation.Nullable;
import java.util.List;

public interface IEEntity {
    void ip_notifyCollidingWithPortal(Entity portal);
    
    @Nullable
    List<PortalCollisionEntry> ip_getPortalCollisions();
    
    void ip_setPortalCollisions(@Nullable List<PortalCollisionEntry> list);
    
    @Nullable
    @Deprecated
    Portal getCollidingPortal();
    
    void tickCollidingPortal(float tickDelta);
    
    boolean isRecentlyCollidingWithPortal();
    
    void ip_clearCollidingPortal();
    
    void portal_unsetRemoved();
    
    // don't trigger entity section update or other update
    // as we are temporarily switching the position
    void ip_setPositionWithoutTriggeringCallback(Vec3 newPos);
}
