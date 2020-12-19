package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.my_util.BoxPredicate;
import com.qouteall.immersive_portals.my_util.Plane;
import com.qouteall.immersive_portals.my_util.RotationHelper;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.render.FrustumCuller;
import com.qouteall.immersive_portals.render.PortalRenderInfo;
import com.qouteall.immersive_portals.render.PortalRenderer;
import com.qouteall.immersive_portals.render.PortalRenderingGroup;
import com.qouteall.immersive_portals.render.ViewAreaRenderer;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Portal entity. Global portals are also entities but not added into world.
 */
public class Portal extends Entity implements PortalLike {
    public static EntityType<Portal> entityType;
    
    public static final UUID nullUUID = Util.NIL_UUID;
    
    /**
     * The portal area length along axisW
     */
    public double width = 0;
    public double height = 0;
    
    /**
     * axisW and axisH define the orientation of the portal
     * They should be normalized and should be perpendicular to each other
     */
    public Vec3d axisW;
    public Vec3d axisH;
    
    /**
     * The destination dimension
     */
    public RegistryKey<World> dimensionTo;
    public Vec3d destination;
    
    /**
     * If false, cannot teleport entities
     */
    public boolean teleportable = true;
    /**
     * If not null, this portal can only be accessed by one player
     * If it's {@link Portal#nullUUID} the portal can only be accessed by entities
     */
    @Nullable
    public UUID specificPlayerId;
    /**
     * If not null, defines the special shape of the portal
     * The shape should not exceed the area defined by width and height
     */
    @Nullable
    public GeometryPortalShape specialShape;
    
    private Box exactBoundingBoxCache;
    private Box boundingBoxCache;
    private Vec3d normal;
    private Vec3d contentDirection;
    
    /**
     * For outer frustum culling
     */
    public double cullableXStart = 0;
    public double cullableXEnd = 0;
    public double cullableYStart = 0;
    public double cullableYEnd = 0;
    
    /**
     * The rotating transformation of the portal
     */
    @Nullable
    public Quaternion rotation;
    
    /**
     * The scaling transformation of the portal
     */
    public double scaling = 1.0;
    
    /**
     * Whether the entity scale changes after crossing the portal
     */
    public boolean teleportChangesScale = true;
    
    
    /**
     * Whether the player can place and break blocks across the portal
     */
    private boolean interactable = true;
    
    PortalExtension extension;
    
    @Nullable
    public String portalTag;
    
    public boolean isGlobalPortal = false;
    
    public static final SignalArged<Portal> clientPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> serverPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> portalCacheUpdateSignal = new SignalArged<>();
    public static final SignalArged<Portal> portalDisposeSignal = new SignalArged<>();
    public static final SignalBiArged<Portal, CompoundTag> readPortalDataSignal = new SignalBiArged<>();
    public static final SignalBiArged<Portal, CompoundTag> writePortalDataSignal = new SignalBiArged<>();
    
    public Portal(
        EntityType<?> entityType, World world
    ) {
        super(entityType, world);
    }
    
