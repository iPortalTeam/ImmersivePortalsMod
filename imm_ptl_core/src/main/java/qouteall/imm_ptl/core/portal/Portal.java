package qouteall.imm_ptl.core.portal;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.portal.animation.DefaultPortalAnimation;
import qouteall.imm_ptl.core.portal.animation.PortalAnimation;
import qouteall.imm_ptl.core.portal.animation.PortalAnimationDriver;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.imm_ptl.core.mc_utils.IPEntityEventListenableEntity;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.imm_ptl.core.render.FrustumCuller;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.teleportation.CollisionHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.*;

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
    private static final AABB nullBox = new AABB(0, 0, 0, 0, 0, 0);
    
    /**
     * The portal area length along axisW
     */
    public double width = 0;
    public double height = 0;
    
    /**
     * axisW and axisH define the orientation of the portal
     * They should be normalized and should be perpendicular to each other
     */
    public Vec3 axisW;
    public Vec3 axisH;
    
    /**
     * The destination dimension
     */
    public ResourceKey<Level> dimensionTo;
    
    
    /**
     * The destination position
     */
    public Vec3 destination;
    
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
    private AABB exactBoundingBoxCache;
    
    /**
     * The bounding box. Expanded a little.
     */
    private AABB boundingBoxCache;
    private Vec3 normal;
    private Vec3 contentDirection;
    
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
    public Quaternion rotation; // TODO make it DQuaternion in MC 1.20 to increase precision
    
    /**
     * The scaling transformation of the portal
     */
    public double scaling = 1.0;
    
    /**
     * Whether the entity scale changes after crossing the portal
     */
    public boolean teleportChangesScale = true;
    
    /**
     * Whether the entity gravity direction changes after crossing the portal
     */
    protected boolean teleportChangesGravity = false;
    
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
    public boolean hasCrossPortalCollision = true;
    
    /**
     * Whether to render player inside this portal
     */
    public boolean doRenderPlayer = true;
    
    /**
     * If it's invisible, it will not be rendered. But collision, teleportation and chunk loading will still work.
     */
    protected boolean visible = true;
    
    @Nullable
    public List<String> commandsOnTeleported;
    
    @Environment(EnvType.CLIENT)
    PortalRenderInfo portalRenderInfo;
    
    public final PortalAnimation animation = new PortalAnimation();
    
    @Nullable
    private PortalState lastTickPortalState;
    @Nullable
    private PortalState thisTickPortalState;
    
    private boolean reloadAndSyncNextTick = false;
    
    public static final SignalArged<Portal> clientPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> serverPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> portalCacheUpdateSignal = new SignalArged<>();
    public static final SignalArged<Portal> portalDisposeSignal = new SignalArged<>();
    public static final SignalBiArged<Portal, CompoundTag> readPortalDataSignal = new SignalBiArged<>();
    public static final SignalBiArged<Portal, CompoundTag> writePortalDataSignal = new SignalBiArged<>();
    
    public Portal(
        EntityType<?> entityType, Level world
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
     * @return use the portal's transformation to transform a point
     */
    @Override
    public Vec3 transformPoint(Vec3 pos) {
        Vec3 localPos = pos.subtract(getOriginPos());
        
        return transformLocalVec(localPos).add(getDestPos());
    }
    
    /**
     * Transform a vector in portal-centered coordinate (without translation transformation)
     */
    @Override
    public Vec3 transformLocalVec(Vec3 localVec) {
        return transformLocalVecNonScale(localVec).scale(scaling);
    }
    
    /**
     * @return The normal vector of the portal plane
     */
    public Vec3 getNormal() {
        if (normal == null) {
            normal = axisW.cross(axisH).normalize();
        }
        return normal;
    }
    
    /**
     * @return The direction of "portal content", the "direction" of the "inner world view"
     */
    public Vec3 getContentDirection() {
        if (contentDirection == null) {
            contentDirection = transformLocalVecNonScale(getNormal().scale(-1));
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
        }
    }
    
    /**
     * Update the portal's cache and send the portal data to client
     * Call this when you changed the portal after spawning the portal
     */
    public void reloadAndSyncToClient() {
        reloadAndSyncNextTick = false;
        
        Validate.isTrue(!isGlobalPortal);
        Validate.isTrue(!level.isClientSide(), "must be used on server side");
        updateCache();
        
        CompoundTag customData = new CompoundTag();
        addAdditionalSaveData(customData);
        
        ClientboundCustomPayloadPacket packet = McRemoteProcedureCall.createPacketToSendToClient(
            "qouteall.imm_ptl.core.portal.Portal.RemoteCallables.acceptPortalDataSync",
            level.dimension(),
            getId(),
            position(),
            customData
        );
        
        McHelper.sendToTrackers(this, packet);
    }
    
    public void reloadAndSyncToClientNextTick() {
        Validate.isTrue(!level.isClientSide(), "must be used on server side");
        reloadAndSyncNextTick = true;
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
        thisTickPortalState = null;
        
        if (updates) {
            portalCacheUpdateSignal.emit(this);
        }
    }
    
    @Override
    public AABB getBoundingBox() {
        if (boundingBoxCache == null) {
            boundingBoxCache = makeBoundingBox();
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
    public Vec3 getOriginPos() {
        return position();
    }
    
    /**
     * @return The destination position
     */
    @Override
    public Vec3 getDestPos() {
        return destination;
    }
    
    /**
     * Set the portal's center position
     */
    public void setOriginPos(Vec3 pos) {
        setPos(pos);
        // it will call setPos and update the cache
    }
    
    /**
     * Set the destination dimension
     * If the dimension does not exist, the portal is invalid and will be automatically removed
     */
    public void setDestinationDimension(ResourceKey<Level> dim) {
        dimensionTo = dim;
    }
    
    /**
     * Set the portal's destination
     */
    public void setDestination(Vec3 destination) {
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
    public void renderViewAreaMesh(Vec3 portalPosRelativeToCamera, Consumer<Vec3> vertexOutput) {
        if (this instanceof Mirror) {
            //rendering portal behind translucent objects with shader is broken
            boolean offsetFront = IrisInterface.invoker.isShaders()
                || IPGlobal.pureMirror;
            double mirrorOffset = offsetFront ? 0.01 : -0.01;
            portalPosRelativeToCamera = portalPosRelativeToCamera.add(
                ((Mirror) this).getNormal().scale(mirrorOffset));
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
    public Level getOriginWorld() {
        return level;
    }
    
    @Override
    public Level getDestWorld() {
        return getDestinationWorld();
    }
    
    @Override
    public ResourceKey<Level> getDestDim() {
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
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    public void setIsVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * @return Can the portal teleport this entity.
     */
    public boolean canTeleportEntity(Entity entity) {
        if (!teleportable) {
            return false;
        }
        if (entity instanceof Portal) {
            return false;
        }
        if (entity instanceof Player) {
            if (specificPlayerId != null) {
                if (!entity.getUUID().equals(specificPlayerId)) {
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
        
        return entity.canChangeDimensions();
    }
    
    /**
     * @return the portal's rotation transformation. may be null
     */
    @Nullable
    @Override
    public Quaternion getRotation() {
        return rotation;
    }
    
    /**
     * @return the portal's rotation transformation. will not be null.
     */
    public DQuaternion getRotationD() {
        if (rotation == null) {
            return DQuaternion.identity;
        }
        else {
            return DQuaternion.fromMcQuaternion(rotation);
        }
    }
    
    @Override
    public boolean getDoRenderPlayer() {
        return doRenderPlayer;
    }
    
    public void setOrientationAndSize(
        Vec3 newAxisW, Vec3 newAxisH,
        double newWidth, double newHeight
    ) {
        setOrientation(newAxisW, newAxisH);
        width = newWidth;
        height = newHeight;
        
        updateCache();
    }
    
    public void setOrientation(Vec3 newAxisW, Vec3 newAxisH) {
        axisW = newAxisW.normalize();
        axisH = newAxisH.normalize();
        updateCache();
    }
    
    public DQuaternion getOrientationRotation() {
        return PortalManipulation.getPortalOrientationQuaternion(axisW, axisH);
    }
    
    public void setOrientationRotation(DQuaternion quaternion) {
        DQuaternion fixed = level.isClientSide() ? quaternion : quaternion.fixFloatingPointErrorAccumulation();
        setOrientation(
            McHelper.getAxisWFromOrientation(fixed),
            McHelper.getAxisHFromOrientation(fixed)
        );
    }
    
    public void setRotationTransformation(@Nullable Quaternion quaternion) {
        if (quaternion != null) {
            rotation = level.isClientSide() ? quaternion :
                DQuaternion.fromMcQuaternion(quaternion)
                    .fixFloatingPointErrorAccumulation().toMcQuaternion();
        }
        else {
            rotation = null;
        }
        updateCache();
    }
    
    public void setRotationTransformationD(@Nullable DQuaternion quaternion) {
        if (quaternion == null) {
            rotation = null;
        }
        else {
            rotation = quaternion.fixFloatingPointErrorAccumulation().toMcQuaternion();
        }
        updateCache();
    }
    
    public void setScaleTransformation(double newScale) {
        scaling = newScale;
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        width = compoundTag.getDouble("width");
        height = compoundTag.getDouble("height");
        axisW = Helper.getVec3d(compoundTag, "axisW").normalize();
        axisH = Helper.getVec3d(compoundTag, "axisH").normalize();
        dimensionTo = DimId.getWorldId(compoundTag, "dimensionTo", level.isClientSide);
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
        else {
            specialShape = null;
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
        else {
            rotation = null;
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
        if (compoundTag.contains("teleportChangesGravity")) {
            teleportChangesGravity = compoundTag.getBoolean("teleportChangesGravity");
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
            ListTag list = compoundTag.getList("commandsOnTeleported", 8);
            commandsOnTeleported = list.stream()
                .map(t -> ((StringTag) t).getAsString()).collect(Collectors.toList());
        }
        else {
            commandsOnTeleported = null;
        }
        
        if (compoundTag.contains("doRenderPlayer")) {
            doRenderPlayer = compoundTag.getBoolean("doRenderPlayer");
        }
        
        if (compoundTag.contains("isVisible")) {
            visible = compoundTag.getBoolean("isVisible");
        }
        else {
            visible = true;
        }
        
        animation.readFromTag(compoundTag);
        
        readPortalDataSignal.emit(this, compoundTag);
        
        updateCache();
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
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
            compoundTag.putDouble("rotationA", rotation.r());
            compoundTag.putDouble("rotationB", rotation.i());
            compoundTag.putDouble("rotationC", rotation.j());
            compoundTag.putDouble("rotationD", rotation.k());
        }
        
        compoundTag.putBoolean("interactable", interactable);
        
        compoundTag.putDouble("scale", scaling);
        compoundTag.putBoolean("teleportChangesScale", teleportChangesScale);
        compoundTag.putBoolean("teleportChangesGravity", teleportChangesGravity);
        
        if (portalTag != null) {
            compoundTag.putString("portalTag", portalTag);
        }
        
        compoundTag.putBoolean("fuseView", fuseView);
        
        compoundTag.putBoolean("renderingMergable", renderingMergable);
        
        compoundTag.putBoolean("hasCrossPortalCollision", hasCrossPortalCollision);
        
        compoundTag.putBoolean("doRenderPlayer", doRenderPlayer);
        
        compoundTag.putBoolean("isVisible", visible);
        
        if (commandsOnTeleported != null) {
            ListTag list = new ListTag();
            for (String command : commandsOnTeleported) {
                list.add(StringTag.valueOf(command));
            }
            compoundTag.put(
                "commandsOnTeleported",
                list
            );
        }
        
        animation.writeToTag(compoundTag);
        
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
    public Packet<?> getAddEntityPacket() {
        return IPNetworking.createStcSpawnEntity(this);
    }
    
    @Override
    public boolean broadcastToPlayer(ServerPlayer spectator) {
        if (specificPlayerId == null) {
            return true;
        }
        return spectator.getUUID().equals(specificPlayerId);
    }
    
    @Override
    public void tick() {
        if (getBoundingBox().equals(nullBox)) {
            Helper.err("Abnormal bounding box " + this);
        }
        
        if (thisTickPortalState == null) {
            thisTickPortalState = getPortalState();
        }
        lastTickPortalState = thisTickPortalState;
        
        if (!level.isClientSide()) {
            if (reloadAndSyncNextTick) {
                reloadAndSyncToClient();
            }
        }
        
        if (level.isClientSide()) {
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
        
        animation.tick(this);
        
        CollisionHelper.notifyCollidingPortals(this);
        
        super.tick();
    }
    
    @Override
    protected AABB makeBoundingBox() {
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
            
            boundingBoxCache = new AABB(
                getPointInPlane(w / 2, h / 2)
                    .add(getNormal().scale(0.2)),
                getPointInPlane(-w / 2, -h / 2)
                    .add(getNormal().scale(-0.2))
            ).minmax(new AABB(
                getPointInPlane(-w / 2, h / 2)
                    .add(getNormal().scale(0.2)),
                getPointInPlane(w / 2, -h / 2)
                    .add(getNormal().scale(-0.2))
            ));
        }
        return boundingBoxCache;
    }
    
    protected boolean shouldLimitBoundingBox() {
        return !getIsGlobal();
    }
    
    public AABB getExactBoundingBox() {
        if (exactBoundingBoxCache == null) {
            exactBoundingBoxCache = new AABB(
                getPointInPlane(width / 2, height / 2)
                    .add(getNormal().scale(0.02)),
                getPointInPlane(-width / 2, -height / 2)
                    .add(getNormal().scale(-0.02))
            ).minmax(new AABB(
                getPointInPlane(-width / 2, height / 2)
                    .add(getNormal().scale(0.02)),
                getPointInPlane(width / 2, -height / 2)
                    .add(getNormal().scale(-0.02))
            ));
        }
        
        return exactBoundingBoxCache;
    }
    
    @Override
    public void move(MoverType type, Vec3 movement) {
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
            axisW.lengthSqr() > 0.9 &&
            axisH.lengthSqr() > 0.9 &&
            getY() > (McHelper.getMinY(level) - 100);
        if (valid) {
            if (level instanceof ServerLevel) {
                ServerLevel destWorld = MiscHelper.getServer().getLevel(dimensionTo);
                if (destWorld == null) {
                    Helper.err("Portal Dest Dimension Missing " + dimensionTo.location());
                    return false;
                }
                boolean inWorldBorder = destWorld.getWorldBorder().isWithinBounds(new BlockPos(getDestPos()));
                if (!inWorldBorder) {
                    Helper.err("Destination out of World Border " + this);
                    return false;
                }
            }
            
            if (level.isClientSide()) {
                return isPortalValidClient();
            }
            
            return true;
        }
        return false;
    }
    
    @Environment(EnvType.CLIENT)
    private boolean isPortalValidClient() {
        boolean contains = ClientWorldLoader.getServerDimensions().contains(dimensionTo);
        if (!contains) {
            Helper.err("Client Portal Dest Dimension Missing " + dimensionTo.location());
        }
        return contains;
    }
    
    /**
     * @return A UUID for discriminating portal rendering units.
     */
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return getUUID();
    }
    
    @Override
    public String toString() {
        return String.format(
            "%s{%s,%s,(%s %s %s %s)->(%s %s %s %s)%s%s%s}",
            getClass().getSimpleName(),
            getId(),
            Direction.getNearest(
                getNormal().x, getNormal().y, getNormal().z
            ),
            level.dimension().location(), (int) getX(), (int) getY(), (int) getZ(),
            dimensionTo.location(), (int) getDestPos().x, (int) getDestPos().y, (int) getDestPos().z,
            specificPlayerId != null ? (",specificAccessor:" + specificPlayerId.toString()) : "",
            hasScaling() ? (",scale:" + scaling) : "",
            portalTag != null ? "," + portalTag : ""
        );
    }
    
    // TODO in 1.20 change this method to: Vec3 transformVelocity(Entity, Vec3)
    //  to handle animated portal teleportation more elegantly
    public void transformVelocity(Entity entity) {
        Vec3 oldVelocity = McHelper.getWorldVelocity(entity);
        if (PehkuiInterface.invoker.isPehkuiPresent()) {
            if (teleportChangesScale) {
                McHelper.setWorldVelocity(entity, transformLocalVecNonScale(oldVelocity));
            }
            else {
                McHelper.setWorldVelocity(entity, transformLocalVec(oldVelocity));
            }
        }
        else {
            McHelper.setWorldVelocity(entity, transformLocalVec(oldVelocity));
        }
        
        final int maxVelocity = 15;
        if (oldVelocity.length() > maxVelocity) {
            // cannot be too fast
            McHelper.setWorldVelocity(
                entity,
                McHelper.getWorldVelocity(entity).normalize().scale(maxVelocity)
            );
        }
        
        // avoid cannot push minecart out of nether portal
        if (entity instanceof AbstractMinecart && oldVelocity.lengthSqr() < 0.5) {
            McHelper.setWorldVelocity(entity, McHelper.getWorldVelocity(entity).scale(2));
        }
    }
    
    /**
     * @param pos
     * @return the distance to the portal plane without regarding the shape
     */
    public double getDistanceToPlane(Vec3 pos) {
        return pos.subtract(getOriginPos()).dot(getNormal());
    }
    
    /**
     * @param pos
     * @return is the point in front of the portal plane without regarding the shape
     */
    public boolean isInFrontOfPortal(Vec3 pos) {
        return getDistanceToPlane(pos) > 0;
    }
    
    /**
     * @param xInPlane
     * @param yInPlane
     * @return Convert the 2D coordinate in portal plane into the world coordinate
     */
    public Vec3 getPointInPlane(double xInPlane, double yInPlane) {
        return getOriginPos().add(getPointInPlaneLocal(xInPlane, yInPlane));
    }
    
    /**
     * @param xInPlane
     * @param yInPlane
     * @return Convert the 2D coordinate in portal plane into the portal-centered coordinate
     */
    public Vec3 getPointInPlaneLocal(double xInPlane, double yInPlane) {
        return axisW.scale(xInPlane).add(axisH.scale(yInPlane));
    }
    
    public Vec3 getPointInPlaneLocalClamped(double xInPlane, double yInPlane) {
        return getPointInPlaneLocal(
            Mth.clamp(xInPlane, -width / 2, width / 2),
            Mth.clamp(yInPlane, -height / 2, height / 2)
        );
    }
    
    //3  2
    //1  0
    public Vec3[] getFourVerticesLocal(double shrinkFactor) {
        Vec3[] vertices = new Vec3[4];
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
    private Vec3[] getFourVerticesLocalRotated(double shrinkFactor) {
        Vec3[] fourVerticesLocal = getFourVerticesLocal(shrinkFactor);
        fourVerticesLocal[0] = transformLocalVec(fourVerticesLocal[0]);
        fourVerticesLocal[1] = transformLocalVec(fourVerticesLocal[1]);
        fourVerticesLocal[2] = transformLocalVec(fourVerticesLocal[2]);
        fourVerticesLocal[3] = transformLocalVec(fourVerticesLocal[3]);
        return fourVerticesLocal;
    }
    
    //3  2
    //1  0
    private Vec3[] getFourVerticesLocalCullable(double shrinkFactor) {
        Vec3[] vertices = new Vec3[4];
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
    public final Vec3 transformPointRough(Vec3 pos) {
        Vec3 offset = getDestPos().subtract(getOriginPos());
        return pos.add(offset);
    }
    
    public Vec3 transformLocalVecNonScale(Vec3 localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vector3f temp = new Vector3f(localVec);
        temp.transform(rotation);
        
        return new Vec3(temp);
    }
    
    public Vec3 inverseTransformLocalVecNonScale(Vec3 localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vector3f temp = new Vector3f(localVec);
        Quaternion r = new Quaternion(rotation);//copy() is client only
        r.conj();
        temp.transform(r);
        return new Vec3(temp);
    }
    
    @Override
    public Vec3 inverseTransformLocalVec(Vec3 localVec) {
        return inverseTransformLocalVecNonScale(localVec).scale(1.0 / scaling);
    }
    
    @Override
    public Vec3 inverseTransformPoint(Vec3 point) {
        return getOriginPos().add(inverseTransformLocalVec(point.subtract(getDestPos())));
    }
    
    public AABB getThinAreaBox() {
        return new AABB(
            getPointInPlane(width / 2, height / 2),
            getPointInPlane(-width / 2, -height / 2)
        );
    }
    
    /**
     * Project the point into the portal plane, is it in the portal area
     */
    public boolean isPointInPortalProjection(Vec3 pos) {
        Vec3 offset = pos.subtract(getOriginPos());
        
        double yInPlane = offset.dot(axisH);
        double xInPlane = offset.dot(axisW);
        
        return isLocalXYOnPortal(xInPlane, yInPlane);
    }
    
    public boolean isLocalXYOnPortal(double xInPlane, double yInPlane) {
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
    
    /**
     * NOTE: This does not count animation into consideration.
     */
    public boolean isMovedThroughPortal(
        Vec3 lastTickPos,
        Vec3 pos
    ) {
        return rayTrace(lastTickPos, pos) != null;
    }
    
    @Nullable
    public Vec3 rayTrace(
        Vec3 from,
        Vec3 to
    ) {
        double lastDistance = getDistanceToPlane(from);
        double nowDistance = getDistanceToPlane(to);
        
        if (!(lastDistance > 0 && nowDistance < 0)) {
            return null;
        }
        
        Vec3 lineOrigin = from;
        Vec3 lineDirection = to.subtract(from).normalize();
        
        double collidingT = Helper.getCollidingT(getOriginPos(), normal, lineOrigin, lineDirection);
        Vec3 collidingPoint = lineOrigin.add(lineDirection.scale(collidingT));
        
        if (isPointInPortalProjection(collidingPoint)) {
            return collidingPoint;
        }
        else {
            return null;
        }
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(
        Vec3 point
    ) {
        double distanceToPlane = getDistanceToPlane(point);
        Vec3 localPos = point.subtract(getOriginPos());
        double localX = localPos.dot(axisW);
        double localY = localPos.dot(axisH);
        double distanceToRect = Helper.getDistanceToRectangle(
            localX, localY,
            -(width / 2), -(height / 2),
            (width / 2), (height / 2)
        );
        return Math.sqrt(distanceToPlane * distanceToPlane + distanceToRect * distanceToRect);
    }
    
    public Vec3 getPointProjectedToPlane(Vec3 pos) {
        Vec3 originPos = getOriginPos();
        Vec3 offset = pos.subtract(originPos);
        
        double yInPlane = offset.dot(axisH);
        double xInPlane = offset.dot(axisW);
        
        return originPos.add(
            axisW.scale(xInPlane)
        ).add(
            axisH.scale(yInPlane)
        );
    }
    
    public Vec3 getNearestPointInPortal(Vec3 pos) {
        Vec3 originPos = getOriginPos();
        Vec3 offset = pos.subtract(originPos);
        
        double yInPlane = offset.dot(axisH);
        double xInPlane = offset.dot(axisW);
        
        xInPlane = Mth.clamp(xInPlane, -width / 2, width / 2);
        yInPlane = Mth.clamp(yInPlane, -height / 2, height / 2);
        
        return originPos.add(
            axisW.scale(xInPlane)
        ).add(
            axisH.scale(yInPlane)
        );
    }
    
    public Level getDestinationWorld() {
        return getDestinationWorld(level.isClientSide());
    }
    
    private Level getDestinationWorld(boolean isClient) {
        if (isClient) {
            return CHelper.getClientWorld(dimensionTo);
        }
        else {
            return MiscHelper.getServer().getLevel(dimensionTo);
        }
    }
    
    public static boolean isParallelPortal(Portal currPortal, Portal outerPortal) {
        return currPortal.level.dimension() == outerPortal.dimensionTo &&
            currPortal.dimensionTo == outerPortal.level.dimension() &&
            !(currPortal.getNormal().dot(outerPortal.getContentDirection()) > -0.9) &&
            !outerPortal.isInside(currPortal.getOriginPos(), 0.1);
    }
    
    public static boolean isParallelOrientedPortal(Portal currPortal, Portal outerPortal) {
        return currPortal.level.dimension() == outerPortal.dimensionTo &&
            currPortal.getNormal().dot(outerPortal.getContentDirection()) < -0.9 &&
            !outerPortal.isInside(currPortal.getOriginPos(), 0.1);
    }
    
    public static boolean isReversePortal(Portal a, Portal b) {
        return a.dimensionTo == b.level.dimension() &&
            a.level.dimension() == b.dimensionTo &&
            a.getOriginPos().distanceTo(b.getDestPos()) < 0.1 &&
            a.getDestPos().distanceTo(b.getOriginPos()) < 0.1 &&
            a.getNormal().dot(b.getContentDirection()) > 0.9;
    }
    
    public static boolean isFlippedPortal(Portal a, Portal b) {
        if (a == b) {
            return false;
        }
        return a.level == b.level &&
            a.dimensionTo == b.dimensionTo &&
            a.getOriginPos().distanceTo(b.getOriginPos()) < 0.1 &&
            a.getDestPos().distanceTo(b.getDestPos()) < 0.1 &&
            a.getNormal().dot(b.getNormal()) < -0.9;
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
    public AABB getExactAreaBox() {
        return getExactBoundingBox();
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vec3 cameraPos) {
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
    public Vec3[] getOuterFrustumCullingVertices() {
        return getFourVerticesLocalCullable(0);
    }
    
    
    // function return true for culled
    @Environment(EnvType.CLIENT)
    @Override
    public BoxPredicate getInnerFrustumCullingFunc(
        double cameraX, double cameraY, double cameraZ
    ) {
        
        Vec3 portalOriginInLocalCoordinate = getDestPos().add(
            -cameraX, -cameraY, -cameraZ
        );
        Vec3[] innerFrustumCullingVertices = getFourVerticesLocalRotated(0);
        if (innerFrustumCullingVertices == null) {
            return BoxPredicate.nonePredicate;
        }
        Vec3[] downLeftUpRightPlaneNormals = FrustumCuller.getDownLeftUpRightPlaneNormals(
            portalOriginInLocalCoordinate,
            innerFrustumCullingVertices
        );
        
        Vec3 downPlane = downLeftUpRightPlaneNormals[0];
        Vec3 leftPlane = downLeftUpRightPlaneNormals[1];
        Vec3 upPlane = downLeftUpRightPlaneNormals[2];
        Vec3 rightPlane = downLeftUpRightPlaneNormals[3];
        
        return
            (double minX, double minY, double minZ, double maxX, double maxY, double maxZ) ->
                FrustumCuller.isFullyOutsideFrustum(
                    minX, minY, minZ, maxX, maxY, maxZ,
                    leftPlane, rightPlane, upPlane, downPlane
                );
    }
    
    public static class TransformationDesc {
        public final ResourceKey<Level> dimensionTo;
        @Nullable
        public final Quaternion rotation;
        public final double scaling;
        public final Vec3 offset;
        public final boolean isMirror;
        
        public TransformationDesc(
            ResourceKey<Level> dimensionTo,
            @Nullable Quaternion rotation, double scaling,
            Vec3 offset, boolean isMirror
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
                offset.distanceToSqr(that.offset) < 0.01;//approximately
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
            getDestPos().scale(1.0 / getScale()).subtract(getOriginPos()),
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
    public void refreshDimensions() {
        boundingBoxCache = null;
    }
    
    @Override
    protected float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0;
    }
    
    
    // Scaling does not interfere camera transformation
    @Override
    @Nullable
    public Matrix4f getAdditionalCameraTransformation() {
        
        return PortalRenderer.getPortalTransformation(this);
    }
    
    
    @Override
    protected void defineSynchedData() {
        //do nothing
    }
    
    public boolean canDoOuterFrustumCulling() {
        if (isFuseView()) {
            return false;
        }
        if (!isVisible()) {
            return false;
        }
        if (specialShape == null) {
            initDefaultCullableRange();
        }
        return cullableXStart != cullableXEnd;
    }
    
    // it's recommended to use canTeleportEntity
    @Deprecated
    public boolean isTeleportable() {
        return teleportable;
    }
    
    public static boolean doesPortalBlockEntityView(
        LivingEntity observer, Entity target
    ) {
        observer.level.getProfiler().push("portal_block_view");
        
        List<Portal> viewBlockingPortals = McHelper.findEntitiesByBox(
            Portal.class,
            observer.level,
            observer.getBoundingBox().minmax(target.getBoundingBox()),
            8,
            p -> p.rayTrace(observer.getEyePosition(1), target.getEyePosition(1)) != null
        );
        
        observer.level.getProfiler().pop();
        
        return !viewBlockingPortals.isEmpty();
    }
    
    // It's overridden by MiniScaled
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
    
    public boolean getTeleportChangesGravity() {
        return teleportChangesGravity;
    }
    
    public void setTeleportChangesGravity(boolean cond) {
        teleportChangesGravity = cond;
    }
    
    public Direction getTransformedGravityDirection(Direction oldGravityDir) {
        Vec3 oldGravityVec = Vec3.atLowerCornerOf(oldGravityDir.getNormal());
        
        Vec3 newGravityVec = transformLocalVecNonScale(oldGravityVec);
        
        return Direction.getNearest(
            newGravityVec.x, newGravityVec.y, newGravityVec.z
        );
    }
    
    // if the portal is not yet initialized, will return null
    @Nullable
    public PortalState getPortalState() {
        if (axisW == null) {
            return null;
        }
        
        return new PortalState(
            level.dimension(),
            getOriginPos(),
            dimensionTo,
            getDestPos(),
            getScale(),
            getRotation() == null ? DQuaternion.identity : DQuaternion.fromMcQuaternion(getRotation()),
            getOrientationRotation(),
            width, height
        );
    }
    
    public void setPortalState(PortalState state) {
        Validate.isTrue(level.dimension() == state.fromWorld);
        Validate.isTrue(dimensionTo == state.toWorld);
        
        width = state.width;
        height = state.height;
        
        setOriginPos(state.fromPos);
        setDestination(state.toPos);
        PortalManipulation.setPortalOrientationQuaternion(this, state.orientation);
        if (DQuaternion.isClose(state.rotation, DQuaternion.identity, 0.00005)) {
            setRotationTransformation(null);
        }
        else {
            setRotationTransformation(state.rotation.toMcQuaternion());
        }
        
        setScaleTransformation(state.scaling);
    }
    
    
    @Environment(EnvType.CLIENT)
    private void acceptDataSync(Vec3 pos, CompoundTag customData) {
        PortalState oldState = getPortalState();
        
        setPos(pos);
        readAdditionalSaveData(customData);
        
        if (animation.defaultAnimation.durationTicks > 0) {
            animation.defaultAnimation.startClientDefaultAnimation(this, oldState);
        }
    }
    
    public CompoundTag writePortalDataToNbt() {
        CompoundTag nbtCompound = new CompoundTag();
        addAdditionalSaveData(nbtCompound);
        return nbtCompound;
    }
    
    public void readPortalDataFromNbt(CompoundTag compound) {
        readAdditionalSaveData(compound);
    }
    
    @Deprecated
    public void rectifyClusterPortals() {
        PortalExtension.get(this).rectifyClusterPortals(this, true);
    }
    
    public void rectifyClusterPortals(boolean sync) {
        PortalExtension.get(this).rectifyClusterPortals(this, sync);
    }
    
    public DefaultPortalAnimation getDefaultAnimation() {
        return animation.defaultAnimation;
    }
    
    public void clearAnimationDrivers() {
        animation.clearAnimationDrivers(this, true, true);
    }
    
    public void addThisSideAnimationDriver(PortalAnimationDriver driver) {
        animation.addThisSideAnimationDriver(this, driver);
    }
    
    public void addOtherSideAnimationDriver(PortalAnimationDriver driver) {
        animation.addOtherSideAnimationDriver(this, driver);
    }
    
    public boolean isOtherSideChunkLoaded() {
        Validate.isTrue(!level.isClientSide());
        ChunkPos destChunkPos = new ChunkPos(new BlockPos(getDestPos()));
        LevelChunk chunk = McHelper.getServerChunkIfPresent(
            dimensionTo, destChunkPos.x, destChunkPos.z
        );
        
        if (chunk == null) {
            return false;
        }
        
        boolean entitiesLoaded = ((ServerLevel) getDestWorld()).areEntitiesLoaded(destChunkPos.toLong());
        
        return entitiesLoaded;
    }
    
    // return null if the portal is not yet initialized
    @Nullable
    public PortalState getLastTickPortalState() {
        if (lastTickPortalState == null) {
            return getThisTickPortalState();
        }
        return lastTickPortalState;
    }
    
    // return null if the portal is not yet initialized
    @Nullable
    public PortalState getThisTickPortalState() {
        if (thisTickPortalState == null) {
            thisTickPortalState = getPortalState();
        }
        return thisTickPortalState;
    }
    
    public Vec3 transformFromPortalLocalToWorld(Vec3 localPos) {
        return axisW.scale(localPos.x).add(axisH.scale(localPos.y)).add(getNormal().scale(localPos.z)).add(getOriginPos());
    }
    
    public Vec3 transformFromWorldToPortalLocal(Vec3 worldPos) {
        Vec3 relativePos = worldPos.subtract(getOriginPos());
        return new Vec3(
            relativePos.dot(axisW),
            relativePos.dot(axisH),
            relativePos.dot(getNormal())
        );
    }
    
    public static class RemoteCallables {
        public static void acceptPortalDataSync(
            ResourceKey<Level> dim,
            int entityId,
            Vec3 pos,
            CompoundTag customData
        ) {
            ClientLevel world = ClientWorldLoader.getWorld(dim);
            Entity entity = world.getEntity(entityId);
            if (entity instanceof Portal portal) {
                portal.acceptDataSync(pos, customData);
            }
            else {
                Helper.err("missing portal entity to sync " + entityId);
            }
        }
    }
    
}
