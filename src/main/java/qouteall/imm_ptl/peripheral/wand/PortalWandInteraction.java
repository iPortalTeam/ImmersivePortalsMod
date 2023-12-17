package qouteall.imm_ptl.peripheral.wand;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalExtension;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.portal.util.PortalLocalXYNormalized;
import qouteall.imm_ptl.peripheral.CommandStickItem;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Range;

import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

public class PortalWandInteraction {
    
    private static final double SIZE_LIMIT = 64;
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class RemoteCallables {
        public static void finishPortalCreation(
            ServerPlayer player,
            ProtoPortal protoPortal
        ) {
            if (!checkPermission(player)) return;
            
            handleFinishPortalCreation(player, protoPortal);
        }
        
        public static void requestApplyDrag(
            ServerPlayer player,
            UUID portalId,
            Vec3 cursorPos,
            DraggingInfo draggingInfo
        ) {
            if (!checkPermission(player)) return;
            
            Entity entity = ((ServerLevel) player.level()).getEntity(portalId);
            
            if (!(entity instanceof Portal portal)) {
                LOGGER.error("Cannot find portal {}", portalId);
                return;
            }
            
            if (!draggingInfo.isValid()) {
                player.sendSystemMessage(Component.literal("Invalid dragging info"));
                LOGGER.error("Invalid dragging info {}", draggingInfo);
                return;
            }
            
            handleDraggingRequest(player, portalId, cursorPos, draggingInfo, portal);
        }
        
        public static void undoDrag(ServerPlayer player) {
            if (!checkPermission(player)) return;
            
            handleUndoDrag(player);
        }
        
        public static void finishDragging(ServerPlayer player) {
            if (!checkPermission(player)) return;
            
            handleFinishDrag(player);
        }
        
        public static void copyCutPortal(ServerPlayer player, UUID portalId, boolean isCut) {
            if (!checkPermission(player)) return;
            
            handleCopyCutPortal(player, portalId, isCut);
        }
        
        public static void confirmCopyCut(ServerPlayer player, Vec3 origin, DQuaternion orientation) {
            if (!checkPermission(player)) return;
            
            handleConfirmCopyCut(player, origin, orientation);
        }
        
        public static void clearPortalClipboard(ServerPlayer player) {
            if (!checkPermission(player)) return;
            
            handleClearPortalClipboard(player);
        }
    }
    
