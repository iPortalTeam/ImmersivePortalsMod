package qouteall.imm_ptl.core.portal;

import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.OFInterface;
import qouteall.imm_ptl.core.PehkuiInterface;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.mc_utils.IPEntityEventListenableEntity;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.BoxPredicate;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.RotationHelper;
import qouteall.q_misc_util.my_util.SignalArged;
import qouteall.q_misc_util.my_util.SignalBiArged;
import qouteall.imm_ptl.core.render.FrustumCuller;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Portal entity. Global portals are also entities but not added into world.
 */
public class Portal extends Entity implements PortalLike, IPEntityEventListenableEntity {
    public static EntityType<Portal> entityType;
    
    public static final UUID nullUUID = Util.NIL_UUID;
    private static final Box nullBox = new Box(0, 0, 0, 0, 0, 0);
    
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
    
    
    /**
     * The destination position
     */
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
    
    /**
     * The bounding box. Expanded very little.
     */
    private Box exactBoundingBoxCache;
    
    /**
     * The bounding box. Expanded a little.
     */
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
    
    /**
     * Non-global portals are normal entities,
     * global portals are in GlobalPortalStorage and always loaded
     */
    public boolean isGlobalPortal = false;
    
    /**
     * If true, the portal rendering will not render the sky and maintain the outer world depth.
     * So that the things inside portal will look fused with the things outside portal
     */
    public boolean fuseView = false;
    
    /**
     * If true, if the portal touches another portal that have the same spacial transformation,
     * these two portals' rendering will be merged.
     * It can improve rendering performance.
     * However, the merged rendering's front clipping won't work as if they are separately rendered.
     * So this is by default disabled.
     */
    public boolean renderingMergable = false;
    
    /**
     * If false, the cross portal collision will be ignored
     */
    public boolean hasCrossPortalCollision = true;// TODO make protected
    
    /**
     * Whether to render player inside this portal
     */
    public boolean doRenderPlayer = true;
    
    @Nullable
    public List<String> commandsOnTeleported;
    
    @Environment(EnvType.CLIENT)
    PortalRenderInfo portalRenderInfo;
    
    public static final SignalArged<Portal> clientPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> serverPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> portalCacheUpdateSignal = new SignalArged<>();
    public static final SignalArged<Portal> portalDisposeSignal = new SignalArged<>();
    public static final SignalBiArged<Portal, NbtCompound> readPortalDataSignal = new SignalBiArged<>();
    public static final SignalBiArged<Portal, NbtCompound> writePortalDataSignal = new SignalBiArged<>();
    
    public Portal(
        EntityType<?> entityType, World world
    ) {
        super(entityType, world);
    }
    
    @Override
    public void ip_onEntityPositionUpdated() {
        updateCache();
    }
    
    @Override
    public void ip_onRemoved(RemovalReason reason) {
        portalDisposeSignal.emit(this);
    }
    
    /**
     * @param pos
     * @return use the portal's transformation to transform a point
     */
    @Override
    public Vec3d transformPoint(Vec3d pos) {
        Vec3d localPos = pos.subtract(getOriginPos());
        
        return transformLocalVec(localPos).add(getDestPos());
    }
    
    /**
     * Transform a vector in portal-centered coordinate (without translation transformation)
     */
    @Override
    public Vec3d transformLocalVec(Vec3d localVec) {
        return transformLocalVecNonScale(localVec).multiply(scaling);
    }
    
    /**
     * @return The normal vector of the portal plane
     */
    public Vec3d getNormal() {
        if (normal == null) {
            normal = axisW.crossProduct(axisH).normalize();
        }
        return normal;
    }
    
    /**
     * @return The direction of "portal content", the "direction" of the "inner world view"
     */
    public Vec3d getContentDirection() {
        if (contentDirection == null) {
            contentDirection = transformLocalVecNonScale(getNormal().multiply(-1));
        }
        return contentDirection;
    }
    
