package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Mirror extends Portal {
    public static EntityType<Mirror> entityType;
    
    public Mirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    @Override
    public void tick() {
        super.tick();
        teleportable = false;
    }
    
    @Override
    public boolean isInteractable() {
        return Global.mirrorInteractableThroughPortal && super.isInteractable();
    }
    
//    @Override
//    public Vec3d getContentDirection() {
//        return getNormal();
//    }
    
    @Override
    public Vec3d transformLocalVecNonScale(Vec3d localVec) {
        return getMirrored(super.transformLocalVecNonScale(localVec));
    }
    
    @Override
    public boolean canTeleportEntity(Entity entity) {
        return false;
    }
    
    public Vec3d getMirrored(Vec3d vec) {
        double len = vec.dotProduct(getNormal());
        return vec.add(getNormal().multiply(len * -2));
    }
    
    @Override
    public Vec3d untransformLocalVec(Vec3d localVec) {
        return getMirrored(super.untransformLocalVec(localVec));
    }
}
