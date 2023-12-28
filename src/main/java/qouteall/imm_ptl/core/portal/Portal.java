package qouteall.imm_ptl.core.portal;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.ImmPtlEntityExtension;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.mc_utils.IPEntityEventListenableEntity;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.imm_ptl.core.mixin.common.entity_sync.MixinServerEntity;
import qouteall.imm_ptl.core.mixin.common.mc_util.MixinEntity_U;
import qouteall.imm_ptl.core.network.ImmPtlNetworking;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.animation.AnimationView;
import qouteall.imm_ptl.core.portal.animation.DefaultPortalAnimation;
import qouteall.imm_ptl.core.portal.animation.PortalAnimation;
import qouteall.imm_ptl.core.portal.animation.PortalAnimationDriver;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.portal.shape.PortalShape;
import qouteall.imm_ptl.core.portal.shape.PortalShapeSerialization;
import qouteall.imm_ptl.core.portal.shape.RectangularPortalShape;
import qouteall.imm_ptl.core.portal.shape.SpecialFlatPortalShape;
import qouteall.imm_ptl.core.render.PortalGroup;
import qouteall.imm_ptl.core.render.PortalRenderable;
import qouteall.imm_ptl.core.render.renderer.PortalRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.BoxPredicate;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Mesh2D;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.RayTraceResult;
import qouteall.q_misc_util.my_util.TriangleConsumer;

import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Portal entity. Global portals are also entities but not added into world.
 */
