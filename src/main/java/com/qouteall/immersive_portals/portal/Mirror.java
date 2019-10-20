package com.qouteall.immersive_portals.portal;

import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class Mirror extends Portal {
    public static EntityType<Mirror> entityType;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "mirror"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<Mirror> type, World world1) ->
                    new Mirror(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
    }
    
    public Mirror(EntityType<?> entityType_1, World world_1) {
        super(entityType_1, world_1);
    }
    
    public Mirror(World world) {
        this(entityType, world);
    }
    
    public Vec3d getContentDirection() {
        return getNormal();
    }
    
    @Override
    public boolean isTeleportable() {
        return false;
    }
    
}
