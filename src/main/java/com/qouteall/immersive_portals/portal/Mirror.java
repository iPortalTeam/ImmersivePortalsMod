package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Global;
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
    public boolean isInteractable()
    {
        return Global.mirrorInteractableThroughPortal && super.isInteractable();
    }

    @Override
    public Vec3d getContentDirection() {
        return getNormal();
    }

    @Override
    public Vec3d transformPoint(Vec3d pos) {
        Vec3d localPos = pos.subtract(getPos());
        
        return transformLocalVec(localPos).add(destination);
    }
    
    @Override
    public Vec3d transformLocalVec(Vec3d localVec) {
        double len = localVec.dotProduct(getNormal());
        return localVec.add(getNormal().multiply(len * -2));
    }
    
}
