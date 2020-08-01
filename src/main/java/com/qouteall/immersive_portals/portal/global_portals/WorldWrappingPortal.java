package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
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
    
    private static WorldWrappingPortal createWrappingPortal(
        ServerWorld serverWorld,
        Box area,
        Direction direction,
        int zoneId,
        boolean isInward
    ) {
        WorldWrappingPortal portal = entityType.create(serverWorld);
        portal.isInward = isInward;
        portal.zoneId = zoneId;
        
        initWrappingPortal(serverWorld, area, direction, isInward, portal);
        
        return portal;
    }
    
    public static void initWrappingPortal(
        ServerWorld serverWorld,
        Box area,
        Direction direction,
        boolean isInward,
        Portal portal
    ) {
        Vec3d areaSize = Helper.getBoxSize(area);
        
        Pair<Direction, Direction> axises = Helper.getPerpendicularDirections(
            isInward ? direction : direction.getOpposite()
        );
        Box boxSurface = Helper.getBoxSurfaceInversed(area, direction);
        Vec3d center = boxSurface.getCenter();
        Box oppositeSurface = Helper.getBoxSurfaceInversed(area, direction.getOpposite());
        Vec3d destination = oppositeSurface.getCenter();
        portal.updatePosition(center.x, center.y, center.z);
        portal.destination = destination;
        
        portal.axisW = Vec3d.of(axises.getLeft().getVector());
        portal.axisH = Vec3d.of(axises.getRight().getVector());
        portal.width = Helper.getCoordinate(areaSize, axises.getLeft().getAxis());
        portal.height = Helper.getCoordinate(areaSize, axises.getRight().getAxis());
        
        portal.dimensionTo = serverWorld.getRegistryKey();
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
            gps.onDataChanged();
        }
        
        public Box getArea() {
            return portals.stream().map(
                Portal::getThinAreaBox
            ).reduce(Box::union).orElse(null);
        }
        
        public IntBox getIntArea() {
            Box floatBox = getArea();
            
            return new IntBox(
                new BlockPos(
                    Math.round(floatBox.minX), 0, Math.round(floatBox.minZ)
                ),
                new BlockPos(
                    Math.round(floatBox.maxX) - 1, 256, Math.round(floatBox.maxZ) - 1
                )
            );
        }
        
        public IntBox getBorderBox() {
            
            if (!isInwardZone) {
                return getIntArea();
            }
            
            Box floatBox = getArea();
            
            return new IntBox(
                new BlockPos(
                    Math.round(floatBox.minX) - 1, 0, Math.round(floatBox.minZ) - 1
                ),
                new BlockPos(
                    Math.round(floatBox.maxX), 256, Math.round(floatBox.maxZ)
                )
            );
        }
        
        @Override
        public String toString() {
            Box area = getArea();
            return String.format(
                "[%d] %s %s %s ~ %s %s\n",
                id,
                isInwardZone ? "inward" : "outward",
                area.minX, area.minZ,
                area.maxX, area.maxZ
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
    
    public static int getAvailableId(List<WrappingZone> zones) {
        return zones.stream()
            .max(Comparator.comparingInt(z -> z.id))
            .map(z -> z.id + 1)
            .orElse(1);
    }
    
    public static void invokeAddWrappingZone(
        ServerWorld world,
        int x1, int z1, int x2, int z2,
        boolean isInward,
        Consumer<Text> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        for (WrappingZone zone : wrappingZones) {
            if (!zone.isValid()) {
                feedbackSender.accept(new TranslatableText(
                    "imm_ptl.removed_invalid_wrapping_portals",
                    Helper.myToString(zone.portals.stream())
                ));
                zone.removeFromWorld();
            }
        }
        
        int availableId = getAvailableId(wrappingZones);
        
        Box box = new IntBox(new BlockPos(x1, 0, z1), new BlockPos(x2, 255, z2)).toRealNumberBox();
        
        WorldWrappingPortal p1 = createWrappingPortal(
            world, box, Direction.NORTH, availableId, isInward
        );
        WorldWrappingPortal p2 = createWrappingPortal(
            world, box, Direction.SOUTH, availableId, isInward
        );
        WorldWrappingPortal p3 = createWrappingPortal(
            world, box, Direction.WEST, availableId, isInward
        );
        WorldWrappingPortal p4 = createWrappingPortal(
            world, box, Direction.EAST, availableId, isInward
        );
        
        GlobalPortalStorage gps = GlobalPortalStorage.get(world);
        gps.data.add(p1);
        gps.data.add(p2);
        gps.data.add(p3);
        gps.data.add(p4);
        gps.onDataChanged();
    }
    
    public static void invokeViewWrappingZones(
        ServerWorld world,
        Consumer<Text> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        wrappingZones.forEach(wrappingZone -> {
            feedbackSender.accept(new LiteralText(wrappingZone.toString()));
        });
    }
    
    public static void invokeRemoveWrappingZone(
        ServerWorld world,
        Vec3d playerPos,
        Consumer<Text> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        WrappingZone zone = wrappingZones.stream()
            .filter(z -> z.getArea().contains(playerPos))
            .findFirst().orElse(null);
        
        if (zone != null) {
            zone.removeFromWorld();
            feedbackSender.accept(new TranslatableText("imm_ptl.removed_portal", zone.toString()));
        }
        else {
            feedbackSender.accept(new TranslatableText("imm_ptl.not_in_wrapping_zone"));
        }
    }
    
    public static void invokeRemoveWrappingZone(
        ServerWorld world,
        int zoneId,
        Consumer<Text> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        WrappingZone zone = wrappingZones.stream()
            .filter(wrappingZone -> wrappingZone.id == zoneId)
            .findFirst().orElse(null);
        
        if (zone != null) {
            zone.removeFromWorld();
            feedbackSender.accept(new TranslatableText("imm_ptl.removed_portal", zone.toString()));
        }
        else {
            feedbackSender.accept(new TranslatableText("imm_ptl.cannot_find_zone"));
        }
    }
    
}