    // Scaling does not interfere camera transformation
    @Override
    @Nullable
    public Matrix4f getAdditionalCameraTransformation() {
        
        return PortalRenderer.getPortalTransformation(this);
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
        dimensionTo = DimId.getWorldId(compoundTag, "dimensionTo", world.isClient);
        setDestination(Helper.getVec3d(compoundTag, "destination"));
        if (compoundTag.contains("specificPlayer")) {
            specificPlayerId = Helper.getUuid(compoundTag, "specificPlayer");
        }
        if (compoundTag.contains("specialShape")) {
            specialShape = new GeometryPortalShape(
                compoundTag.getList("specialShape", 6)
            );
            if (specialShape.triangles.isEmpty()) {
                specialShape = null;
            }
        }
        if (compoundTag.contains("teleportable")) {
            teleportable = compoundTag.getBoolean("teleportable");
        }
        if (compoundTag.contains("cullableXStart")) {
            cullableXStart = compoundTag.getDouble("cullableXStart");
            cullableXEnd = compoundTag.getDouble("cullableXEnd");
            cullableYStart = compoundTag.getDouble("cullableYStart");
            cullableYEnd = compoundTag.getDouble("cullableYEnd");
            
            cullableXEnd = Math.min(cullableXEnd, width / 2);
            cullableXStart = Math.max(cullableXStart, -width / 2);
            cullableYEnd = Math.min(cullableYEnd, height / 2);
            cullableYStart = Math.max(cullableYStart, -height / 2);
        }
        else {
            if (specialShape != null) {
                cullableXStart = 0;
                cullableXEnd = 0;
                cullableYStart = 0;
                cullableYEnd = 0;
            }
            else {
                initDefaultCullableRange();
            }
        }
        if (compoundTag.contains("rotationA")) {
            rotation = new Quaternion(
                compoundTag.getFloat("rotationB"),
                compoundTag.getFloat("rotationC"),
                compoundTag.getFloat("rotationD"),
                compoundTag.getFloat("rotationA")
            );
        }
        
        if (compoundTag.contains("interactable")) {
            interactable = compoundTag.getBoolean("interactable");
        }
        
        if (compoundTag.contains("scale")) {
            scaling = compoundTag.getDouble("scale");
        }
        if (compoundTag.contains("teleportChangesScale")) {
            teleportChangesScale = compoundTag.getBoolean("teleportChangesScale");
        }
        
        if (compoundTag.contains("portalTag")) {
            portalTag = compoundTag.getString("portalTag");
        }
        
        readPortalDataSignal.emit(this, compoundTag);
        
        updateCache();
    }
    
    @Override
    protected void writeCustomDataToTag(CompoundTag compoundTag) {
        compoundTag.putDouble("width", width);
        compoundTag.putDouble("height", height);
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        DimId.putWorldId(compoundTag, "dimensionTo", dimensionTo);
        Helper.putVec3d(compoundTag, "destination", getDestPos());
        
        if (specificPlayerId != null) {
            Helper.putUuid(compoundTag, "specificPlayer", specificPlayerId);
        }
        
        if (specialShape != null) {
            compoundTag.put("specialShape", specialShape.writeToTag());
        }
        
        compoundTag.putBoolean("teleportable", teleportable);
        
        if (specialShape == null) {
            initDefaultCullableRange();
        }
        compoundTag.putDouble("cullableXStart", cullableXStart);
        compoundTag.putDouble("cullableXEnd", cullableXEnd);
        compoundTag.putDouble("cullableYStart", cullableYStart);
        compoundTag.putDouble("cullableYEnd", cullableYEnd);
        if (rotation != null) {
            compoundTag.putDouble("rotationA", rotation.getW());
            compoundTag.putDouble("rotationB", rotation.getX());
            compoundTag.putDouble("rotationC", rotation.getY());
            compoundTag.putDouble("rotationD", rotation.getZ());
        }
        
        compoundTag.putBoolean("interactable", interactable);
        
        compoundTag.putDouble("scale", scaling);
        compoundTag.putBoolean("teleportChangesScale", teleportChangesScale);
        
        if (portalTag != null) {
            compoundTag.putString("portalTag", portalTag);
        }
        
        writePortalDataSignal.emit(this, compoundTag);
        
    }
    
    public boolean canDoOuterFrustumCulling() {
        if (specialShape == null) {
            initDefaultCullableRange();
        }
        return cullableXStart != cullableXEnd;
    }
    
    // use canTeleportEntity
    @Deprecated
    public boolean isTeleportable() {
        return teleportable;
    }
    
    /**
     * Determines whether the player should be able to reach through the portal or not.
     * Can be overridden by a sub class.
     *
     * @return the interactability of the portal
     */
    public boolean isInteractable() {
        return interactable;
    }
    