    /**
     * Will be invoked when an entity teleports through this portal on server side.
     * This method can be overridden.
     */
    public void onEntityTeleportedOnServer(Entity entity) {
        if (commandsOnTeleported != null) {
            McHelper.invokeCommandAs(entity, commandsOnTeleported);

//            ServerCommandSource commandSource =
//                entity.getCommandSource().withLevel(2).withSilent();
//
//            CommandManager commandManager = McHelper.getServer().getCommandManager();
//            for (String command : commandsOnTeleported) {
//                commandManager.execute(commandSource, command);
//            }
        }
    }
    
    /**
     * Update the portal's cache and send the entity spawn packet to client again
     * Call this when you changed the portal after spawning the portal
     */
    public void reloadAndSyncToClient() {
        Validate.isTrue(!isGlobalPortal);
        Validate.isTrue(!world.isClient(), "must be used on server side");
        updateCache();
        McHelper.getIEStorage(this.world.getRegistryKey()).resendSpawnPacketToTrackers(this);
    }
    
    /**
     * The bounding box, normal vector, content direction vector is cached
     * If the portal attributes get changed, these cache should be updated
     */
    public void updateCache() {
        if (axisW == null) {
            return;
        }
        
        boolean updates =
            boundingBoxCache != null || exactBoundingBoxCache != null ||
                normal != null || contentDirection != null;
        
        boundingBoxCache = null;
        exactBoundingBoxCache = null;
        normal = null;
        contentDirection = null;
        
        if (updates) {
            portalCacheUpdateSignal.emit(this);
        }
    }
    
    @Override
    public Box getBoundingBox() {
        if (boundingBoxCache == null) {
            boundingBoxCache = calculateBoundingBox();
            if (boundingBoxCache == null) {
                Helper.err("Cannot calc portal bounding box");
                return nullBox;
            }
        }
        return boundingBoxCache;
    }
    
    /**
     * @return The portal center
     */
    @Override
    public Vec3d getOriginPos() {
        return getPos();
    }
    
    /**
     * @return The destination position
     */
    @Override
    public Vec3d getDestPos() {
        return destination;
    }
    
    /**
     * Set the portal's center position
     */
    public void setOriginPos(Vec3d pos) {
        setPosition(pos.x, pos.y, pos.z);
        // it will call setPos and update the cache
    }
    
    /**
     * Set the destination dimension
     * If the dimension does not exist, the portal is invalid and will be automatically removed
     */
    public void setDestinationDimension(RegistryKey<World> dim) {
        dimensionTo = dim;
    }
    
    /**
     * Set the portal's destination
     */
    public void setDestination(Vec3d destination) {
        this.destination = destination;
        updateCache();
    }
    
    /**
     * @param portalPosRelativeToCamera The portal position relative to camera
     * @param vertexOutput              Output the triangles that constitute the portal view area.
     *                                  Every 3 vertices correspond to a triangle.
     *                                  In camera-centered coordinate.
     */
    @Environment(EnvType.CLIENT)
    @Override
    public void renderViewAreaMesh(Vec3d portalPosRelativeToCamera, Consumer<Vec3d> vertexOutput) {
        if (this instanceof Mirror) {
            //rendering portal behind translucent objects with shader is broken
            boolean offsetFront = IrisInterface.invoker.isShaders()
                || IPGlobal.pureMirror;
            double mirrorOffset = offsetFront ? 0.01 : -0.01;
            portalPosRelativeToCamera = portalPosRelativeToCamera.add(
                ((Mirror) this).getNormal().multiply(mirrorOffset));
        }
        
        ViewAreaRenderer.generateViewAreaTriangles(this, portalPosRelativeToCamera, vertexOutput);
    }
    
    @Override
    public boolean isFuseView() {
        return fuseView;
    }
    
