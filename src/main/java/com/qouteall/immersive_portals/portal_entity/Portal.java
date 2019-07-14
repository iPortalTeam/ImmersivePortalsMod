package com.qouteall.immersive_portals.portal_entity;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.SignalArged;
import javafx.util.Pair;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

public class Portal extends Entity {
    public static EntityType<Portal> entityType;
    
    public double width = 0;
    public double height = 0;
    public Vec3d axisW;
    public Vec3d axisH;
    private Vec3d normal;
    public DimensionType dimensionTo;
    public Vec3d destination;
    
    public static final SignalArged<Portal> clientPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> serverPortalTickSignal = new SignalArged<>();
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("immersive_portals", "portal"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<Portal> type, World world1) ->
                    new Portal(type, world1)
            ).size(
                new EntityDimensions(1, 1, true)
            ).build()
        );
    
        EntityRendererRegistry.INSTANCE.register(
            Portal.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
    
    
    }
    
    public Portal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public Portal(
        World world
    ) {
        this(entityType, world);
    }
    
    public Portal(
        World world_1,
        double width,
        double height,
        Vec3d axisW,
        Vec3d axisH,
        DimensionType dimensionTo,
        Vec3d destination
    ) {
        super(entityType, world_1);
        this.width = width;
        this.height = height;
        this.axisW = axisW;
        this.axisH = axisH;
        this.dimensionTo = dimensionTo;
        this.destination = destination;
        
        normal = axisW.crossProduct(axisH).normalize();
    }
    
    @Override
    protected void initDataTracker() {
        //do nothing
    }
    
    @Override
    protected void readCustomDataFromTag(CompoundTag compoundTag) {
        width = compoundTag.getDouble("width");
        height = compoundTag.getDouble("height");
        axisW = Helper.getVec3d(compoundTag, "axisW").normalize();
        axisH = Helper.getVec3d(compoundTag, "axisH").normalize();
        dimensionTo = DimensionType.byRawId(compoundTag.getInt("dimensionTo"));
        destination = Helper.getVec3d(compoundTag, "destination");
    }
    
    public Vec3d getNormal() {
        if (normal == null)
            normal = axisW.crossProduct(axisH).normalize();
        return normal;
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag) {
        compoundTag.putDouble("width", width);
        compoundTag.putDouble("height", height);
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        compoundTag.putInt("dimensionTo", dimensionTo.getRawId());
        Helper.putVec3d(compoundTag, "destination", destination);
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(
            entityType,
            this
        );
    }
    
    @Override
    public void tick() {
        if (world.isClient) {
            clientPortalTickSignal.emit(this);
        }
        else {
            if (!isPortalValid()) {
                Helper.log("removed invalid portal" + this);
                removed = true;
                return;
            }
            serverPortalTickSignal.emit(this);
        }
    }
    
    public boolean isPortalValid() {
        return dimensionTo != null &&
            width != 0 &&
            height != 0 &&
            axisW != null &&
            axisH != null &&
            destination != null;
    }
    
    public double getDistanceToPlane(
        Vec3d pos
    ) {
        return pos.subtract(getPos()).dotProduct(getNormal());
    }
    
    public boolean isInFrontOfPortal(
        Vec3d playerPos
    ) {
        return getDistanceToPlane(playerPos) > 0;
    }
    
    public boolean isInFrontOfPortalRoughly(
        Vec3d playerPos
    ) {
        return getDistanceToPlane(playerPos) > -0.3;
    }
    
    public boolean canRenderPortalInsideMe(Portal anotherPortal) {
        if (anotherPortal.dimension != dimensionTo) {
            return false;
        }
        double v = anotherPortal.getPos().subtract(destination).dotProduct(getNormal());
        return v < -0.5;
    }
    
    public Vec3d getPointInPlane(double xInPlane, double yInPlane) {
        return getPos().add(axisW.multiply(xInPlane)).add(axisH.multiply(yInPlane));
    }
    
    public Vec3d getPointInPlaneInLocalCoordinate(double xInPlane, double yInPlane) {
        return axisW.multiply(xInPlane).add(axisH.multiply(yInPlane));
    }
    
    //3  2
    //1  0
    public Vec3d[] getFourVertices(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlane(width / 2 - shrinkFactor, -height / 2 + shrinkFactor);
        vertices[1] = getPointInPlane(-width / 2 + shrinkFactor, -height / 2 + shrinkFactor);
        vertices[2] = getPointInPlane(width / 2 - shrinkFactor, height / 2 - shrinkFactor);
        vertices[3] = getPointInPlane(-width / 2 + shrinkFactor, height / 2 - shrinkFactor);
        
        return vertices;
    }
    
    //3  2
    //1  0
    public Vec3d[] getFourVerticesInLocalCoordinate(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlaneInLocalCoordinate(
            width / 2 - shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[1] = getPointInPlaneInLocalCoordinate(
            -width / 2 + shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[2] = getPointInPlaneInLocalCoordinate(
            width / 2 - shrinkFactor,
            height / 2 - shrinkFactor
        );
        vertices[3] = getPointInPlaneInLocalCoordinate(
            -width / 2 + shrinkFactor,
            height / 2 - shrinkFactor
        );
        
        return vertices;
    }
    
    public Vec3d applyTransformationToPoint(Vec3d pos) {
        Vec3d offset = destination.subtract(getPos());
        return pos.add(offset);
    }
    
    public Vec3d getCullingPoint() {
        return destination;
    }
    
    public Box getPortalCollisionBox() {
        return new Box(
            getPointInPlane(width / 2, height / 2),
            getPointInPlane(-width / 2, -height / 2)
        ).expand(0.1);
    }
    
    @Override
    public String toString() {
        return "Portal{" +
            "pos=" + getBlockPos() +
            ", dimensionTo=" + dimensionTo +
            ", destination=" + new BlockPos(destination) +
            ", normal=" + new BlockPos(getNormal()) +
            '}';
    }
    
    public static void initBiWayBiFacedPortal(
        Portal[] portals,
        DimensionType dimension1,
        Vec3d center1,
        DimensionType dimension2,
        Vec3d center2,
        Direction.Axis normalAxis,
        Vec3d portalSize
    ) {
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(normalAxis);
        Direction.Axis wAxis = anotherTwoAxis.getKey();
        Direction.Axis hAxis = anotherTwoAxis.getValue();
        
        float width = (float) Helper.getCoordinate(portalSize, wAxis);
        float height = (float) Helper.getCoordinate(portalSize, hAxis);
        
        Vec3d wAxisVec = new Vec3d(Helper.getUnitFromAxis(wAxis));
        Vec3d hAxisVec = new Vec3d(Helper.getUnitFromAxis(hAxis));
        
        portals[0].setPosition(center1.x, center1.y, center1.z);
        portals[1].setPosition(center1.x, center1.y, center1.z);
        portals[2].setPosition(center2.x, center2.y, center2.z);
        portals[3].setPosition(center2.x, center2.y, center2.z);
        
        portals[0].destination = center2;
        portals[1].destination = center2;
        portals[2].destination = center1;
        portals[3].destination = center1;
        
        portals[0].dimensionTo = dimension2;
        portals[1].dimensionTo = dimension2;
        portals[2].dimensionTo = dimension1;
        portals[3].dimensionTo = dimension1;
        
        portals[0].axisW = wAxisVec;
        portals[1].axisW = wAxisVec.multiply(-1);
        portals[2].axisW = wAxisVec;
        portals[3].axisW = wAxisVec.multiply(-1);
        
        portals[0].axisH = hAxisVec;
        portals[1].axisH = hAxisVec;
        portals[2].axisH = hAxisVec;
        portals[3].axisH = hAxisVec;
        
        portals[0].width = width;
        portals[1].width = width;
        portals[2].width = width;
        portals[3].width = width;
        
        portals[0].height = height;
        portals[1].height = height;
        portals[2].height = height;
        portals[3].height = height;
    }
    
    public boolean shouldEntityTeleport(Entity player) {
        return player.dimension == this.dimension &&
            isMovedThroughPortal(
                Helper.lastTickPosOf(player),
                player.getPos()
            );
    }
    
    public boolean isPointInPortalProjection(Vec3d pos) {
        Vec3d offset = pos.subtract(getPos());
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        return Math.abs(xInPlane) < (width / 2 + 0.1) &&
            Math.abs(yInPlane) < (height / 2 + 0.1);
        
    }
    
    public boolean isMovedThroughPortal(
        Vec3d lastTickPos,
        Vec3d pos
    ) {
        double lastDistance = getDistanceToPlane(lastTickPos);
        double nowDistance = getDistanceToPlane(pos);
        
        if (!(lastDistance > 0 && nowDistance < 0)) {
            return false;
        }
        
        Vec3d lineOrigin = lastTickPos;
        Vec3d lineDirection = pos.subtract(lastTickPos).normalize();
        
        double collidingT = Helper.getCollidingT(getPos(), normal, lineOrigin, lineDirection);
        Vec3d collidingPoint = lineOrigin.add(lineDirection.multiply(collidingT));
        
        return isPointInPortalProjection(collidingPoint);
    }
    
}
