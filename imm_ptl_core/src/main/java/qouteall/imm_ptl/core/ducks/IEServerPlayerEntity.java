package qouteall.imm_ptl.core.ducks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface IEServerPlayerEntity {
    void setEnteredNetherPos(Vec3 pos);
    
    void setIsInTeleportationState(boolean arg);
    
    void stopRidingWithoutTeleportRequest();
    
    void startRidingWithoutTeleportRequest(Entity newVehicle);
    
    void portal_worldChanged(ServerLevel fromWorld, Vec3 fromPos);
    
//    boolean ip_getRealIsContainerMenuValid(AbstractContainerMenu instance);
}