    public boolean isRenderingMergable() {
        return renderingMergable;
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
    public double getScale() {
        return scaling;
    }
    
    @Override
    public boolean getIsGlobal() {
        return isGlobalPortal;
    }
    
    /**
     * @param entity
     * @return Can the portal teleport this entity.
     */
    public boolean canTeleportEntity(Entity entity) {
        if (!teleportable) {
            return false;
        }
        if (entity instanceof Portal) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
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
    
    @Nullable
    @Override
    public Quaternion getRotation() {
        return rotation;
    }
    
    @Override
    public boolean getDoRenderPlayer() {
        return doRenderPlayer;
    }
    
    public void setOrientationAndSize(
        Vec3d newAxisW, Vec3d newAxisH,
        double newWidth, double newHeight
    ) {
        setOrientation(newAxisW, newAxisH);
        width = newWidth;
        height = newHeight;
        
        updateCache();
    }
    
    public void setOrientation(Vec3d newAxisW, Vec3d newAxisH) {
        axisW = newAxisW;
        axisH = newAxisH;
        updateCache();
    }
    
    public void setRotationTransformation(Quaternion quaternion) {
        rotation = quaternion;
        updateCache();
    }
    
    public void setScaleTransformation(double newScale) {
        scaling = newScale;
    }
    
    @Override
    protected void readCustomDataFromNbt(NbtCompound compoundTag) {
        width = compoundTag.getDouble("width");
        height = compoundTag.getDouble("height");
        axisW = Helper.getVec3d(compoundTag, "axisW").normalize();
        axisH = Helper.getVec3d(compoundTag, "axisH").normalize();
        dimensionTo = DimId.getWorldId(compoundTag, "dimensionTo", world.isClient);
        destination = (Helper.getVec3d(compoundTag, "destination"));
        specificPlayerId = Helper.getUuid(compoundTag, "specificPlayer");
        if (compoundTag.contains("specialShape")) {
            specialShape = new GeometryPortalShape(
                compoundTag.getList("specialShape", 6)
            );
            
            if (specialShape.triangles.isEmpty()) {
                specialShape = null;
            }
            else {
                if (!specialShape.isValid()) {
                    Helper.err("Portal shape invalid ");
                    specialShape = null;
                }
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
        
        if (compoundTag.contains("fuseView")) {
            fuseView = compoundTag.getBoolean("fuseView");
        }
        
        if (compoundTag.contains("renderingMergable")) {
            renderingMergable = compoundTag.getBoolean("renderingMergable");
        }
        
        if (compoundTag.contains("hasCrossPortalCollision")) {
            hasCrossPortalCollision = compoundTag.getBoolean("hasCrossPortalCollision");
        }
        
        if (compoundTag.contains("commandsOnTeleported")) {
            NbtList list = compoundTag.getList("commandsOnTeleported", 8);
            commandsOnTeleported = list.stream()
                .map(t -> ((NbtString) t).asString()).collect(Collectors.toList());
        }
        else {
            commandsOnTeleported = null;
        }
        
        if (compoundTag.contains("doRenderPlayer")) {
            doRenderPlayer = compoundTag.getBoolean("doRenderPlayer");
        }
        
        readPortalDataSignal.emit(this, compoundTag);
        
        updateCache();
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound compoundTag) {
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
        
        compoundTag.putBoolean("fuseView", fuseView);
        
        compoundTag.putBoolean("renderingMergable", renderingMergable);
        
        compoundTag.putBoolean("hasCrossPortalCollision", hasCrossPortalCollision);
        
        compoundTag.putBoolean("doRenderPlayer", doRenderPlayer);
        
        if (commandsOnTeleported != null) {
            NbtList list = new NbtList();
            for (String command : commandsOnTeleported) {
                list.add(NbtString.of(command));
            }
            compoundTag.put(
                "commandsOnTeleported",
                list
            );
        }
        
        writePortalDataSignal.emit(this, compoundTag);
        
    }
    
    private void initDefaultCullableRange() {
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
        return IPNetworking.createStcSpawnEntity(this);
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
        if (getBoundingBox().equals(nullBox)) {
            Helper.err("Abnormal bounding box " + this);
        }
        
        if (world.isClient) {
            clientPortalTickSignal.emit(this);
        }
        else {
            if (!isPortalValid()) {
                Helper.log("removed invalid portal" + this);
                remove(RemovalReason.KILLED);
                return;
            }
            serverPortalTickSignal.emit(this);
        }
        
        CollisionHelper.notifyCollidingPortals(this);
    }
    
    @Override
    protected Box calculateBoundingBox() {
        if (axisW == null) {
            // it may be called when the portal is not yet initialized
            boundingBoxCache = null;
            return nullBox;
        }
        if (boundingBoxCache == null) {
            double w = width;
            double h = height;
            if (shouldLimitBoundingBox()) {
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
    
    protected boolean shouldLimitBoundingBox() {
        return !getIsGlobal();
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
    public void move(MovementType type, Vec3d movement) {
        //portal cannot be moved
    }
    
    /**
     * Invalid portals will be automatically removed
     */
    public boolean isPortalValid() {
        boolean valid = dimensionTo != null &&
            width != 0 &&
            height != 0 &&
            axisW != null &&
            axisH != null &&
            getDestPos() != null &&
            axisW.lengthSquared() > 0.9 &&
            axisH.lengthSquared() > 0.9 &&
            getY() > -1000;
        if (valid) {
            if (world instanceof ServerWorld) {
                ServerWorld destWorld = MiscHelper.getServer().getWorld(dimensionTo);
                if (destWorld == null) {
                    Helper.err("Missing Dimension " + dimensionTo.getValue());
                    return false;
                }
                boolean inWorldBorder = destWorld.getWorldBorder().contains(new BlockPos(getDestPos()));
                if (!inWorldBorder) {
                    Helper.err("Destination out of world border " + this);
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * @return A UUID for discriminating portal rendering units.
     */
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return getUuid();
    }
    
    @Override
    public String toString() {
        return String.format(
            "%s{%s,%s,(%s %s %s %s)->(%s %s %s %s)%s%s%s}",
            getClass().getSimpleName(),
            getId(),
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
        
        // avoid cannot push minecart out of nether portal
        if (entity instanceof AbstractMinecartEntity && entity.getVelocity().lengthSquared() < 0.5) {
            entity.setVelocity(entity.getVelocity().multiply(2));
        }
    }
    
    /**
     * @param pos
     * @return the distance to the portal plane without regarding the shape
     */
    public double getDistanceToPlane(Vec3d pos) {
        return pos.subtract(getOriginPos()).dotProduct(getNormal());
    }
    
    /**
     * @param pos
     * @return is the point in front of the portal plane without regarding the shape
     */
    public boolean isInFrontOfPortal(Vec3d pos) {
        return getDistanceToPlane(pos) > 0;
    }
    
    /**
     * @param xInPlane
     * @param yInPlane
     * @return Convert the 2D coordinate in portal plane into the world coordinate
     */
    public Vec3d getPointInPlane(double xInPlane, double yInPlane) {
        return getOriginPos().add(getPointInPlaneLocal(xInPlane, yInPlane));
    }
    
    /**
     * @param xInPlane
     * @param yInPlane
     * @return Convert the 2D coordinate in portal plane into the portal-centered coordinate
     */
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
    
    /**
     * Transform a point regardless of rotation transformation and scale transformation
     */
    public final Vec3d transformPointRough(Vec3d pos) {
        Vec3d offset = getDestPos().subtract(getOriginPos());
        return pos.add(offset);
    }
    
    public Vec3d transformLocalVecNonScale(Vec3d localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vec3f temp = new Vec3f(localVec);
        temp.rotate(rotation);
        
        return new Vec3d(temp);
    }
    
    public Vec3d inverseTransformLocalVecNonScale(Vec3d localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vec3f temp = new Vec3f(localVec);
        Quaternion r = new Quaternion(rotation);//copy() is client only
        r.conjugate();
        temp.rotate(r);
        return new Vec3d(temp);
    }
    
    @Override
    public Vec3d inverseTransformLocalVec(Vec3d localVec) {
        return inverseTransformLocalVecNonScale(localVec).multiply(1.0 / scaling);
    }
    
    @Override
    public Vec3d inverseTransformPoint(Vec3d point) {
        return getOriginPos().add(inverseTransformLocalVec(point.subtract(getDestPos())));
    }
    
    public Box getThinAreaBox() {
        return new Box(
            getPointInPlane(width / 2, height / 2),
            getPointInPlane(-width / 2, -height / 2)
        );
    }
    
    /**
     * Project the point into the portal plane, is it in the portal area
     */
    public boolean isPointInPortalProjection(Vec3d pos) {
        Vec3d offset = pos.subtract(getOriginPos());
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        boolean roughResult = Math.abs(xInPlane) < (width / 2 + 0.001) &&
            Math.abs(yInPlane) < (height / 2 + 0.001);
        
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
    
    @Nullable
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
        Vec3d localPos = point.subtract(getOriginPos());
        double localX = localPos.dotProduct(axisW);
        double localY = localPos.dotProduct(axisH);
        double distanceToRect = Helper.getDistanceToRectangle(
            localX, localY,
            -(width / 2), -(height / 2),
            (width / 2), (height / 2)
        );
        return Math.sqrt(distanceToPlane * distanceToPlane + distanceToRect * distanceToRect);
    }
    
    public Vec3d getPointProjectedToPlane(Vec3d pos) {
        Vec3d originPos = getOriginPos();
        Vec3d offset = pos.subtract(originPos);
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        return originPos.add(
            axisW.multiply(xInPlane)
        ).add(
            axisH.multiply(yInPlane)
        );
    }
    
    public Vec3d getNearestPointInPortal(Vec3d pos) {
        Vec3d originPos = getOriginPos();
        Vec3d offset = pos.subtract(originPos);
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        xInPlane = MathHelper.clamp(xInPlane, -width / 2, width / 2);
        yInPlane = MathHelper.clamp(yInPlane, -height / 2, height / 2);
        
        return originPos.add(
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
            return MiscHelper.getServer().getWorld(dimensionTo);
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
    
    @Override
    public boolean isConventionalPortal() {
        return true;
    }
    
    @Override
    public Box getExactAreaBox() {
        return getExactBoundingBox();
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vec3d cameraPos) {
        return isInFrontOfPortal(cameraPos);
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
    
    
    // function return true for culled
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
    public boolean cannotRenderInMe(Portal portal) {
        return isParallelOrientedPortal(portal, this);
    }
    
    public void myUnsetRemoved() {
        unsetRemoved();
    }
    
    @Environment(EnvType.CLIENT)
    public PortalLike getRenderingDelegate() {
        if (IPGlobal.enablePortalRenderingMerge) {
            PortalGroup group = PortalRenderInfo.getGroupOf(this);
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
    
    
    @Override
    public void calculateDimensions() {
        boundingBoxCache = null;
    }
    
    @Override
    protected float getEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0;
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
    
    public boolean canDoOuterFrustumCulling() {
        if (isFuseView()) {
            return false;
        }
        if (specialShape == null) {
            initDefaultCullableRange();
        }
        return cullableXStart != cullableXEnd;
    }
    
    // use canTeleportEntity
    // TODO remove
    @Deprecated
    public boolean isTeleportable() {
        return teleportable;
    }
    
    public static boolean doesPortalBlockEntityView(
        LivingEntity observer, Entity target
    ) {
        observer.world.getProfiler().push("portal_block_view");
        
        List<Portal> viewBlockingPortals = McHelper.findEntitiesByBox(
            Portal.class,
            observer.world,
            observer.getBoundingBox().union(target.getBoundingBox()),
            8,
            p -> p.rayTrace(observer.getCameraPosVec(1), target.getCameraPosVec(1)) != null
        );
        
        observer.world.getProfiler().pop();
        
        return !viewBlockingPortals.isEmpty();
    }
    
    // only for one-faced portal
    public boolean allowOverlappedTeleport() {
        return false;
    }
    
    // can be overridden
    // NOTE you should not add or remove or move entity here
    public void onCollidingWithEntity(Entity entity) {
    
    }
    
    @Override
    public boolean getHasCrossPortalCollision() {
        return hasCrossPortalCollision;
    }
}
