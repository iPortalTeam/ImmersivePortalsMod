package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WorldWrappingPortal extends GlobalTrackedPortal {
    public static EntityType<WorldWrappingPortal> entityType;
    
    public boolean isInward = true;
    public int zoneId = -1;
    
    public WorldWrappingPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag compoundTag) {
        super.readCustomDataFromTag(compoundTag);
        
        if (compoundTag.contains("isInward")) {
            isInward = compoundTag.getBoolean("isInward");
        }
        if (compoundTag.contains("zoneId")) {
            zoneId = compoundTag.getInt("zoneId");
        }
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag) {
        super.writeCustomDataToTag(compoundTag);
        
        compoundTag.putBoolean("isInward", isInward);
        compoundTag.putInt("zoneId", zoneId);
    }

//    public static void setBorderPortal(
//        ServerWorld world,
//        int x1, int y1,
//        int x2, int y2
//    ) {
//        GlobalPortalStorage storage = GlobalPortalStorage.get(world);
//
//        storage.data.removeIf(
//            portal -> portal instanceof WorldWrappingPortal
//        );
//
//        IntBox box = new IntBox(
//            new BlockPos(x1, 0, y1),
//            new BlockPos(x2, 256, y2)
//        ).getSorted();
//        Box area = box.toRealNumberBox();
//
//        storage.data.add(createWrappingPortal(world, area, Direction.NORTH));
//        storage.data.add(createWrappingPortal(world, area, Direction.SOUTH));
//        storage.data.add(createWrappingPortal(world, area, Direction.WEST));
//        storage.data.add(createWrappingPortal(world, area, Direction.EAST));
//
//        storage.onDataChanged();
//    }
//
//    public static void removeBorderPortal(
//        ServerWorld world
//    ) {
//        GlobalPortalStorage storage = GlobalPortalStorage.get(world);
//
//        storage.data.removeIf(
//            portal -> portal instanceof WorldWrappingPortal
//        );
//
//        storage.onDataChanged();
//    }
    
    private static WorldWrappingPortal createWrappingPortal(
        ServerWorld serverWorld,
        Box area,
        Direction direction,
        int zoneId,
        boolean isInward
    ) {
        WorldWrappingPortal portal = new WorldWrappingPortal(entityType, serverWorld);
        portal.isInward = isInward;
        portal.zoneId = zoneId;
        
        Vec3d areaSize = Helper.getBoxSize(area);
        
        Pair<Direction, Direction> axises = Helper.getPerpendicularDirections(
            isInward ? direction : direction.getOpposite()
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
        
        return portal;
    }
    
    public static class WrappingZone {
        public ServerWorld world;
        public boolean isInwardZone;
        public int id;
        public List<WorldWrappingPortal> portals;
        
        public WrappingZone(
            ServerWorld world,
            boolean isInwardZone,
            int id,
            List<WorldWrappingPortal> portals
        ) {
            this.world = world;
            this.isInwardZone = isInwardZone;
            this.id = id;
            this.portals = portals;
        }
        
        public boolean isValid() {
            return (portals.size() == 4) &&
                (portals.get(0).isInward == isInwardZone) &&
                (portals.get(1).isInward == isInwardZone) &&
                (portals.get(2).isInward == isInwardZone) &&
                (portals.get(3).isInward == isInwardZone);
        }
        
        public void removeFromWorld() {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            portals.forEach(worldWrappingPortal -> gps.data.remove(worldWrappingPortal));
        }
        
        public Box getArea() {
            return new Box(
                portals.get(0).getPos(),
                portals.get(1).getPos()
            ).union(
                new Box(
                    portals.get(2).getPos(),
                    portals.get(3).getPos()
                )
            );
        }
        
        public IntBox getIntArea() {
            Box floatBox = getArea();
            
            return new IntBox(
                new BlockPos(
                    Math.round(floatBox.x1), 0, Math.round(floatBox.z1)
                ),
                new BlockPos(
                    Math.round(floatBox.x2) - 1, 256, Math.round(floatBox.z2) - 1
                )
            );
        }
        
        public IntBox getBorderBox() {
            Box floatBox = getArea();
            
            return new IntBox(
                new BlockPos(
                    Math.round(floatBox.x1) - 1, 0, Math.round(floatBox.z1) - 1
                ),
                new BlockPos(
                    Math.round(floatBox.x2), 256, Math.round(floatBox.z2)
                )
            );
        }
    }
    
    public static List<WrappingZone> getWrappingZones(ServerWorld world) {
        GlobalPortalStorage gps = GlobalPortalStorage.get(world);
        
        List<WrappingZone> result = new ArrayList<>();
        
        gps.data.stream()
            .filter(portal -> portal instanceof WorldWrappingPortal)
            .map(portal -> ((WorldWrappingPortal) portal))
            .collect(Collectors.groupingBy(
                portal -> portal.zoneId
            ))
            .forEach((zoneId, portals) -> {
                result.add(new WrappingZone(
                    world, portals.get(0).isInward,
                    zoneId, portals
                ));
            });
        
        return result;
    }
    
    public static void purgeWrappingZones(List<WrappingZone> zones) {
        for (WrappingZone zone : zones) {
            if (!zone.isValid()) {
                zone.removeFromWorld();
            }
        }
    }
    
    public static int getAvailableId(List<WrappingZone> zones) {
        return zones.stream()
            .max(Comparator.comparingInt(z -> z.id))
            .map(z -> z.id + 1)
            .orElse(1);
    }
    
}