    /**
     * Changes the reach-through behavior of the portal.
     *
     * @param interactable the interactability of the portal
     */
    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
    }
    
    @Override
    public void setPos(double x, double y, double z) {
        boolean shouldUpdate = getX() != x || getY() != y || getZ() != z;
        
        super.setPos(x, y, z);
        
        if (shouldUpdate) {
            updateCache();
        }
    }
    
    public void updateCache() {
        boundingBoxCache = null;
        exactBoundingBoxCache = null;
        normal = null;
        contentDirection = null;
        portalCacheUpdateSignal.emit(this);
    }
    
    public void initDefaultCullableRange() {
        cullableXStart = -(width / 2);
        cullableXEnd = (width / 2);
        cullableYStart = -(height / 2);
        cullableYEnd = (height / 2);
    }
    
    public void initCullableRange(
        double cullableXStart,
        double cullableXEnd,
        double cullableYStart,
        double cullableYEnd
    ) {
        this.cullableXStart = Math.min(cullableXStart, cullableXEnd);
        this.cullableXEnd = Math.max(cullableXStart, cullableXEnd);
        this.cullableYStart = Math.min(cullableYStart, cullableYEnd);
        this.cullableYEnd = Math.max(cullableYStart, cullableYEnd);
    }
    
    @Override
    public Packet<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    @Override
    public boolean canBeSpectated(ServerPlayerEntity spectator) {
        if (specificPlayerId == null) {
            return true;
        }
        return spectator.getUuid().equals(specificPlayerId);
    }
    
    @Override
    public void tick() {
        if (world.isClient) {
            clientPortalTickSignal.emit(this);
        }
        else {
            if (!isPortalValid()) {
                Helper.log("removed invalid portal" + this);
                remove();
                return;
            }
            serverPortalTickSignal.emit(this);
        }
        
        CollisionHelper.notifyCollidingPortals(this);
    }
    
    @Override
    public Box getBoundingBox() {
        if (axisW == null) {
            //avoid npe with pehkui
            //pehkui will invoke this when axisW is not initialized
            boundingBoxCache = null;
            return new Box(0, 0, 0, 0, 0, 0);
        }
        if (boundingBoxCache == null) {
            double w = width;
            double h = height;
            if (!getIsGlobal()) {
                // avoid bounding box too big after converting global portal to normal portal
                w = Math.min(this.width, 64.0);
                h = Math.min(this.height, 64.0);
            }
            
            boundingBoxCache = new Box(
                getPointInPlane(w / 2, h / 2)
                    .add(getNormal().multiply(0.2)),
                getPointInPlane(-w / 2, -h / 2)
                    .add(getNormal().multiply(-0.2))
            ).union(new Box(
                getPointInPlane(-w / 2, h / 2)
                    .add(getNormal().multiply(0.2)),
                getPointInPlane(w / 2, -h / 2)
                    .add(getNormal().multiply(-0.2))
            ));
        }
        return boundingBoxCache;
    }
    
    public Box getExactBoundingBox() {
        if (exactBoundingBoxCache == null) {
            exactBoundingBoxCache = new Box(
                getPointInPlane(width / 2, height / 2)
                    .add(getNormal().multiply(0.02)),
                getPointInPlane(-width / 2, -height / 2)
                    .add(getNormal().multiply(-0.02))
            ).union(new Box(
                getPointInPlane(-width / 2, height / 2)
                    .add(getNormal().multiply(0.02)),
                getPointInPlane(width / 2, -height / 2)
                    .add(getNormal().multiply(-0.02))
            ));
        }
        
        return exactBoundingBoxCache;
    }
    
    @Override
    public void setBoundingBox(Box boundingBox) {
        boundingBoxCache = null;
    }
    
    @Override
    public void move(MovementType type, Vec3d movement) {
        //portal cannot be moved
    }
    
    public boolean isPortalValid() {
        boolean valid = dimensionTo != null &&
            width != 0 &&
            height != 0 &&
            axisW != null &&
            axisH != null &&
            getDestPos() != null &&
            axisW.lengthSquared() > 0.9 &&
            axisH.lengthSquared() > 0.9;
        if (valid) {
            if (world instanceof ServerWorld) {
                ServerWorld destWorld = McHelper.getServer().getWorld(dimensionTo);
                if (destWorld == null) {
                    Helper.err("Missing Dimension " + dimensionTo.getValue());
                    return false;
                }
            }
        }
        return valid;
    }
    
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return getUuid();
    }
    
    public void onEntityTeleportedOnServer(Entity entity) {
        //nothing
    }
    
    public void reloadAndSyncToClient() {
        Validate.isTrue(!world.isClient());
        updateCache();
        McHelper.getIEStorage(this.world.getRegistryKey()).resendSpawnPacketToTrackers(this);
    }
    
    @Override
    public String toString() {
        return String.format(
            "%s{%s,%s,(%s %s %s %s)->(%s %s %s %s)%s%s%s}",
            getClass().getSimpleName(),
            getEntityId(),
            Direction.getFacing(
                getNormal().x, getNormal().y, getNormal().z
            ),
            world.getRegistryKey().getValue(), (int) getX(), (int) getY(), (int) getZ(),
            dimensionTo.getValue(), (int) getDestPos().x, (int) getDestPos().y, (int) getDestPos().z,
            specificPlayerId != null ? (",specificAccessor:" + specificPlayerId.toString()) : "",
            hasScaling() ? (",scale:" + scaling) : "",
            portalTag != null ? "," + portalTag : ""
        );
    }
    
    public void transformVelocity(Entity entity) {
        if (PehkuiInterface.isPehkuiPresent) {
            if (teleportChangesScale) {
                entity.setVelocity(transformLocalVecNonScale(entity.getVelocity()));
            }
            else {
                entity.setVelocity(transformLocalVec(entity.getVelocity()));
            }
        }
        else {
            entity.setVelocity(transformLocalVec(entity.getVelocity()));
        }
        
        final int maxVelocity = 15;
        if (entity.getVelocity().length() > maxVelocity) {
            // cannot be too fast
            entity.setVelocity(entity.getVelocity().normalize().multiply(maxVelocity));
        }
    }
    
    public boolean canTeleportEntity(Entity entity) {
        if (!teleportable) {
            return false;
        }
        if (entity instanceof ServerPlayerEntity) {
            if (specificPlayerId != null) {
                if (!entity.getUuid().equals(specificPlayerId)) {
                    return false;
                }
            }
        }
        else {
            if (specificPlayerId != null) {
                if (!specificPlayerId.equals(nullUUID)) {
                    // it can only be used by the player
                    return false;
                }
            }
        }
        
        return entity.canUsePortals();
    }
    
    public Vec3d getNormal() {
        if (normal == null) {
            normal = axisW.crossProduct(axisH).normalize();
        }
        return normal;
    }
    
    public Vec3d getContentDirection() {
        if (contentDirection == null) {
            contentDirection = transformLocalVecNonScale(getNormal().multiply(-1));
        }
        return contentDirection;
    }
    
    public double getDistanceToPlane(
        Vec3d pos
    ) {
        return pos.subtract(getOriginPos()).dotProduct(getNormal());
    }
    
    public boolean isInFrontOfPortal(
        Vec3d playerPos
    ) {
        return getDistanceToPlane(playerPos) > 0;
    }
    
    public Vec3d getPointInPlane(double xInPlane, double yInPlane) {
        return getOriginPos().add(getPointInPlaneLocal(xInPlane, yInPlane));
    }
    
    public Vec3d getPointInPlaneLocal(double xInPlane, double yInPlane) {
        return axisW.multiply(xInPlane).add(axisH.multiply(yInPlane));
    }
    
    public Vec3d getPointInPlaneLocalClamped(double xInPlane, double yInPlane) {
        return getPointInPlaneLocal(
            MathHelper.clamp(xInPlane, -width / 2, width / 2),
            MathHelper.clamp(yInPlane, -height / 2, height / 2)
        );
    }
    
    //3  2
    //1  0
    public Vec3d[] getFourVerticesLocal(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlaneLocal(
            width / 2 - shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[1] = getPointInPlaneLocal(
            -width / 2 + shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[2] = getPointInPlaneLocal(
            width / 2 - shrinkFactor,
            height / 2 - shrinkFactor
        );
        vertices[3] = getPointInPlaneLocal(
            -width / 2 + shrinkFactor,
            height / 2 - shrinkFactor
        );
        
        return vertices;
    }
    
    //3  2
    //1  0
    private Vec3d[] getFourVerticesLocalRotated(double shrinkFactor) {
        Vec3d[] fourVerticesLocal = getFourVerticesLocal(shrinkFactor);
        fourVerticesLocal[0] = transformLocalVec(fourVerticesLocal[0]);
        fourVerticesLocal[1] = transformLocalVec(fourVerticesLocal[1]);
        fourVerticesLocal[2] = transformLocalVec(fourVerticesLocal[2]);
        fourVerticesLocal[3] = transformLocalVec(fourVerticesLocal[3]);
        return fourVerticesLocal;
    }
    
    //3  2
    //1  0
    private Vec3d[] getFourVerticesLocalCullable(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlaneLocal(
            cullableXEnd - shrinkFactor,
            cullableYStart + shrinkFactor
        );
        vertices[1] = getPointInPlaneLocal(
            cullableXStart + shrinkFactor,
            cullableYStart + shrinkFactor
        );
        vertices[2] = getPointInPlaneLocal(
            cullableXEnd - shrinkFactor,
            cullableYEnd - shrinkFactor
        );
        vertices[3] = getPointInPlaneLocal(
            cullableXStart + shrinkFactor,
            cullableYEnd - shrinkFactor
        );
        
        return vertices;
    }
    
    //Server side does not have Matrix3f
    public final Vec3d transformPointRough(Vec3d pos) {
        Vec3d offset = getDestPos().subtract(getOriginPos());
        return pos.add(offset);
    }
    
    @Override
    public Vec3d transformPoint(Vec3d pos) {
        Vec3d localPos = pos.subtract(getOriginPos());
        
        Vec3d result = transformLocalVec(localPos).add(getDestPos());
        
        return result;
        
    }
    
    public Vec3d transformLocalVecNonScale(Vec3d localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vector3f temp = new Vector3f(localVec);
        temp.rotate(rotation);
        
        return new Vec3d(temp);
    }
    
    @Override
    public Vec3d transformLocalVec(Vec3d localVec) {
        return transformLocalVecNonScale(localVec).multiply(scaling);
    }
    
    public Vec3d inverseTransformLocalVecNonScale(Vec3d localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vector3f temp = new Vector3f(localVec);
        Quaternion r = new Quaternion(rotation);//copy() is client only
        r.conjugate();
        temp.rotate(r);
        return new Vec3d(temp);
    }
    
    public Vec3d inverseTransformLocalVec(Vec3d localVec) {
        return inverseTransformLocalVecNonScale(localVec).multiply(1.0 / scaling);
    }
    
    public Vec3d inverseTransformPoint(Vec3d point) {
        return getOriginPos().add(inverseTransformLocalVec(point.subtract(getDestPos())));
    }
    
    public Vec3d scaleLocalVec(Vec3d localVec) {
        if (scaling == 1.0) {
            return localVec;
        }
        
        return localVec.multiply(scaling);
    }
    
    public Box getThinAreaBox() {
        return new Box(
            getPointInPlane(width / 2, height / 2),
            getPointInPlane(-width / 2, -height / 2)
        );
    }
    
    public boolean isPointInPortalProjection(Vec3d pos) {
        Vec3d offset = pos.subtract(getOriginPos());
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        boolean roughResult = Math.abs(xInPlane) < (width / 2 + 0.1) &&
            Math.abs(yInPlane) < (height / 2 + 0.1);
        
        if (roughResult && specialShape != null) {
            return specialShape.triangles.stream()
                .anyMatch(triangle ->
                    triangle.isPointInTriangle(xInPlane, yInPlane)
                );
        }
        
        return roughResult;
    }
    
    public boolean isMovedThroughPortal(
        Vec3d lastTickPos,
        Vec3d pos
    ) {
        return rayTrace(lastTickPos, pos) != null;
    }
    
    public Vec3d rayTrace(
        Vec3d from,
        Vec3d to
    ) {
        double lastDistance = getDistanceToPlane(from);
        double nowDistance = getDistanceToPlane(to);
        
        if (!(lastDistance > 0 && nowDistance < 0)) {
            return null;
        }
        
        Vec3d lineOrigin = from;
        Vec3d lineDirection = to.subtract(from).normalize();
        
        double collidingT = Helper.getCollidingT(getOriginPos(), normal, lineOrigin, lineDirection);
        Vec3d collidingPoint = lineOrigin.add(lineDirection.multiply(collidingT));
        
        if (isPointInPortalProjection(collidingPoint)) {
            return collidingPoint;
        }
        else {
            return null;
        }
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(
        Vec3d point
    ) {
        double distanceToPlane = getDistanceToPlane(point);
        Vec3d posInPlane = point.add(getNormal().multiply(-distanceToPlane));
        Vec3d localPos = posInPlane.subtract(getOriginPos());
        double localX = localPos.dotProduct(axisW);
        double localY = localPos.dotProduct(axisH);
        double distanceToRect = getDistanceToRectangle(
            localX, localY,
            -(width / 2), -(height / 2),
            (width / 2), (height / 2)
        );
        return Math.sqrt(distanceToPlane * distanceToPlane + distanceToRect * distanceToRect);
    }
    
    public static double getDistanceToRectangle(
        double pointX, double pointY,
        double rectAX, double rectAY,
        double rectBX, double rectBY
    ) {
        assert rectAX <= rectBX;
        assert rectAY <= rectBY;
        
        double wx1 = rectAX - pointX;
        double wx2 = rectBX - pointX;
        double dx = (wx1 * wx2 < 0 ? 0 : Math.min(Math.abs(wx1), Math.abs(wx2)));
        
        double wy1 = rectAY - pointY;
        double wy2 = rectBY - pointY;
        double dy = (wy1 * wy2 < 0 ? 0 : Math.min(Math.abs(wy1), Math.abs(wy2)));
        
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public Vec3d getPointInPortalProjection(Vec3d pos) {
        Vec3d myPos = getOriginPos();
        Vec3d offset = pos.subtract(myPos);
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        return myPos.add(
            axisW.multiply(xInPlane)
        ).add(
            axisH.multiply(yInPlane)
        );
    }
    
    public World getDestinationWorld() {
        return getDestinationWorld(world.isClient());
    }
    
    private World getDestinationWorld(boolean isClient) {
        if (isClient) {
            return CHelper.getClientWorld(dimensionTo);
        }
        else {
            return McHelper.getServer().getWorld(dimensionTo);
        }
    }
    
    public static boolean isParallelPortal(Portal currPortal, Portal outerPortal) {
        return currPortal.world.getRegistryKey() == outerPortal.dimensionTo &&
            currPortal.dimensionTo == outerPortal.world.getRegistryKey() &&
            !(currPortal.getNormal().dotProduct(outerPortal.getContentDirection()) > -0.9) &&
            !outerPortal.isInside(currPortal.getOriginPos(), 0.1);
    }
    
    public static boolean isParallelOrientedPortal(Portal currPortal, Portal outerPortal) {
        return currPortal.world.getRegistryKey() == outerPortal.dimensionTo &&
            currPortal.getNormal().dotProduct(outerPortal.getContentDirection()) <= -0.9 &&
            !outerPortal.isInside(currPortal.getOriginPos(), 0.1);
    }
    
    public static boolean isReversePortal(Portal a, Portal b) {
        return a.dimensionTo == b.world.getRegistryKey() &&
            a.world.getRegistryKey() == b.dimensionTo &&
            a.getOriginPos().distanceTo(b.getDestPos()) < 1 &&
            a.getDestPos().distanceTo(b.getOriginPos()) < 1 &&
            a.getNormal().dotProduct(b.getContentDirection()) > 0.5;
    }
    
    public static boolean isFlippedPortal(Portal a, Portal b) {
        if (a == b) {
            return false;
        }
        return a.world == b.world &&
            a.dimensionTo == b.dimensionTo &&
            a.getOriginPos().distanceTo(b.getOriginPos()) < 1 &&
            a.getDestPos().distanceTo(b.getDestPos()) < 1 &&
            a.getNormal().dotProduct(b.getNormal()) < -0.5;
    }
    
    @Override
    public double getDestAreaRadiusEstimation() {
        return Math.max(this.width, this.height) * this.scaling;
    }
    
    public Matrix3f getOuterOrientationMatrix() {
        //transformation: x*axisW+y*axisH+z*normal
        final Matrix3f matrix3f = new Matrix3f();
        matrix3f.set(0, 0, (float) axisW.getX());
        matrix3f.set(0, 1, (float) axisW.getZ());
        matrix3f.set(0, 2, (float) axisW.getY());
        matrix3f.set(1, 0, (float) axisH.getX());
        matrix3f.set(1, 1, (float) axisH.getY());
        matrix3f.set(1, 2, (float) axisH.getZ());
        matrix3f.set(2, 0, (float) getNormal().getX());
        matrix3f.set(2, 1, (float) getNormal().getY());
        matrix3f.set(2, 2, (float) getNormal().getZ());
        return matrix3f;
    }
    
    public Matrix3f getInnerOrientationMatrix() {
        Matrix3f matrix3f;
        if (rotation != null) {
            matrix3f = new Matrix3f(rotation);
        }
        else {
            matrix3f = new Matrix3f();
            matrix3f.loadIdentity();
        }
        matrix3f.multiply((float) scaling);
        return matrix3f;
    }
    
    @Override
    public Vec3d getOriginPos() {
        return getPos();
    }
    
    /**
     * The destination position
     */
    @Override
    public Vec3d getDestPos() {
        return destination;
    }
    
    public void setDestination(Vec3d destination) {
        this.destination = destination;
    }
    
    @Override
    public boolean isConventionalPortal() {
        return true;
    }
    
    @Override
    public Box getExactAreaBox() {
        return getExactBoundingBox();
    }
    
    @Override
    public World getOriginWorld() {
        return world;
    }
    
    @Override
    public World getDestWorld() {
        return getDestinationWorld();
    }
    
    @Override
    public RegistryKey<World> getDestDim() {
        return dimensionTo;
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vec3d cameraPos) {
        return isInFrontOfPortal(cameraPos);
    }
    
    @Nullable
    @Override
    public Quaternion getRotation() {
        return rotation;
    }
    
    @Override
    public double getScale() {
        return scaling;
    }
    
    @Override
    public boolean getIsGlobal() {
        return isGlobalPortal;
    }
    
    @Nullable
    @Override
    public Plane getInnerClipping() {
        return new Plane(getDestPos(), getContentDirection());
    }
    
    //3  2
    //1  0
    @Nullable
    @Override
    public Vec3d[] getOuterFrustumCullingVertices() {
        return getFourVerticesLocalCullable(0);
    }
    
    @Override
    public void renderViewAreaMesh(Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput) {
        if (this instanceof Mirror) {
            //rendering portal behind translucent objects with shader is broken
            double mirrorOffset =
                (OFInterface.isShaders.getAsBoolean() || Global.pureMirror) ? 0.01 : -0.01;
            posInPlayerCoordinate = posInPlayerCoordinate.add(
                ((Mirror) this).getNormal().multiply(mirrorOffset));
        }
        
        ViewAreaRenderer.generateViewAreaTriangles(this, posInPlayerCoordinate, vertexOutput);
    }
    
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicate getInnerFrustumCullingFunc(
        double cameraX, double cameraY, double cameraZ
    ) {
    
        Vec3d portalOriginInLocalCoordinate = getDestPos().add(
            -cameraX, -cameraY, -cameraZ
        );
        Vec3d[] innerFrustumCullingVertices = getFourVerticesLocalRotated(0);
        if (innerFrustumCullingVertices == null) {
            return BoxPredicate.nonePredicate;
        }
        Vec3d[] downLeftUpRightPlaneNormals = FrustumCuller.getDownLeftUpRightPlaneNormals(
            portalOriginInLocalCoordinate,
            innerFrustumCullingVertices
        );
        
        Vec3d downPlane = downLeftUpRightPlaneNormals[0];
        Vec3d leftPlane = downLeftUpRightPlaneNormals[1];
        Vec3d upPlane = downLeftUpRightPlaneNormals[2];
        Vec3d rightPlane = downLeftUpRightPlaneNormals[3];
        
        return
            (double minX, double minY, double minZ, double maxX, double maxY, double maxZ) ->
                FrustumCuller.isFullyOutsideFrustum(
                    minX, minY, minZ, maxX, maxY, maxZ,
                    leftPlane, rightPlane, upPlane, downPlane
                );
    }
    
    public static class TransformationDesc {
        public final RegistryKey<World> dimensionTo;
        @Nullable
        public final Quaternion rotation;
        public final double scaling;
        public final Vec3d offset;
        public final boolean isMirror;
        
        public TransformationDesc(
            RegistryKey<World> dimensionTo,
            @Nullable Quaternion rotation, double scaling,
            Vec3d offset, boolean isMirror
        ) {
            this.dimensionTo = dimensionTo;
            this.rotation = rotation;
            this.scaling = scaling;
            this.offset = offset;
            this.isMirror = isMirror;
        }
        
        private static boolean rotationRoughlyEquals(Quaternion a, Quaternion b) {
            if (a == null && b == null) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            
            return RotationHelper.isClose(a, b, 0.01f);
        }
        
        //roughly equals
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransformationDesc that = (TransformationDesc) o;
            
            if (isMirror || that.isMirror) {
                return false;
            }
            
            return Double.compare(that.scaling, scaling) == 0 &&
                dimensionTo == that.dimensionTo &&
                rotationRoughlyEquals(rotation, that.rotation) &&//approximately
                offset.squaredDistanceTo(that.offset) < 0.01;//approximately
        }
        
        @Override
        public int hashCode() {
            throw new RuntimeException("This cannot be put into a container");
        }
    }
    
    public TransformationDesc getTransformationDesc() {
        return new TransformationDesc(
            getDestDim(),
            getRotation(),
            getScale(),
            getDestPos().multiply(1.0 / getScale()).subtract(getOriginPos()),
            this instanceof Mirror
        );
    }
    
    @Override
    public boolean isParallelWith(Portal portal) {
        return isParallelOrientedPortal(portal, this);
    }
    
    @Override
    public void remove() {
        super.remove();
        portalDisposeSignal.emit(this);
    }
    
    public boolean hasCrossPortalCollision() {
        return true;
    }
    
    @Environment(EnvType.CLIENT)
    public PortalLike getRenderingDelegate() {
        if (Global.mergePortalRendering) {
            PortalRenderingGroup group = PortalRenderInfo.getGroupOf(this);
            if (group != null) {
                return group;
            }
            else {
                return this;
            }
        }
        else {
            return this;
        }
    }
}