public class Portal extends Entity implements
    PortalLike, IPEntityEventListenableEntity, PortalRenderable {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final EntityType<Portal> ENTITY_TYPE = createPortalEntityType(Portal::new);
    
    public static final Event<Consumer<Portal>> CLIENT_PORTAL_ACCEPT_SYNC_EVENT =
        Helper.createConsumerEvent();
    public static final Event<Consumer<Portal>> CLIENT_PORTAL_SPAWN_EVENT =
        Helper.createConsumerEvent();
    
    public static <T extends Portal> EntityType<T> createPortalEntityType(
        EntityType.EntityFactory<T> constructor
    ) {
        return FabricEntityTypeBuilder.create(
                MobCategory.MISC,
                constructor
            ).dimensions(
                new EntityDimensions(1, 1, true)
            ).fireImmune()
            .trackRangeBlocks(96)
            .trackedUpdateRate(20)
            .forceTrackedVelocityUpdates(true)
            .build();
    }
    
    private static final AABB NULL_BOX =
        new AABB(0, 0, 0, 0, 0, 0);
    
    protected double width = 0;
    protected double height = 0;
    protected double thickness = 0;
    
    protected Vec3 axisW;
    protected Vec3 axisH;
    
    protected ResourceKey<Level> dimensionTo;
    
    protected Vec3 destination;
    
    protected boolean teleportable = true;
    
    protected @Nullable PortalShape portalShape;
    
    /**
     * If not null, this portal can only be accessed by one player
     * If it's {@link Util#NIL_UUID} the portal can only be accessed by entities
     * TODO change to entity selector in future versions.
     */
    @Nullable
    public UUID specificPlayerId;
    
    @Nullable
    protected DQuaternion rotation;
    
    protected double scaling = 1.0;
    
    protected boolean teleportChangesScale = true;
    
    /**
     * Whether the entity gravity direction changes after crossing the portal
     */
    protected boolean teleportChangesGravity = IPConfig.getConfig().portalsChangeGravityByDefault;
    
    /**
     * Whether the player can place and break blocks across the portal
     */
    protected boolean interactable = true;
    
    PortalExtension extension;
    
    @Nullable
    public String portalTag;
    
    /**
     * Non-global portals are normal entities,
     * global portals are in GlobalPortalStorage and always loaded
     */
    public boolean isGlobalPortal = false;
    
    protected boolean fuseView = false;
    
    /**
     * If true, if the portal touches another portal that have the same spacial transformation,
     * these two portals' rendering will be merged.
     * It can improve rendering performance.
     * However, the merged rendering's front clipping won't work as if they are separately rendered.
     * So this is by default disabled.
     * TODO remove in a future version.
     */
    @Deprecated
    public boolean renderingMergable = false;
    
    protected boolean crossPortalCollisionEnabled = true;
    
    protected boolean doRenderPlayer = true;
    
    /**
     * If it's invisible, it will not be rendered. But collision, teleportation and chunk loading will still work.
     */
    protected boolean visible = true;
    
    @Nullable
    protected List<String> commandsOnTeleported;
    
    @Environment(EnvType.CLIENT)
    PortalRenderInfo portalRenderInfo;
    
    public final PortalAnimation animation = new PortalAnimation();
    
    protected @Nullable PortalState lastTickPortalState;
    
    protected boolean reloadAndSyncNextTick = false;
    
    // these are caches
    private @Nullable AABB exactBoundingBoxCache;
    private @Nullable AABB boundingBoxCache;
    private @Nullable Vec3 normalCache;
    private @Nullable Vec3 contentDirectionCache;
    private @Nullable PortalState portalStateCache;
    private @Nullable VoxelShape thisSideCollisionExclusion;
    private @Nullable UnilateralPortalState thisSideStateCache;
    private @Nullable UnilateralPortalState otherSideStateCache;
    
    public static final Event<Consumer<Portal>> CLIENT_PORTAL_TICK_SIGNAL =
        Helper.createConsumerEvent();
    public static final Event<Consumer<Portal>> SERVER_PORTAL_TICK_SIGNAL =
        Helper.createConsumerEvent();
    
    public static final Event<Consumer<Portal>> PORTAL_DISPOSE_SIGNAL =
        Helper.createConsumerEvent();
    
    public static final Event<BiConsumer<Portal, CompoundTag>> READ_PORTAL_DATA_SIGNAL =
        Helper.createBiConsumerEvent();
    public static final Event<BiConsumer<Portal, CompoundTag>> WRITE_PORTAL_DATA_SIGNAL =
        Helper.createBiConsumerEvent();
    
    public Portal(
        EntityType<?> entityType, Level world
    ) {
        super(entityType, world);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        width = compoundTag.getDouble("width");
        height = compoundTag.getDouble("height");
        thickness = compoundTag.getDouble("thickness");
        axisW = Helper.getVec3d(compoundTag, "axisW").normalize();
        axisH = Helper.getVec3d(compoundTag, "axisH").normalize();
        dimensionTo = Helper.getWorldId(compoundTag, "dimensionTo");
        destination = Helper.getVec3d(compoundTag, "destination");
        specificPlayerId = Helper.getUuid(compoundTag, "specificPlayer");
        
        if (compoundTag.contains("portalShape")) {
            CompoundTag portalShapeTag = compoundTag.getCompound("portalShape");
            PortalShape portalShape = PortalShapeSerialization.deserialize(portalShapeTag);
            if (portalShape == null) {
                LOGGER.error("Cannot deserialize portal shape {}", portalShapeTag);
                this.portalShape = RectangularPortalShape.INSTANCE;
            }
            else {
                this.portalShape = portalShape;
            }
        }
        else {
            // upgrade old data
            Mesh2D mesh2D;
            
            if (compoundTag.contains("specialShape")) {
                // if missing, it will be false
                boolean shapeNormalized = compoundTag.getBoolean("shapeNormalized");
                
                if (shapeNormalized) {
                    mesh2D = GeometryPortalShape.readOldMeshFromTag(
                        compoundTag.getList("specialShape", 6)
                    );
                }
                else {
                    mesh2D = GeometryPortalShape.readOldMeshFromTagNonNormalized(
                        compoundTag.getList("specialShape", 6),
                        width / 2, height / 2
                    );
                }
            }
            else {
                mesh2D = null;
            }
            
            if (mesh2D == null) {
                portalShape = RectangularPortalShape.INSTANCE;
            }
            else {
                portalShape = new SpecialFlatPortalShape(mesh2D);
            }
        }
        
        if (compoundTag.contains("teleportable")) {
            teleportable = compoundTag.getBoolean("teleportable");
        }
        
        if (compoundTag.contains("rotationA")) {
            setRotationTransformationD(new DQuaternion(
                compoundTag.getFloat("rotationB"),
                compoundTag.getFloat("rotationC"),
                compoundTag.getFloat("rotationD"),
                compoundTag.getFloat("rotationA")
            ));
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
        else {
            teleportChangesGravity = IPConfig.getConfig().portalsChangeGravityByDefault;
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
            crossPortalCollisionEnabled = compoundTag.getBoolean("hasCrossPortalCollision");
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
        else {
            doRenderPlayer = true;
        }
        
        if (compoundTag.contains("isVisible")) {
            visible = compoundTag.getBoolean("isVisible");
        }
        else {
            visible = true;
        }
        
        animation.readFromTag(compoundTag);
        
        READ_PORTAL_DATA_SIGNAL.invoker().accept(this, compoundTag);
        
        updateCache();
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putDouble("width", width);
        compoundTag.putDouble("height", height);
        compoundTag.putDouble("thickness", thickness);
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        Helper.putWorldId(compoundTag, "dimensionTo", dimensionTo);
        Helper.putVec3d(compoundTag, "destination", getDestPos());
        
        if (specificPlayerId != null) {
            Helper.putUuid(compoundTag, "specificPlayer", specificPlayerId);
        }
        
        CompoundTag portalShapeTag = PortalShapeSerialization.serialize(getPortalShape());
        compoundTag.put("portalShape", portalShapeTag);
        
        compoundTag.putBoolean("teleportable", teleportable);
        
        if (rotation != null) {
            compoundTag.putDouble("rotationA", rotation.w);
            compoundTag.putDouble("rotationB", rotation.x);
            compoundTag.putDouble("rotationC", rotation.y);
            compoundTag.putDouble("rotationD", rotation.z);
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
        
        compoundTag.putBoolean("hasCrossPortalCollision", crossPortalCollisionEnabled);
        
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
        
        WRITE_PORTAL_DATA_SIGNAL.invoker().accept(this, compoundTag);
        
    }
    
    public @NotNull PortalShape getPortalShape() {
        if (portalShape == null) {
            portalShape = RectangularPortalShape.INSTANCE;
        }
        
        return portalShape;
    }
    
    public void setPortalShape(PortalShape portalShape) {
        this.portalShape = portalShape;
        
        if (portalShape.isPlanar()) {
            thickness = 0;
        }
        
        updateCache();
    }
    
    public void setPortalShapeToDefault() {
        setPortalShape(RectangularPortalShape.INSTANCE);
    }
    
    @Override
    public void ip_onEntityPositionUpdated() {
        updateCache();
    }
    
    @Override
    public void ip_onRemoved(RemovalReason reason) {
        PORTAL_DISPOSE_SIGNAL.invoker().accept(this);
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
     * Note: the normal is no longer the plane normal for 3D portals.
     */
    public Vec3 getNormal() {
        if (normalCache == null) {
            normalCache = axisW.cross(axisH).normalize();
        }
        return normalCache;
    }
    
    /**
     * @return The direction of "portal content", the "direction" of the "inner world view"
     * Note: it should not be used for 3D portals.
     */
    public Vec3 getContentDirection() {
        if (contentDirectionCache == null) {
            contentDirectionCache = transformLocalVecNonScale(getNormal().scale(-1));
        }
        return contentDirectionCache;
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
     * Update the portal's cache and send the portal data to client.
     * Call this when you changed the portal after spawning the portal.
     * Note portal position is not updated via vanilla packet because vanilla packet
     * encodes position in 1/4096 unit which is not precise enough (for portal animation).
     * This is the only place of syncing portal position.
     * The /tp command won't work for portals.
     * {@link MixinServerEntity}
     */
    public void reloadAndSyncToClient() {
        reloadAndSyncNextTick = false;
        
        Validate.isTrue(!isGlobalPortal, "global portal is not synced by this");
        Validate.isTrue(!level().isClientSide(), "must be used on server side");
        updateCache();
        
        var packet = createSyncPacket();
        
        McHelper.sendToTrackers(this, packet);
    }
    
    public void reloadAndSyncToClientNextTick() {
        Validate.isTrue(!level().isClientSide(), "must be used on server side");
        reloadAndSyncNextTick = true;
    }
    
    public void reloadAndSyncClusterToClientNextTick() {
        PortalExtension.forClusterPortals(this, Portal::reloadAndSyncToClientNextTick);
    }
    
    public void reloadAndSyncToClientWithTickDelay(int tickDelay) {
        Validate.isTrue(!level().isClientSide(), "must be used on server side");
        ServerTaskList.of(getServer()).addTask(MyTaskList.withDelay(tickDelay, () -> {
            reloadAndSyncToClientNextTick();
            return true;
        }));
    }
    
    /**
     * The bounding box, normal vector, content direction vector is cached
     * If the portal attributes get changed, these cache should be updated
     */
    public void updateCache() {
        if (axisW == null || axisH == null) {
            return;
        }
        
        portalStateCache = null;
        boundingBoxCache = null;
        exactBoundingBoxCache = null;
        normalCache = null;
        contentDirectionCache = null;
        thisSideCollisionExclusion = null;
        thisSideStateCache = null;
        otherSideStateCache = null;
        
        if (!level().isClientSide()) {
            reloadAndSyncToClientNextTick();
        }
    }
    
    @Override
    public @NotNull AABB getBoundingBox() {
        if (boundingBoxCache == null) {
            boundingBoxCache = makeBoundingBox();
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
    @SuppressWarnings("DanglingJavadoc")
    public void setOriginPos(Vec3 pos) {
        setPos(pos);
        /**In {@link MixinEntity_U} it will update the cache.*/
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
     * If true, the portal rendering will not render the sky and maintain the outer world depth.
     * So that the things inside portal will look fused with the things outside portal
     */
    @Override
    public boolean isFuseView() {
        return fuseView;
    }
    
    @Deprecated
    public boolean isRenderingMergable() {
        return renderingMergable;
    }
    
    /**
     * Determines whether the player should be able to reach through the portal or not.
     * Update: Should use {@link Portal#isInteractableBy(Player)}.
     *
     * @return the interactability of the portal
     */
    @SuppressWarnings("SpellCheckingInspection")
    public boolean isInteractable() {
        return interactable;
    }
    
    /**
     * Changes the reach-through behavior of the portal.
     *
     * @param interactable the interactability of the portal
     */
    @SuppressWarnings("SpellCheckingInspection")
    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
    }
    
    @Override
    public Level getOriginWorld() {
        return level();
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
    
    /**
     * Set whether the portal is visible.
     * Note: Don't use vanilla's {@link Entity#setInvisible(boolean)}. It has no effect on portals.
     */
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
                if (!specificPlayerId.equals(Util.NIL_UUID)) {
                    // it can only be used by the player
                    return false;
                }
            }
        }
        
        if (!O_O.allowTeleportingEntity(entity, this)) {
            return false;
        }
        
        // cannot use entity.canChangeDimensions() because that disables riding entity to go through portal
        return ((ImmPtlEntityExtension) entity).imm_ptl_canTeleportThroughPortal(this);
    }
    
    /**
     * @return Can the entity collide with the portal.
     * Note: {@link Portal#crossPortalCollisionEnabled} determined other-side collision.
     * This limits both this-side and other-side collision.
     */
    public boolean canCollideWithEntity(Entity entity) {
        // by default, a non-teleportable portal disallows collision
        return canTeleportEntity(entity);
    }
    
    /**
     * Can the player interact with blocks through portal
     */
    @SuppressWarnings("SpellCheckingInspection")
    public boolean isInteractableBy(Player player) {
        if (!isInteractable()) {
            return false;
        }
        if (!isVisible()) {
            return false;
        }
        return canTeleportEntity(player);
    }
    
    /**
     * @return the portal's rotation transformation. may be null
     */
    @Nullable
    @Override
    public DQuaternion getRotation() {
        return rotation;
    }
    
    /**
     * @return the portal's rotation transformation. will not be null.
     */
    @NotNull
    public DQuaternion getRotationD() {
        return DQuaternion.fromNullable(getRotation());
    }
    
    @Override
    public boolean getDoRenderPlayer() {
        return doRenderPlayer;
    }
    
    public boolean getTeleportable() {
        return teleportable;
    }
    
    public void setTeleportable(boolean teleportable) {
        this.teleportable = teleportable;
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
    
    public void setWidth(double newWidth) {
        width = newWidth;
        updateCache();
    }
    
    public void setHeight(double newHeight) {
        height = newHeight;
        updateCache();
    }
    
    public void setThickness(double newThickness) {
        thickness = newThickness;
        updateCache();
    }
    
    public void setPortalSize(double newWidth, double newHeight, double newThickness) {
        width = newWidth;
        height = newHeight;
        thickness = newThickness;
        updateCache();
    }
    
    public DQuaternion getOrientationRotation() {
        return PortalManipulation.getPortalOrientationQuaternion(axisW, axisH);
    }
    
    public void setOrientationRotation(DQuaternion quaternion) {
        DQuaternion fixed = level().isClientSide() ? quaternion : quaternion.fixFloatingPointErrorAccumulation();
        setOrientation(
            McHelper.getAxisWFromOrientation(fixed),
            McHelper.getAxisHFromOrientation(fixed)
        );
    }
    
    /**
     * NOTE: This is not the portal orientation. It's the rotation transformation.
     *
     * @param quaternion The new rotation transformation.
     */
    public void setRotation(@Nullable DQuaternion quaternion) {
        setRotationTransformationD(quaternion);
    }
    
    public void setRotationTransformation(@Nullable DQuaternion quaternion) {
        setRotationTransformationD(quaternion);
    }
    
    public void setRotationTransformationD(@Nullable DQuaternion quaternion) {
        if (quaternion == null) {
            rotation = null;
        }
        else {
            rotation = quaternion.fixFloatingPointErrorAccumulation();
        }
        updateCache();
    }
    
    public void setOtherSideOrientation(DQuaternion otherSideOrientation) {
        setRotation(PortalManipulation.computeDeltaTransformation(
            getOrientationRotation(), otherSideOrientation
        ));
    }
    
    public void setScaleTransformation(double newScale) {
        scaling = newScale;
    }
    
    
    /**
     * @param portalPosRelativeToCamera The portal position relative to camera
     * @param vertexOutput              Output the triangles that constitute the portal view area.
     *                                  Every 3 vertices correspond to a triangle.
     *                                  In camera-centered coordinate.
     */
    @Environment(EnvType.CLIENT)
    public void renderViewAreaMesh(
        Vec3 portalPosRelativeToCamera, TriangleConsumer vertexOutput
    ) {
        if (this instanceof Mirror) {
            //rendering portal behind translucent objects with shader is broken
            boolean offsetFront = IrisInterface.invoker.isShaders()
                || IPGlobal.pureMirror;
            double mirrorOffset = offsetFront ? 0.01 : -0.01;
            portalPosRelativeToCamera = portalPosRelativeToCamera.add(
                ((Mirror) this).getNormal().scale(mirrorOffset));
        }
        
        getPortalShape().renderViewAreaMesh(
            portalPosRelativeToCamera,
            getThisSideState(),
            vertexOutput,
            getIsGlobal()
        );
    }
    
    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return createSyncPacket();
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Packet<ClientGamePacketListener> createSyncPacket() {
        Validate.isTrue(!level().isClientSide());
        
        CompoundTag compoundTag = new CompoundTag();
        addAdditionalSaveData(compoundTag);
        
        // the listener generic parameter is contravariant. this is fine
        return (Packet<ClientGamePacketListener>) (Packet)
            ServerPlayNetworking.createS2CPacket(new ImmPtlNetworking.PortalSyncPacket(
                getId(), getUUID(), getType(),
                PortalAPI.serverDimKeyToInt(getServer(), getOriginDim()),
                getX(), getY(), getZ(),
                compoundTag
            ));
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
        if (getBoundingBox().equals(NULL_BOX)) {
            LOGGER.error("Abnormal bounding box {}", this);
        }
        
        lastTickPortalState = getThisTickPortalState();
        
        if (!level().isClientSide()) {
            if (reloadAndSyncNextTick) {
                reloadAndSyncToClient();
            }
        }
        
        if (level().isClientSide()) {
            CLIENT_PORTAL_TICK_SIGNAL.invoker().accept(this);
        }
        else {
            if (!isPortalValid()) {
                LOGGER.info("Removed invalid portal {}", this);
                remove(RemovalReason.KILLED);
                return;
            }
            SERVER_PORTAL_TICK_SIGNAL.invoker().accept(this);
        }
        
        animation.tick(this);
        
        super.tick();
    }
    
    @Override
    protected @NotNull AABB makeBoundingBox() {
        if (axisW == null) {
            // it may be called when the portal is not yet initialized
            boundingBoxCache = null;
            return NULL_BOX;
        }
        if (boundingBoxCache == null) {
            boundingBoxCache = getPortalShape()
                .getBoundingBox(getThisSideState(), shouldLimitBoundingBox(), 0.2);
        }
        return boundingBoxCache;
    }
    
    protected boolean shouldLimitBoundingBox() {
        return !getIsGlobal();
    }
    
    public AABB getExactBoundingBox() {
        if (exactBoundingBoxCache == null) {
            exactBoundingBoxCache = getPortalShape().getBoundingBox(
                getThisSideState(), false, 0.001
            );
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
            getY() > (McHelper.getMinY(level()) - 100);
        if (valid) {
            if (level() instanceof ServerLevel serverLevel) {
                ServerLevel destWorld = serverLevel.getServer().getLevel(dimensionTo);
                if (destWorld == null) {
                    LOGGER.error("Portal Dest Dimension Missing {}", dimensionTo.location());
                    return false;
                }
                boolean inWorldBorder = destWorld.getWorldBorder().isWithinBounds(BlockPos.containing(getDestPos()));
                if (!inWorldBorder) {
                    LOGGER.error("Destination out of World Border {}", this);
                    return false;
                }
            }
            
            if (level().isClientSide()) {
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
            LOGGER.error("Client Portal Dest Dimension Missing {}", dimensionTo.location());
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
    public @NotNull String toString() {
        return String.format(
            "%s{%s,%s,(%s %.1f %.1f %.1f)->(%s %.1f %.1f %.1f)%s%s%s}",
            getClass().getSimpleName(),
            getId(),
            getApproximateFacingDirection(),
            level().dimension().location(), getX(), getY(), getZ(),
            dimensionTo.location(), getDestPos().x, getDestPos().y, getDestPos().z,
            specificPlayerId != null ? (",specificAccessor:" + specificPlayerId.toString()) : "",
            hasScaling() ? (",scale:" + scaling) : "",
            portalTag != null ? "," + portalTag : ""
        );
    }
    
    public Direction getApproximateFacingDirection() {
        return Direction.getNearest(
            getNormal().x, getNormal().y, getNormal().z
        );
    }
    
    /**
     * @param originalVelocityRelativeToPortal The velocity relative to portal movement. In world coordinate (not portal local coordinate).
     * @param oldEntityPos
     * @return The resulting velocity relative to portal movement (in world coordinate).
     */
    public Vec3 transformVelocityRelativeToPortal(
        Vec3 originalVelocityRelativeToPortal, Entity entity,
        Vec3 oldEntityPos
    ) {
        Vec3 result;
        if (PehkuiInterface.invoker.isPehkuiPresent()) {
            if (teleportChangesScale) {
                result = transformLocalVecNonScale(originalVelocityRelativeToPortal);
            }
            else {
                result = transformLocalVec(originalVelocityRelativeToPortal);
            }
        }
        else {
            result = transformLocalVec(originalVelocityRelativeToPortal);
        }
        
        final int maxVelocity = 15;
        if (originalVelocityRelativeToPortal.length() > maxVelocity) {
            // cannot be too fast
            result = result.normalize().scale(maxVelocity);
        }
        
        // avoid cannot push minecart out of nether portal
        if (entity instanceof AbstractMinecart && result.lengthSqr() < 0.5) {
            result = result.scale(2);
        }
        
        return result;
    }
    
    /**
     * @param pos
     * @return the distance to the portal plane without regarding the shape
     * Note: only works with flat portal
     */
    public double getDistanceToPlane(Vec3 pos) {
        return pos.subtract(getOriginPos()).dot(getNormal());
    }
    
    /**
     * @param pos
     * @return is the point in front of the portal plane without regarding the shape
     * Note: only works with flat portal
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
    private Vec3[] getFourVerticesLocalCullable(double shrink) {
        double xStart = -width / 2;
        double xEnd = width / 2;
        double yStart = -height / 2;
        double yEnd = height / 2;
        
        Vec3[] vertices = new Vec3[4];
        vertices[0] = getPointInPlaneLocal(
            xEnd - shrink,
            yStart + shrink
        );
        vertices[1] = getPointInPlaneLocal(
            xStart + shrink,
            yStart + shrink
        );
        vertices[2] = getPointInPlaneLocal(
            xEnd - shrink,
            yEnd - shrink
        );
        vertices[3] = getPointInPlaneLocal(
            xStart + shrink,
            yEnd - shrink
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
        
        return rotation.rotate(localVec);
    }
    
    public Vec3 inverseTransformLocalVecNonScale(Vec3 localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        return rotation.getConjugated().rotate(localVec);
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
        return getExactAreaBox();
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
        Vec3 from, Vec3 to
    ) {
        return lenientRayTrace(from, to, 0.001);
    }
    
    @Nullable
    public Vec3 lenientRayTrace(Vec3 from, Vec3 to, double leniency) {
        RayTraceResult rayTraceResult = getPortalShape().raytracePortalShape(
            getThisSideState(), from, to, leniency
        );
        
        if (rayTraceResult == null) {
            return null;
        }
        
        return rayTraceResult.hitPos();
    }
    
    public @Nullable RayTraceResult generalRayTrace(Vec3 from, Vec3 to, double leniency) {
        return getPortalShape().raytracePortalShape(
            getThisSideState(), from, to, leniency
        );
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(
        Vec3 point
    ) {
        return getPortalShape().roughDistanceToPortalShape(
            getThisSideState(), point
        );
    }
    
    public Vec3 getPointProjectedToPlane(Vec3 pos) {
        Vec3 originPos = getOriginPos();
        return getLocalVecProjectedToPlane(pos.subtract(originPos)).add(originPos);
    }
    
    public Vec3 getLocalVecProjectedToPlane(Vec3 offset) {
        double yInPlane = offset.dot(axisH);
        double xInPlane = offset.dot(axisW);
        
        return axisW.scale(xInPlane).add(axisH.scale(yInPlane));
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
        return getDestinationWorld(level().isClientSide());
    }
    
    private Level getDestinationWorld(boolean isClient) {
        if (isClient) {
            return CHelper.getClientWorld(dimensionTo);
        }
        else {
            MinecraftServer server = getServer();
            assert server != null;
            return server.getLevel(dimensionTo);
        }
    }
    
    public static boolean isParallelPortal(Portal a, Portal b) {
        if (a == b) {
            return false;
        }
        return a.dimensionTo == b.level().dimension() &&
            a.level().dimension() == b.dimensionTo &&
            a.getOriginPos().distanceTo(b.getDestPos()) < 0.1 &&
            a.getDestPos().distanceTo(b.getOriginPos()) < 0.1 &&
            a.getNormal().dot(b.getContentDirection()) < -0.9;
    }
    
    public static boolean isParallelOrientedPortal(Portal currPortal, Portal outerPortal) {
        double dot = currPortal.getOriginPos().subtract(outerPortal.getDestPos())
            .dot(outerPortal.getContentDirection());
        
        return currPortal.level().dimension() == outerPortal.dimensionTo &&
            currPortal.getNormal().dot(outerPortal.getContentDirection()) < -0.9 &&
            Math.abs(dot) < 0.001;
    }
    
    public static boolean isReversePortal(Portal a, Portal b) {
        return a.dimensionTo == b.level().dimension() &&
            a.level().dimension() == b.dimensionTo &&
            a.getOriginPos().distanceTo(b.getDestPos()) < 0.1 &&
            a.getDestPos().distanceTo(b.getOriginPos()) < 0.1 &&
            a.getNormal().dot(b.getContentDirection()) > 0.9;
    }
    
    public static boolean isFlippedPortal(Portal a, Portal b) {
        if (a == b) {
            return false;
        }
        return a.level() == b.level() &&
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
    
    // TODO rename to getThinBoundingBox
    @Override
    public AABB getExactAreaBox() {
        return getExactBoundingBox();
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vec3 cameraPos) {
        return getPortalShape().roughTestVisibility(getThisSideState(), cameraPos);
    }
    
    @Nullable
    @Override
    public Plane getInnerClipping() {
        return getPortalShape().getInnerClipping(
            getThisSideState(), getOtherSideState(), this
        );
    }
    
    //3  2
    //1  0
    @Nullable
    @Override
    public Vec3[] getOuterFrustumCullingVertices() {
        return getFourVerticesLocalCullable(0);
    }
    
    
    @Environment(EnvType.CLIENT)
    @Deprecated
    @Override
    public BoxPredicate getInnerFrustumCullingFunc(
        double cameraX, double cameraY, double cameraZ
    ) {
        throw new UnsupportedOperationException();
    }
    
    public Matrix4d getFullSpaceTransformation() {
        Vec3 originPos = getOriginPos();
        Vec3 destPos = getDestPos();
        DQuaternion rot = getRotationD();
        return new Matrix4d()
            .translation(destPos.x, destPos.y, destPos.z)
            .scale(getScale())
            .rotate(rot.toMcQuaternion())
            .translate(-originPos.x, -originPos.y, -originPos.z);
    }
    
    /**
     * The portal area length along axisW
     */
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public double getThickness() {
        return thickness;
    }
    
    /**
     * axisW and axisH define the orientation of the portal
     * They should be normalized and should be perpendicular to each other
     */
    public Vec3 getAxisW() {
        return axisW;
    }
    
    public void setAxisW(Vec3 axisW) {
        this.axisW = axisW;
        updateCache();
    }
    
    public Vec3 getAxisH() {
        return axisH;
    }
    
    public void setAxisH(Vec3 axisH) {
        this.axisH = axisH;
        updateCache();
    }
    
    public void setDestDim(ResourceKey<Level> dimensionTo) {
        this.dimensionTo = dimensionTo;
    }
    
    /**
     * The destination position
     */
    public Vec3 getDestination() {
        return destination;
    }
    
    /**
     * The scaling transformation of the portal
     */
    public double getScaling() {
        return scaling;
    }
    
    public void setScaling(double scaling) {
        this.scaling = scaling;
        updateCache();
    }
    
    /**
     * Whether the entity scale changes after crossing the portal
     */
    public boolean isTeleportChangesScale() {
        return teleportChangesScale;
    }
    
    public void setFuseView(boolean fuseView) {
        this.fuseView = fuseView;
    }
    
    /**
     * If false, the cross portal collision will be ignored
     */
    public boolean isCrossPortalCollisionEnabled() {
        return crossPortalCollisionEnabled;
    }
    
    public void setCrossPortalCollisionEnabled(boolean crossPortalCollisionEnabled) {
        this.crossPortalCollisionEnabled = crossPortalCollisionEnabled;
    }
    
    /**
     * Whether to render player inside this portal
     */
    public boolean isDoRenderPlayer() {
        return doRenderPlayer;
    }
    
    public void setDoRenderPlayer(boolean doRenderPlayer) {
        this.doRenderPlayer = doRenderPlayer;
    }
    
    public @Nullable List<String> getCommandsOnTeleported() {
        return commandsOnTeleported;
    }
    
    public void setCommandsOnTeleported(@Nullable List<String> commandsOnTeleported) {
        this.commandsOnTeleported = commandsOnTeleported;
    }
    
    public record TransformationDesc(
        ResourceKey<Level> dimensionTo,
        Matrix4d fullSpaceTransformation,
        DQuaternion rotation,
        double scaling
    ) {
        public static boolean isRoughlyEqual(TransformationDesc a, TransformationDesc b) {
            if (a.dimensionTo != b.dimensionTo) {
                return false;
            }
            
            Matrix4d diff = new Matrix4d().set(a.fullSpaceTransformation).sub(b.fullSpaceTransformation);
            double diffSquareSum = diff.m00() * diff.m00()
                + diff.m01() * diff.m01()
                + diff.m02() * diff.m02()
                + diff.m03() * diff.m03()
                + diff.m10() * diff.m10()
                + diff.m11() * diff.m11()
                + diff.m12() * diff.m12()
                + diff.m13() * diff.m13()
                + diff.m20() * diff.m20()
                + diff.m21() * diff.m21()
                + diff.m22() * diff.m22()
                + diff.m23() * diff.m23()
                + diff.m30() * diff.m30()
                + diff.m31() * diff.m31()
                + diff.m32() * diff.m32()
                + diff.m33() * diff.m33();
            return diffSquareSum < 0.1;
        }
    }
    
    public TransformationDesc getTransformationDesc() {
        return new TransformationDesc(
            getDestDim(),
            getFullSpaceTransformation(),
            getRotationD(),
            getScale()
        );
    }
    
    @Override
    public boolean cannotRenderInMe(Portal portal) {
        if (respectParallelOrientedPortal()) {
            return isParallelPortal(portal, this);
        }
        else {
            return isParallelOrientedPortal(portal, this);
        }
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
        // nothing
    }
    
    public boolean canDoOuterFrustumCulling() {
        if (isFuseView()) {
            return false;
        }
        if (!isVisible()) {
            return false;
        }
        return getPortalShape().canDoOuterFrustumCulling();
    }
    
    /**
     * Note: Use canTeleportEntity to test whether it can actually teleport an entity.
     * This is just a filtering condition.
     */
    public boolean isTeleportable() {
        return teleportable;
    }
    
    /**
     * If true, the parallel-oriented portal of the current portal will be rendered,
     * and checked in teleportation.
     * This is false by default, because that, if the parallel portal is rendered inside current portal,
     * it will "occlude" the other side view, and if the parallel portal is checked in teleportation,
     * the entity will teleport back by the parallel portal.
     * The same issue also exists in "connected room" case.
     * <p>
     * This only need to enable for cases like MiniScaled portal.
     * In MiniScaled, if two scale boxes are places together,
     * the player in one scale box should be able to see the other scale box and teleport to that.
     * That requires not ignoring the parallel-oriented portal.
     */
    public boolean respectParallelOrientedPortal() {
        return false;
    }
    
    // can be overridden
    // NOTE you should not add or remove or move entity here
    public void onCollidingWithEntity(Entity entity) {
    
    }
    
    @Override
    public boolean getHasCrossPortalCollision() {
        return crossPortalCollisionEnabled;
    }
    
    public boolean getTeleportChangesScale() {
        return teleportChangesScale;
    }
    
    public void setTeleportChangesScale(boolean teleportChangesScale) {
        this.teleportChangesScale = teleportChangesScale;
    }
    
    public boolean getTeleportChangesGravity() {
        return teleportChangesGravity;
    }
    
    public void setTeleportChangesGravity(boolean cond) {
        teleportChangesGravity = cond;
    }
    
    public Direction getTeleportedGravityDirection(Direction oldGravityDir) {
        if (!getTeleportChangesGravity()) {
            return oldGravityDir;
        }
        return getTransformedGravityDirection(oldGravityDir);
    }
    
    public Direction getTransformedGravityDirection(Direction oldGravityDir) {
        Vec3 oldGravityVec = Vec3.atLowerCornerOf(oldGravityDir.getNormal());
        
        Vec3 newGravityVec = transformLocalVecNonScale(oldGravityVec);
        
        return Direction.getNearest(
            newGravityVec.x, newGravityVec.y, newGravityVec.z
        );
    }
    
    // if the portal is not yet initialized, will return null
    public @NotNull PortalState getPortalState() {
        Validate.isTrue(
            axisH != null, "the portal is not yet initialized"
        );
        
        return new PortalState(
            level().dimension(),
            getOriginPos(),
            dimensionTo,
            getDestPos(),
            getScale(),
            getRotationD(),
            getOrientationRotation(),
            width, height, thickness,
            this instanceof Mirror
        );
    }
    
    public void setPortalState(PortalState state) {
        Validate.isTrue(level().dimension() == state.fromWorld);
        Validate.isTrue(dimensionTo == state.toWorld);
        
        width = state.width;
        height = state.height;
        thickness = state.thickness;
        
        setOriginPos(state.fromPos);
        setDestination(state.toPos);
        PortalManipulation.setPortalOrientationQuaternion(this, state.orientation);
        if (DQuaternion.isClose(state.rotation, DQuaternion.identity)) {
            setRotationTransformationD(null);
        }
        else {
            setRotationTransformationD(state.rotation);
        }
        
        setScaleTransformation(state.scaling);
    }
    
    public UnilateralPortalState getThisSideState() {
        if (thisSideStateCache == null) {
            thisSideStateCache = new UnilateralPortalState(
                getOriginDim(), getOriginPos(),
                getOrientationRotation(), width, height, thickness
            );
        }
        
        return thisSideStateCache;
    }
    
    public UnilateralPortalState getOtherSideState() {
        if (otherSideStateCache == null) {
            PortalState portalState = getPortalState();
            otherSideStateCache = UnilateralPortalState.extractOtherSide(portalState);
        }
        
        return otherSideStateCache;
    }
    
    public void setThisSideState(UnilateralPortalState ups) {
        setThisSideState(ups, false);
    }
    
    public void setThisSideState(UnilateralPortalState ups, boolean lockScale) {
        Validate.notNull(ups);
        PortalState portalState = getPortalState();
        
        PortalState newPortalState = portalState.withThisSideUpdated(ups, lockScale);
        setPortalState(newPortalState);
    }
    
    @Environment(EnvType.CLIENT)
    public void acceptDataSync(Vec3 pos, CompoundTag customData) {
        PortalState oldState = getPortalState();
        
        setPos(pos);
        readAdditionalSaveData(customData);
        
        if (animation.defaultAnimation.durationTicks > 0) {
            animation.defaultAnimation.startClientDefaultAnimation(this, oldState);
        }
        
        CLIENT_PORTAL_ACCEPT_SYNC_EVENT.invoker().accept(this);
    }
    
    public CompoundTag writePortalDataToNbt() {
        CompoundTag nbtCompound = new CompoundTag();
        addAdditionalSaveData(nbtCompound);
        return nbtCompound;
    }
    
    public void readPortalDataFromNbt(CompoundTag compound) {
        try {
            readAdditionalSaveData(compound);
        }
        catch (Exception e) {
            LOGGER.error("Failed to read portal data from nbt {}", compound, e);
            if (!isPortalValid()) {
                setRemoved(RemovalReason.KILLED);
            }
        }
    }
    
    public void updatePortalFromNbt(CompoundTag newNbt) {
        CompoundTag data = writePortalDataToNbt();
        
        newNbt.getAllKeys().forEach(
            key -> data.put(key, newNbt.get(key))
        );
        
        readPortalDataFromNbt(data);
    }
    
    public void rectifyClusterPortals(boolean sync) {
        PortalExtension.get(this).rectifyClusterPortals(this, sync);
    }
    
    /**
     * Use this after modifying the portal on server side.
     * This will:
     * 1. invalidate caches
     * 2. rectify cluster portals
     * (make flipped portal, reverse portal and parallel portal to update accordingly)
     * 3. send update packet(s) to client
     */
    public void reloadPortal() {
        Validate.isTrue(!level().isClientSide());
        updateCache();
        rectifyClusterPortals(true);
        reloadAndSyncToClientNextTick();
    }
    
    public DefaultPortalAnimation getDefaultAnimation() {
        return animation.defaultAnimation;
    }
    
    public void clearAnimationDrivers(boolean clearThisSide, boolean clearOtherSide) {
        animation.clearAnimationDrivers(this, clearThisSide, clearOtherSide);
    }
    
    public void addThisSideAnimationDriver(PortalAnimationDriver driver) {
        getAnimationView().addThisSideAnimation(driver);
        reloadAndSyncClusterToClientNextTick();
    }
    
    public void addOtherSideAnimationDriver(PortalAnimationDriver driver) {
        getAnimationView().addOtherSideAnimation(driver);
        reloadAndSyncClusterToClientNextTick();
    }
    
    public void pauseAnimation() {
        animation.setPaused(this, true);
    }
    
    public void resumeAnimation() {
        animation.setPaused(this, false);
    }
    
    public void resetAnimationReferenceState(boolean resetThisSide, boolean resetOtherSide) {
        this.animation.resetReferenceState(this, resetThisSide, resetOtherSide);
    }
    
    public AnimationView getAnimationView() {
        PortalExtension extension = PortalExtension.get(this);
        if (extension.flippedPortal != null) {
            if (extension.flippedPortal.animation.hasAnimationDriver()) {
                return new AnimationView(
                    this, extension.flippedPortal,
                    IntraClusterRelation.FLIPPED
                );
            }
        }
        
        if (extension.reversePortal != null) {
            if (extension.reversePortal.animation.hasAnimationDriver()) {
                return new AnimationView(
                    this, extension.reversePortal,
                    IntraClusterRelation.REVERSE
                );
            }
        }
        
        if (extension.parallelPortal != null) {
            if (extension.parallelPortal.animation.hasAnimationDriver()) {
                return new AnimationView(
                    this, extension.parallelPortal,
                    IntraClusterRelation.PARALLEL
                );
            }
        }
        
        return new AnimationView(
            this, this,
            IntraClusterRelation.SAME
        );
    }
    
    public boolean isOtherSideChunkLoaded() {
        Validate.isTrue(!level().isClientSide());
        
        return McHelper.isServerChunkFullyLoaded(
            (ServerLevel) getDestWorld(),
            new ChunkPos(BlockPos.containing(getDestPos()))
        );
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
        if (portalStateCache == null) {
            portalStateCache = getPortalState();
        }
        return portalStateCache;
    }
    
    public PortalState getAnimationEndingState() {
        return animation.getAnimationEndingState(this);
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
    
    @Nullable
    public Portal getAnimationHolder() {
        if (animation.hasAnimationDriver()) {
            return this;
        }
        
        PortalExtension portalExtension = PortalExtension.get(this);
        
        if (portalExtension.flippedPortal != null) {
            if (portalExtension.flippedPortal.animation.hasAnimationDriver()) {
                return portalExtension.flippedPortal;
            }
        }
        
        if (portalExtension.reversePortal != null) {
            if (portalExtension.reversePortal.animation.hasAnimationDriver()) {
                return portalExtension.reversePortal;
            }
        }
        
        if (portalExtension.parallelPortal != null) {
            if (portalExtension.parallelPortal.animation.hasAnimationDriver()) {
                return portalExtension.parallelPortal;
            }
        }
        
        return null;
    }
    
    public Portal getPossibleAnimationHolder() {
        Portal holder = getAnimationHolder();
        if (holder != null) {
            return holder;
        }
        return this;
    }
    
    public long getAnimationEffectiveTime() {
        Portal holder = getPossibleAnimationHolder();
        return holder.animation.getEffectiveTime(holder.level().getGameTime());
    }
    
    public void disableDefaultAnimation() {
        animation.defaultAnimation.durationTicks = 0;
        reloadAndSyncToClientNextTick();
    }
    
    public VoxelShape getThisSideCollisionExclusion() {
        if (thisSideCollisionExclusion == null) {
            thisSideCollisionExclusion = getPortalShape()
                .getThisSideCollisionExclusion(getThisSideState());
            
        }
        return thisSideCollisionExclusion;
    }
    
    // for PortalRenderable
    @Override
    public PortalLike getPortalLike() {
        return this;
    }
}
