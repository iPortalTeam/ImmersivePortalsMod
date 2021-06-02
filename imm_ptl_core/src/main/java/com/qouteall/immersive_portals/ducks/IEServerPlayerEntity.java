package com.qouteall.immersive_portals.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public interface IEServerPlayerEntity {
    void setEnteredNetherPos(Vec3d pos);
    
    void updateDimensionTravelAdvancements(ServerWorld fromWorld);
    
    void setIsInTeleportationState(boolean arg);
    
    void stopRidingWithoutTeleportRequest();
    
    void startRidingWithoutTeleportRequest(Entity newVehicle);
    
    void portal_worldChanged(ServerWorld fromWorld);
}
