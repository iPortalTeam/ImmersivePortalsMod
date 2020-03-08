package com.qouteall.immersive_portals.portal;

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
    
    public Vec3d getContentDirection() {
        return getNormal();
    }
    
}
