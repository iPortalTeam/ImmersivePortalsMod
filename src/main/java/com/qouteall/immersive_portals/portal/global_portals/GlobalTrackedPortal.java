package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class GlobalTrackedPortal extends Portal {
    public static EntityType<GlobalTrackedPortal> entityType;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "global_tracked_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                GlobalTrackedPortal::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
    }
    
    public GlobalTrackedPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
}
