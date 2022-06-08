package qouteall.imm_ptl.core.portal.global_portals;

import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WorldWrappingPortal extends GlobalTrackedPortal {
    public static EntityType<WorldWrappingPortal> entityType;
    
    public boolean isInward = true;
    public int zoneId = -1;
    
    public WorldWrappingPortal(
        EntityType<?> entityType_1,
        Level world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        
        if (compoundTag.contains("isInward")) {
            isInward = compoundTag.getBoolean("isInward");
        }
        if (compoundTag.contains("zoneId")) {
            zoneId = compoundTag.getInt("zoneId");
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        
        compoundTag.putBoolean("isInward", isInward);
        compoundTag.putInt("zoneId", zoneId);
    }
    
    private static WorldWrappingPortal createWrappingPortal(
        ServerLevel serverWorld,
        AABB area,
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
        ServerLevel serverWorld,
        AABB area,
        Direction direction,
        boolean isInward,
        Portal portal
    ) {
        Vec3 areaSize = Helper.getBoxSize(area);
        
        Tuple<Direction, Direction> axises = Helper.getPerpendicularDirections(
            isInward ? direction : direction.getOpposite()
        );
        AABB boxSurface = Helper.getBoxSurfaceInversed(area, direction);
        Vec3 center = boxSurface.getCenter();
        AABB oppositeSurface = Helper.getBoxSurfaceInversed(area, direction.getOpposite());
        Vec3 destination = oppositeSurface.getCenter();
        portal.setPos(center.x, center.y, center.z);
        portal.setDestination(destination);
        
        portal.axisW = Vec3.atLowerCornerOf(axises.getA().getNormal());
        portal.axisH = Vec3.atLowerCornerOf(axises.getB().getNormal());
        portal.width = Helper.getCoordinate(areaSize, axises.getA().getAxis());
        portal.height = Helper.getCoordinate(areaSize, axises.getB().getAxis());
        
        portal.dimensionTo = serverWorld.dimension();
    }
    
    public static class WrappingZone {
        public ServerLevel world;
        public boolean isInwardZone;
        public int id;
        public List<WorldWrappingPortal> portals;
        
        public WrappingZone(
            ServerLevel world,
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
            portals.forEach(worldWrappingPortal -> gps.removePortal(worldWrappingPortal));
        }
        
        public AABB getArea() {
            return portals.stream().map(
                Portal::getThinAreaBox
            ).reduce(AABB::minmax).orElse(null);
        }
        
        public IntBox getIntArea() {
            AABB floatBox = getArea();
            
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
            
            AABB floatBox = getArea();
            
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
            AABB area = getArea();
            return String.format(
                "[%d] %s %s %s ~ %s %s\n",
                id,
                isInwardZone ? "inward" : "outward",
                area.minX, area.minZ,
                area.maxX, area.maxZ
            );
        }
    }
    
    public static List<WrappingZone> getWrappingZones(ServerLevel world) {
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
        ServerLevel world,
        int x1, int z1, int x2, int z2,
        boolean isInward,
        Consumer<Component> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        for (WrappingZone zone : wrappingZones) {
            if (!zone.isValid()) {
                feedbackSender.accept(Component.translatable(
                    "imm_ptl.removed_invalid_wrapping_portals",
                    Helper.myToString(zone.portals.stream())
                ));
                zone.removeFromWorld();
            }
        }
        
        int availableId = getAvailableId(wrappingZones);
    
        AABB box = new IntBox(new BlockPos(x1, 0, z1), new BlockPos(x2, 255, z2)).toRealNumberBox();
        
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
        gps.addPortal(p1);
        gps.addPortal(p2);
        gps.addPortal(p3);
        gps.addPortal(p4);
    }
    
    public static void invokeViewWrappingZones(
        ServerLevel world,
        Consumer<Component> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        wrappingZones.forEach(wrappingZone -> {
            feedbackSender.accept(Component.literal(wrappingZone.toString()));
        });
    }
    
    public static void invokeRemoveWrappingZone(
        ServerLevel world,
        Vec3 playerPos,
        Consumer<Component> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        WrappingZone zone = wrappingZones.stream()
            .filter(z -> z.getArea().contains(playerPos))
            .findFirst().orElse(null);
        
        if (zone != null) {
            zone.removeFromWorld();
            feedbackSender.accept(Component.translatable("imm_ptl.removed_portal", zone.toString()));
        }
        else {
            feedbackSender.accept(Component.translatable("imm_ptl.not_in_wrapping_zone"));
        }
    }
    
    public static void invokeRemoveWrappingZone(
        ServerLevel world,
        int zoneId,
        Consumer<Component> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        WrappingZone zone = wrappingZones.stream()
            .filter(wrappingZone -> wrappingZone.id == zoneId)
            .findFirst().orElse(null);
        
        if (zone != null) {
            zone.removeFromWorld();
            feedbackSender.accept(Component.translatable("imm_ptl.removed_portal", zone.toString()));
        }
        else {
            feedbackSender.accept(Component.translatable("imm_ptl.cannot_find_zone"));
        }
    }
    
}