    private static void handleFinishPortalCreation(ServerPlayer player, ProtoPortal protoPortal) {
        Validate.isTrue(protoPortal.firstSide != null);
        Validate.isTrue(protoPortal.secondSide != null);
        
        Vec3 firstSideLeftBottom = protoPortal.firstSide.leftBottom;
        Vec3 firstSideRightBottom = protoPortal.firstSide.rightBottom;
        Vec3 firstSideLeftUp = protoPortal.firstSide.leftTop;
        Vec3 secondSideLeftBottom = protoPortal.secondSide.leftBottom;
        Vec3 secondSideRightBottom = protoPortal.secondSide.rightBottom;
        Vec3 secondSideLeftUp = protoPortal.secondSide.leftTop;
        
        Validate.notNull(firstSideLeftBottom);
        Validate.notNull(firstSideRightBottom);
        Validate.notNull(firstSideLeftUp);
        Validate.notNull(secondSideLeftBottom);
        Validate.notNull(secondSideRightBottom);
        Validate.notNull(secondSideLeftUp);
        
        ResourceKey<Level> firstSideDimension = protoPortal.firstSide.dimension;
        ResourceKey<Level> secondSideDimension = protoPortal.secondSide.dimension;
        
        Vec3 firstSideHorizontalAxis = firstSideRightBottom.subtract(firstSideLeftBottom);
        Vec3 firstSideVerticalAxis = firstSideLeftUp.subtract(firstSideLeftBottom);
        double firstSideWidth = firstSideHorizontalAxis.length();
        double firstSideHeight = firstSideVerticalAxis.length();
        Vec3 firstSideHorizontalUnitAxis = firstSideHorizontalAxis.normalize();
        Vec3 firstSideVerticalUnitAxis = firstSideVerticalAxis.normalize();
        
        if (Math.abs(firstSideWidth) < 0.001 || Math.abs(firstSideHeight) < 0.001) {
            player.sendSystemMessage(Component.literal("The first side is too small"));
            LOGGER.error("The first side is too small");
            return;
        }
        
        if (firstSideHorizontalUnitAxis.dot(firstSideVerticalUnitAxis) > 0.001) {
            player.sendSystemMessage(Component.literal("The horizontal and vertical axis are not perpendicular in first side"));
            LOGGER.error("The horizontal and vertical axis are not perpendicular in first side");
            return;
        }
        
        if (firstSideWidth > SIZE_LIMIT || firstSideHeight > SIZE_LIMIT) {
            player.sendSystemMessage(Component.literal("The first side is too large"));
            LOGGER.error("The first side is too large");
            return;
        }
        
        Vec3 secondSideHorizontalAxis = secondSideRightBottom.subtract(secondSideLeftBottom);
        Vec3 secondSideVerticalAxis = secondSideLeftUp.subtract(secondSideLeftBottom);
        double secondSideWidth = secondSideHorizontalAxis.length();
        double secondSideHeight = secondSideVerticalAxis.length();
        Vec3 secondSideHorizontalUnitAxis = secondSideHorizontalAxis.normalize();
        Vec3 secondSideVerticalUnitAxis = secondSideVerticalAxis.normalize();
        
        if (Math.abs(secondSideWidth) < 0.001 || Math.abs(secondSideHeight) < 0.001) {
            player.sendSystemMessage(Component.literal("The second side is too small"));
            LOGGER.error("The second side is too small");
            return;
        }
        
        if (secondSideHorizontalUnitAxis.dot(secondSideVerticalUnitAxis) > 0.001) {
            player.sendSystemMessage(Component.literal("The horizontal and vertical axis are not perpendicular in second side"));
            LOGGER.error("The horizontal and vertical axis are not perpendicular in second side");
            return;
        }
        
        if (secondSideWidth > SIZE_LIMIT || secondSideHeight > SIZE_LIMIT) {
            player.sendSystemMessage(Component.literal("The second side is too large"));
            LOGGER.error("The second side is too large");
            return;
        }
        
        if (Math.abs((firstSideHeight / firstSideWidth) - (secondSideHeight / secondSideWidth)) > 0.001) {
            player.sendSystemMessage(Component.literal("The two sides have different aspect ratio"));
            LOGGER.error("The two sides have different aspect ratio");
            return;
        }
        
        boolean overlaps = false;
        if (firstSideDimension == secondSideDimension) {
            Vec3 firstSideNormal = firstSideHorizontalUnitAxis.cross(firstSideVerticalUnitAxis);
            Vec3 secondSideNormal = secondSideHorizontalUnitAxis.cross(secondSideVerticalUnitAxis);
            
            // check orientation overlap
            if (Math.abs(firstSideNormal.dot(secondSideNormal)) > 0.99) {
                // check plane overlap
                if (Math.abs(firstSideLeftBottom.subtract(secondSideLeftBottom).dot(firstSideNormal)) < 0.001) {
                    // check portal area overlap
                    
                    Vec3 coordCenter = firstSideLeftBottom;
                    Vec3 coordX = firstSideHorizontalAxis;
                    Vec3 coordY = firstSideVerticalAxis;
                    
                    Range firstSideXRange = Range.createUnordered(
                        firstSideLeftBottom.subtract(coordCenter).dot(coordX),
                        firstSideRightBottom.subtract(coordCenter).dot(coordX)
                    );
                    Range firstSideYRange = Range.createUnordered(
                        firstSideLeftBottom.subtract(coordCenter).dot(coordY),
                        firstSideLeftUp.subtract(coordCenter).dot(coordY)
                    );
                    
                    Range secondSideXRange = Range.createUnordered(
                        secondSideLeftBottom.subtract(coordCenter).dot(coordX),
                        secondSideRightBottom.subtract(coordCenter).dot(coordX)
                    );
                    Range secondSideYRange = Range.createUnordered(
                        secondSideLeftBottom.subtract(coordCenter).dot(coordY),
                        secondSideLeftUp.subtract(coordCenter).dot(coordY)
                    );
                    
                    if (firstSideXRange.intersect(secondSideXRange) != null &&
                        firstSideYRange.intersect(secondSideYRange) != null
                    ) {
                        overlaps = true;
                    }
                }
            }
        }
        
        Portal portal = Portal.entityType.create(McHelper.getServerWorld(firstSideDimension));
        Validate.notNull(portal);
        portal.setOriginPos(
            firstSideLeftBottom
                .add(firstSideHorizontalAxis.scale(0.5))
                .add(firstSideVerticalAxis.scale(0.5))
        );
        portal.width = firstSideWidth;
        portal.height = firstSideHeight;
        portal.axisW = firstSideHorizontalUnitAxis;
        portal.axisH = firstSideVerticalUnitAxis;
        
        portal.dimensionTo = secondSideDimension;
        portal.setDestination(
            secondSideLeftBottom
                .add(secondSideHorizontalAxis.scale(0.5))
                .add(secondSideVerticalAxis.scale(0.5))
        );
        
        portal.scaling = secondSideWidth / firstSideWidth;
        
        DQuaternion secondSideOrientation = DQuaternion.matrixToQuaternion(
            secondSideHorizontalUnitAxis,
            secondSideVerticalUnitAxis,
            secondSideHorizontalUnitAxis.cross(secondSideVerticalUnitAxis)
        );
        portal.setOtherSideOrientation(secondSideOrientation);
        
        Portal flippedPortal = PortalManipulation.createFlippedPortal(portal, Portal.entityType);
        Portal reversePortal = PortalManipulation.createReversePortal(portal, Portal.entityType);
        Portal parallelPortal = PortalManipulation.createFlippedPortal(reversePortal, Portal.entityType);
        
        McHelper.spawnServerEntity(portal);
        
        if (overlaps) {
            player.sendSystemMessage(Component.translatable("imm_ptl.wand.overlap"));
        }
        else {
            McHelper.spawnServerEntity(flippedPortal);
            McHelper.spawnServerEntity(reversePortal);
            McHelper.spawnServerEntity(parallelPortal);
        }
        
        player.sendSystemMessage(Component.translatable("imm_ptl.wand.finished"));
        
        giveCommandStick(player, "/portal eradicate_portal_cluster");
    }
    
