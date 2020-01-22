package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class BorderPortal extends GlobalTrackedPortal {
    public static EntityType<BorderPortal> entityType;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "border_portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                BorderPortal::new
            ).size(
                new EntityDimensions(1, 1, true)
            ).setImmuneToFire().build()
        );
    }
    
    public BorderPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void setBorderPortal(
        ServerWorld world,
        int x1, int y1,
        int x2, int y2
    ) {
        GlobalPortalStorage storage = GlobalPortalStorage.get(world);
        
        storage.data.removeIf(
            portal -> portal instanceof BorderPortal
        );
        
        IntegerAABBInclusive box = new IntegerAABBInclusive(
            new BlockPos(x1, 0, y1),
            new BlockPos(x2, 256, y2)
        ).getSorted();
        Box area = box.toRealNumberBox();
        
        storage.data.add(createWrappingPortal(world, area, Direction.NORTH));
        storage.data.add(createWrappingPortal(world, area, Direction.SOUTH));
        storage.data.add(createWrappingPortal(world, area, Direction.WEST));
        storage.data.add(createWrappingPortal(world, area, Direction.EAST));
        
        storage.onDataChanged();
    }
    
    public static void removeBorderPortal(
        ServerWorld world
    ) {
        GlobalPortalStorage storage = GlobalPortalStorage.get(world);
        
        storage.data.removeIf(
            portal -> portal instanceof BorderPortal
        );
        
        storage.onDataChanged();
    }
    
    private static BorderPortal createWrappingPortal(
        ServerWorld serverWorld,
        Box area,
        Direction direction
    ) {
        BorderPortal portal = new BorderPortal(entityType, serverWorld);
        
        Vec3d areaSize = Helper.getBoxSize(area);
        
        Pair<Direction, Direction> axises = Helper.getPerpendicularDirections(
            direction
        );
        Box boxSurface = Helper.getBoxSurface(area, direction);
        Vec3d center = boxSurface.getCenter();
        Box oppositeSurface = Helper.getBoxSurface(area, direction.getOpposite());
        Vec3d destination = oppositeSurface.getCenter();
        portal.updatePosition(center.x, center.y, center.z);
        portal.destination = destination;
        
        portal.axisW = new Vec3d(axises.getLeft().getVector());
        portal.axisH = new Vec3d(axises.getRight().getVector());
        portal.width = Helper.getCoordinate(areaSize, axises.getLeft().getAxis());
        portal.height = Helper.getCoordinate(areaSize, axises.getRight().getAxis());
        
        portal.dimensionTo = serverWorld.dimension.getType();
        portal.loadFewerChunks = false;
        
        return portal;
    }
}
