package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.my_util.Helper;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class EndFloorPortal extends GlobalTrackedPortal {
    public static EntityType<EndFloorPortal> entityType;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "end_floor_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                EndFloorPortal::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
    }
    
    public EndFloorPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void enableFloor() {
        ServerWorld endWorld = Helper.getServer().getWorld(DimensionType.THE_END);
        GlobalPortalStorage storage = GlobalPortalStorage.get(endWorld);
        
        boolean isEndFloorPresent = storage.data.stream()
            .anyMatch(
                portal -> portal instanceof EndFloorPortal
            );
        
        if (!isEndFloorPresent) {
            EndFloorPortal endFloorPortal = new EndFloorPortal(entityType, endWorld);
            
            endFloorPortal.setPosition(0, 0, 0);
            endFloorPortal.destination = new Vec3d(0, 256, 0);
            endFloorPortal.dimensionTo = DimensionType.OVERWORLD;
            endFloorPortal.axisW = new Vec3d(0, 0, 1);
            endFloorPortal.axisH = new Vec3d(1, 0, 0);
            endFloorPortal.width = 23333;
            endFloorPortal.height = 23333;
            
            endFloorPortal.loadFewerChunks = false;
            
            storage.data.add(endFloorPortal);
            
            storage.onDataChanged();
        }
    }
    
    public static void removeFloor() {
        ServerWorld endWorld = Helper.getServer().getWorld(DimensionType.THE_END);
        GlobalPortalStorage storage = GlobalPortalStorage.get(endWorld);
        
        storage.data.removeIf(
            portal -> portal instanceof EndFloorPortal
        );
        
        storage.onDataChanged();
    }
}
