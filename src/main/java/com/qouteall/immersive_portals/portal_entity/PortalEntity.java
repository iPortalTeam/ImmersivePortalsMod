package com.qouteall.immersive_portals.portal_entity;

import com.qouteall.immersive_portals.my_util.Helper;
import net.fabricmc.fabric.api.client.render.EntityRendererRegistry;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.minecraft.client.network.packet.EntitySpawnS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCategory;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.stream.Stream;

public class PortalEntity extends Entity {
    public static EntityType<PortalEntity> entityType;
    
    public double width = 0;
    public double height = 0;
    public Vec3d axisW;
    public Vec3d axisH;
    public Vec3d normal;
    public DimensionType dimensionTo;
    public Vec3d destination;
    
    public static void init() {
        entityType = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier("wiki-entity", "cookie-creeper"),
            FabricEntityTypeBuilder.create(
                EntityCategory.MISC,
                (EntityType<PortalEntity> type, World world1) -> new PortalEntity(
                    type, world1
                )
            ).size(
                1, 1
            ).build()
        );
        
        EntityRendererRegistry.INSTANCE.register(
            PortalEntity.class,
            (entityRenderDispatcher, context) -> new PortalDummyRenderer(entityRenderDispatcher)
        );
    }
    
    public PortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public PortalEntity(
        World world
    ) {
        this(entityType, world);
    }
    
    public PortalEntity(
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
        
        normal = axisW.crossProduct(axisH).normalize();
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag) {
        compoundTag.putDouble("width", width);
        compoundTag.putDouble("height", height);
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        compoundTag.putInt("dimensionTo", dimensionTo.getRawId());
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
    
    public double getDistanceToPlane(
        Vec3d pos
    ) {
        return pos.subtract(getPos()).dotProduct(normal);
    }
    
    public boolean canSeeThroughFromPos(
        Vec3d playerPos
    ) {
        return getDistanceToPlane(playerPos) > 0;
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
        return applyTransformationToPoint(getPos());
    }
    
    
}
