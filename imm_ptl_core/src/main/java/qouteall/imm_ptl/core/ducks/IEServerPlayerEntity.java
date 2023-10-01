package qouteall.imm_ptl.core.ducks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface IEServerPlayerEntity {
    
    void ip_stopRidingWithoutTeleportRequest();
    
    void ip_startRidingWithoutTeleportRequest(Entity newVehicle);
    
    void portal_worldChanged(ServerLevel fromWorld, Vec3 fromPos);
}
