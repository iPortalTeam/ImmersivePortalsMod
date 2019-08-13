package com.qouteall.immersive_portals.exposer;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public interface IEServerPlayerEntity {
    void setEnteredNetherPos(Vec3d pos);
    
    void updateDimensionTravelAdvancements(ServerWorld fromWorld);
    
    void setIsInTeleportationState(boolean arg);
}
