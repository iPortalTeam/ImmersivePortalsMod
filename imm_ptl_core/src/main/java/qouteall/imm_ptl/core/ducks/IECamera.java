package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public interface IECamera {
    void resetState(Vec3d pos, ClientWorld currWorld);
    
    void portal_setPos(Vec3d pos);
    
    float getCameraY();
    
    float getLastCameraY();
    
    void setCameraY(float cameraY, float lastCameraY);
    
    void portal_setFocusedEntity(Entity arg);
}