    private static class DraggingSession {
        private final ResourceKey<Level> dimension;
        private final UUID portalId;
        private final PortalState originalState;
        private final DraggingInfo lastDraggingInfo;
        
        public DraggingSession(
            ResourceKey<Level> dimension, UUID portalId,
            PortalState originalState, DraggingInfo lastDraggingInfo
        ) {
            this.dimension = dimension;
            this.portalId = portalId;
            this.originalState = originalState;
            this.lastDraggingInfo = lastDraggingInfo;
        }
        
        @Nullable
        public Portal getPortal() {
            Entity entity = McHelper.getServerWorld(dimension).getEntity(portalId);
            
            if (entity instanceof Portal) {
                return (Portal) entity;
            }
            else {
                return null;
            }
        }
    }
    
    // TODO make per-server
    private static final WeakHashMap<ServerPlayer, DraggingSession> draggingSessionMap = new WeakHashMap<>();
    
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            draggingSessionMap.entrySet().removeIf(
                e -> {
                    ServerPlayer player = e.getKey();
                    if (player.isRemoved()) {
                        return true;
                    }
                    
                    if (player.getMainHandItem().getItem() != PortalWandItem.instance) {
                        return true;
                    }
                    
                    return false;
                }
            );
            
            copyingSessionMap.entrySet().removeIf(
                e -> {
                    ServerPlayer player = e.getKey();
                    return player.isRemoved();
                }
            );
        });
        
        IPGlobal.SERVER_CLEANUP_EVENT.register(s -> draggingSessionMap.clear());
        IPGlobal.SERVER_CLEANUP_EVENT.register(s -> copyingSessionMap.clear());
    }
    
    public static final class DraggingInfo {
        public final @Nullable PortalLocalXYNormalized lockedAnchor;
        public final PortalLocalXYNormalized draggingAnchor;
        public @Nullable Vec3 previousRotationAxis;
        public final boolean lockWidth;
        public final boolean lockHeight;
        
        public DraggingInfo(
            @Nullable PortalLocalXYNormalized lockedAnchor,
            PortalLocalXYNormalized draggingAnchor,
            @Nullable Vec3 previousRotationAxis,
            boolean lockWidth,
            boolean lockHeight
        ) {
            this.lockedAnchor = lockedAnchor;
            this.draggingAnchor = draggingAnchor;
            this.previousRotationAxis = previousRotationAxis;
            this.lockWidth = lockWidth;
            this.lockHeight = lockHeight;
        }
        
        public boolean shouldLockScale() {
            return lockWidth || lockHeight;
        }
        
        public boolean isValid() {
            if (lockedAnchor != null) {
                if (!lockedAnchor.isValid()) {
                    return false;
                }
            }
            if (!draggingAnchor.isValid()) {
                return false;
            }
            return true;
        }
    }
    
    @Nullable
    public static UnilateralPortalState applyDrag(
        UnilateralPortalState originalState, Vec3 cursorPos, DraggingInfo info,
        boolean updateInternalState
    ) {
        if (info.lockedAnchor == null) {
            Vec3 offset = info.draggingAnchor.getOffset(originalState);
            Vec3 newPos = cursorPos.subtract(offset);
            
            return new UnilateralPortalState.Builder()
                .from(originalState)
                .position(newPos)
                .build();
        }
        
        OneLockDraggingResult r = performDragWithOneLockedAnchor(
            originalState, info.lockedAnchor, info.draggingAnchor, cursorPos, info.previousRotationAxis,
            info.lockWidth, info.lockHeight
        );
        
        if (r == null) {
            return null;
        }
        
        if (updateInternalState) {
            info.previousRotationAxis = r.rotationAxis();
        }
        return r.newState();
    }
    
    private static void handleFinishDrag(ServerPlayer player) {
        DraggingSession session = draggingSessionMap.remove(player);
        
        if (session == null) {
            return;
        }
        
        Portal portal = session.getPortal();
        
        if (portal != null) {
            portal.reloadAndSyncToClientNextTick();
        }
    }
    
    private static void handleUndoDrag(ServerPlayer player) {
        DraggingSession session = draggingSessionMap.get(player);
        
        if (session == null) {
//            player.sendSystemMessage(Component.literal("Cannot undo"));
            return;
        }
        
        Portal portal = session.getPortal();
        
        if (portal == null) {
            LOGGER.error("Cannot find portal {}", session.portalId);
            return;
        }
        
        portal.setPortalState(session.originalState);
        portal.reloadAndSyncToClientNextTick();
        portal.rectifyClusterPortals(true);
        
        draggingSessionMap.remove(player);
    }
    
    private static void handleDraggingRequest(
        ServerPlayer player, UUID portalId, Vec3 cursorPos, DraggingInfo draggingInfo, Portal portal
    ) {
        DraggingSession session = draggingSessionMap.get(player);
        
        if (session != null && session.portalId.equals(portalId)) {
            // reuse session
        }
        else {
            session = new DraggingSession(
                player.level().dimension(),
                portalId,
                portal.getPortalState(),
                draggingInfo
            );
            draggingSessionMap.put(player, session);
        }
        
        UnilateralPortalState newThisSideState = applyDrag(
            session.originalState.getThisSideState(), cursorPos, draggingInfo,
            true
        );
        if (validateDraggedPortalState(session.originalState, newThisSideState, player)) {
            portal.setThisSideState(newThisSideState, draggingInfo.shouldLockScale());
            portal.reloadAndSyncToClientNextTick();
            portal.rectifyClusterPortals(true);
        }
        else {
            player.sendSystemMessage(Component.literal("Invalid dragging"));
        }
    }
    
    private static boolean checkPermission(ServerPlayer player) {
        if (!canPlayerUsePortalWand(player)) {
            player.sendSystemMessage(Component.literal("You cannot use portal wand"));
            LOGGER.error("Player cannot use portal wand {}", player);
            return false;
        }
        return true;
    }
    
    public static boolean validateDraggedPortalState(
        PortalState originalState, UnilateralPortalState newThisSideState,
        Player player
    ) {
        if (newThisSideState == null) {
            return false;
        }
        if (newThisSideState.width() > 64.1) {
            return false;
        }
        if (newThisSideState.height() > 64.1) {
            return false;
        }
        if (newThisSideState.width() < 0.05) {
            return false;
        }
        if (newThisSideState.height() < 0.05) {
            return false;
        }
        
        if (originalState.fromWorld != newThisSideState.dimension()) {
            return false;
        }
        
        if (newThisSideState.position().distanceTo(player.position()) > 64) {
            return false;
        }
        
        return true;
    }
    
    private static boolean canPlayerUsePortalWand(ServerPlayer player) {
        return player.hasPermissions(2) || (IPGlobal.easeCreativePermission && player.isCreative());
    }
    
    private static void giveCommandStick(ServerPlayer player, String command) {
        CommandStickItem.Data data = CommandStickItem.BUILT_IN_COMMAND_STICK_TYPES.get(command);
        
        if (data == null) {
            data = new CommandStickItem.Data(command, command, List.of());
        }
        
        ItemStack stack = new ItemStack(CommandStickItem.instance);
        stack.setTag(data.toTag());
        
        if (!player.getInventory().contains(stack)) {
            player.getInventory().add(stack);
        }
    }
    
    public static boolean isDragging(ServerPlayer player) {
        return draggingSessionMap.containsKey(player);
    }
    
    @Nullable
    public static PortalWandInteraction.OneLockDraggingResult performDragWithOneLockedAnchor(
        UnilateralPortalState originalState,
        PortalLocalXYNormalized lockedLocalPos,
        PortalLocalXYNormalized draggingLocalPos, Vec3 draggedPos,
        @Nullable Vec3 previousRotationAxis,
        boolean lockWidth, boolean lockHeight
    ) {
        Vec3 draggedPosOriginalPos = draggingLocalPos.getPos(originalState);
        Vec3 lockedPos = lockedLocalPos.getPos(originalState);
        Vec3 originalOffset = draggedPosOriginalPos.subtract(lockedPos);
        Vec3 newOffset = draggedPos.subtract(lockedPos);
        
        double newOffsetLen = newOffset.length();
        double originalOffsetLen = originalOffset.length();
        
        if (newOffsetLen < 0.00001 || originalOffsetLen < 0.00001) {
            return null;
        }
        
        Vec3 originalOffsetN = originalOffset.normalize();
        Vec3 newOffsetN = newOffset.normalize();
        
        double dot = originalOffsetN.dot(newOffsetN);
        
        DQuaternion rotation;
        if (Math.abs(dot) < 0.99999) {
            rotation = DQuaternion.getRotationBetween(originalOffset, newOffset)
                .fixFloatingPointErrorAccumulation();
        }
        else {
            // the originalOffset and newOffset are colinear
            
            if (dot > 0) {
                // the two offsets are roughly equal. no dragging
                rotation = DQuaternion.identity;
            }
            else {
                // the two offsets are opposite.
                // we cannot determine the rotation axis. the possible axises are on a plane
                
                Plane planeOfPossibleAxis = new Plane(Vec3.ZERO, originalOffsetN);
                
                // to improve user-friendliness, use the axis from the previous rotation
                if (previousRotationAxis != null) {
                    // project the previous axis onto the plane of possible axis
                    Vec3 projected = planeOfPossibleAxis.getProjection(previousRotationAxis);
                    if (projected.lengthSqr() < 0.00001) {
                        // the previous axis is perpendicular to the plane
                        // cannot determine axis
                        return null;
                    }
                    Vec3 axis = projected.normalize();
                    rotation = DQuaternion.rotationByDegrees(axis, 180)
                        .fixFloatingPointErrorAccumulation();
                }
                else {
                    rotation = DQuaternion.identity;
                }
            }
        }
        
        DQuaternion newOrientation = rotation.hamiltonProduct(originalState.orientation())
            .fixFloatingPointErrorAccumulation();
        
        PortalLocalXYNormalized deltaLocalXY = draggingLocalPos.subtract(lockedLocalPos);
        
        double newWidth;
        double newHeight;
        if (lockWidth && lockHeight) {
            newWidth = originalState.width();
            newHeight = originalState.height();
        }
        else {
            Vec3 newNormal = rotation.rotate(originalState.getNormal());
            if (lockWidth) {
                assert !lockHeight;
                newWidth = originalState.width();
                if (Math.abs(deltaLocalXY.ny()) < 0.001) {
                    newHeight = originalState.height();
                }
                else {
                    double subWidth = Math.abs(deltaLocalXY.nx()) * newWidth;
                    double diff = newOffsetLen * newOffsetLen - subWidth * subWidth;
                    if (diff < 0.000001) {
                        return null;
                    }
                    double subHeight = Math.sqrt(diff);
                    newHeight = subHeight / Math.abs(deltaLocalXY.ny());
                    if (Math.abs(subWidth) > 0.001) {
                        // if dragging on non-axis-aligned direction and aspect ratio changes,
                        // the simple rotation will not match the dragging cursor.
                        // recalculate the orientation
                        newOrientation = getOrientationByNormalDiagonalWidthHeight(
                            newNormal, newOffset, subWidth, subHeight,
                            Math.signum(deltaLocalXY.nx()), Math.signum(deltaLocalXY.ny())
                        );
                    }
                }
            }
            else if (lockHeight) {
                assert !lockWidth;
                newHeight = originalState.height();
                if (Math.abs(deltaLocalXY.nx()) < 0.001) {
                    newWidth = originalState.width();
                }
                else {
                    double subHeight = Math.abs(deltaLocalXY.ny()) * newHeight;
                    double diff = newOffsetLen * newOffsetLen - subHeight * subHeight;
                    if (diff < 0.000001) {
                        return null;
                    }
                    double subWidth = Math.sqrt(diff);
                    newWidth = subWidth / Math.abs(deltaLocalXY.nx());
                    if (Math.abs(subHeight) > 0.001) {
                        // if dragging on non-axis-aligned direction and aspect ratio changes,
                        // the simple rotation will not match the dragging cursor.
                        // recalculate the orientation
                        newOrientation = getOrientationByNormalDiagonalWidthHeight(
                            newNormal, newOffset, subWidth, subHeight,
                            Math.signum(deltaLocalXY.nx()), Math.signum(deltaLocalXY.ny())
                        );
                    }
                }
            }
            else {
                double scaling = newOffsetLen / originalOffsetLen;
                newWidth = originalState.width() * scaling;
                newHeight = originalState.height() * scaling;
            }
        }
        
        // get the new unilateral portal state by scaling and rotation
        Vec3 newLockedPosOffset = newOrientation.rotate(new Vec3(
            (lockedLocalPos.nx() - 0.5) * newWidth,
            (lockedLocalPos.ny() - 0.5) * newHeight,
            0
        ));
        Vec3 newOrigin = lockedPos.subtract(newLockedPosOffset);
        
        UnilateralPortalState newPortalState = new UnilateralPortalState(
            originalState.dimension(), newOrigin, newOrientation, newWidth, newHeight
        );
        return new OneLockDraggingResult(
            newPortalState, rotation.getRotatingAxis()
        );
    }
    
    /**
     * The sub-portal defined by the locked pos and the cursor pos
     * has the same orientation as the outer portal.
     * Get the sub-portal's orientation by normal, diagonal, width and height.
     */
    private static DQuaternion getOrientationByNormalDiagonalWidthHeight(
        Vec3 normal, Vec3 diagonal,
        double width, double height,
        double sigX, double sigY
    ) {
        Vec3 newOffsetN = diagonal.normalize();
        double newOffsetLen = diagonal.length();
        
        // the side vec is on the left side of the diagonal (right-hand rule)
        Vec3 sideVecN = normal.cross(newOffsetN).normalize();
        
        // the triangle area can be represented by two ways:
        // diagonalLen * sideVecLen / 2, or width * height / 2
        double sideVecLen = (width * height) / newOffsetLen;
        
        // the tangent can be represented by two ways:
        // width / height, or sideVecLen / wFront
        double wFront = sideVecLen * width / height;
        double hFront = sideVecLen * height / width;
        
        // if sigX and sigY are both 1,
        // then the axisW is on the right side of the diagonal (right-hand rule),
        // and axisH is on the left side of the diagonal.
        // if either sigX or sigY flips, this flips, so multiply the signum.
        Vec3 sideVecW = sideVecN.scale(-sideVecLen * sigX * sigY);
        Vec3 sideVecH = sideVecW.scale(-1);
        
        Vec3 newAxisW = newOffsetN.scale(wFront)
            .add(sideVecW).normalize()
            .scale(sigX); // make it the same direction of the outer portal
        
        Vec3 newAxisH = newOffsetN.scale(hFront)
            .add(sideVecH).normalize()
            .scale(sigY);
        
        DQuaternion newOrientation = DQuaternion.fromFacingVecs(newAxisW, newAxisH)
            .fixFloatingPointErrorAccumulation();
        return newOrientation;
    }
    
    public static record OneLockDraggingResult(
        UnilateralPortalState newState,
        Vec3 rotationAxis
    ) {
    }
    
    private record CopyingSession(
        PortalState portalState, CompoundTag portalData, boolean isCut,
        boolean hasFlipped, boolean hasReverse, boolean hasParallel
        // TODO store animation view
    ) {
    }
    
    private static final WeakHashMap<ServerPlayer, CopyingSession> copyingSessionMap = new WeakHashMap<>();
    
    public static void handleCopyCutPortal(ServerPlayer player, UUID portalId, boolean isCut) {
        Portal portal = WandUtil.getPortalByUUID(player.level(), portalId);
        
        if (portal == null) {
            player.sendSystemMessage(Component.literal("Cannot find portal " + portalId));
            return;
        }
        
        PortalState portalState = portal.getPortalState();
        CompoundTag portalData = portal.writePortalDataToNbt();
        
        Portal flipped = null;
        Portal reverse = null;
        Portal parallel = null;
        
        PortalExtension ext = PortalExtension.get(portal);
        if (ext.flippedPortal != null) {
            flipped = ext.flippedPortal;
        }
        if (ext.reversePortal != null) {
            reverse = ext.reversePortal;
        }
        if (ext.parallelPortal != null) {
            parallel = ext.parallelPortal;
        }
        
        CopyingSession copyingSession = new CopyingSession(
            portalState, portalData, isCut,
            flipped != null, reverse != null, parallel != null
        );
        copyingSessionMap.put(player, copyingSession);
        
        if (isCut) {
            portal.remove(Entity.RemovalReason.KILLED);
            if (flipped != null) {
                flipped.remove(Entity.RemovalReason.KILLED);
            }
            if (reverse != null) {
                reverse.remove(Entity.RemovalReason.KILLED);
            }
            if (parallel != null) {
                parallel.remove(Entity.RemovalReason.KILLED);
            }
        }
    }
    
    private static void handleConfirmCopyCut(ServerPlayer player, Vec3 origin, DQuaternion rawOrientation) {
        CopyingSession copyingSession = copyingSessionMap.remove(player);
        
        if (copyingSession == null) {
            player.sendSystemMessage(Component.literal("Missing copying session"));
            return;
        }
        
        DQuaternion orientation = rawOrientation.fixFloatingPointErrorAccumulation();
        
        if (player.position().distanceToSqr(origin) > 64 * 64) {
            player.sendSystemMessage(Component.literal("Too far away from the portal"));
            return;
        }
        
        Portal portal = Portal.entityType.create(player.level());
        assert portal != null;
        
        portal.readPortalDataFromNbt(copyingSession.portalData);
        
        PortalState originalPortalState = copyingSession.portalState;
        
        UnilateralPortalState originalThisSide = originalPortalState.getThisSideState();
        UnilateralPortalState originalOtherSide = originalPortalState.getOtherSideState();
        
        UnilateralPortalState newThisSide = new UnilateralPortalState(
            player.level().dimension(),
            origin,
            orientation,
            originalThisSide.width(), originalThisSide.height()
        );
        
        portal.setPortalState(UnilateralPortalState.combine(
            newThisSide, originalOtherSide
        ));
        portal.resetAnimationReferenceState(true, false);
        
        if (copyingSession.isCut()) {
            McHelper.spawnServerEntity(portal);
            
            if (copyingSession.hasFlipped) {
                Portal flippedPortal = PortalManipulation.createFlippedPortal(portal, Portal.entityType);
                flippedPortal.resetAnimationReferenceState(true, false);
                McHelper.spawnServerEntity(flippedPortal);
            }
            
            if (copyingSession.hasReverse) {
                Portal reversePortal = PortalManipulation.createReversePortal(portal, Portal.entityType);
                reversePortal.resetAnimationReferenceState(false, true);
                McHelper.spawnServerEntity(reversePortal);
                
                if (copyingSession.hasParallel) {
                    Portal parallelPortal = PortalManipulation.createFlippedPortal(reversePortal, Portal.entityType);
                    parallelPortal.resetAnimationReferenceState(false, true);
                    McHelper.spawnServerEntity(parallelPortal);
                }
            }
        }
        else {
            PortalExtension.get(portal).bindCluster = false;
            McHelper.spawnServerEntity(portal);
            
            if (copyingSession.hasFlipped || copyingSession.hasReverse || copyingSession.hasParallel) {
                player.sendSystemMessage(Component.translatable("imm_ptl.wand.copy.not_copying_cluster"));
                giveCommandStick(player, "/portal complete_bi_way_bi_faced_portal");
            }
        }
    }
    
    private static void handleClearPortalClipboard(ServerPlayer player) {
        copyingSessionMap.remove(player);
    }
    
    
}
