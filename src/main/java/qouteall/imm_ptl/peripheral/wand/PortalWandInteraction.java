package qouteall.imm_ptl.peripheral.wand;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.peripheral.CommandStickItem;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Range;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class PortalWandInteraction {
    
    private static final double SIZE_LIMIT = 64;
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static class DraggingSession {
        private final ResourceKey<Level> dimension;
        private final UUID portalId;
        private final PortalState originalState;
        private final DraggingInfo lastDraggingInfo;
        @NotNull
        private Vec3 lastCursorPos;
        
        public DraggingSession(
            ResourceKey<Level> dimension, UUID portalId,
            PortalState originalState, DraggingInfo lastDraggingInfo,
            @NotNull Vec3 lastCursorPos
        ) {
            this.dimension = dimension;
            this.portalId = portalId;
            this.originalState = originalState;
            this.lastDraggingInfo = lastDraggingInfo;
            this.lastCursorPos = lastCursorPos;
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
    
    private static final WeakHashMap<ServerPlayer, DraggingSession> draggingSessionMap = new WeakHashMap<>();
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(() -> {
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
        });
        
        IPGlobal.serverCleanupSignal.connect(draggingSessionMap::clear);
    }
    
    public static record DraggingInfo(
        Set<PortalCorner> lockedCorners,
        PortalCorner selectedCorner
    ) {
    }
    
    @Nullable
    public static UnilateralPortalState applyDrag(
        UnilateralPortalState originalThisSideState, Vec3 cursorPos, DraggingInfo info
    ) {
        PortalCorner selectedCorner = info.selectedCorner;
        if (info.lockedCorners.contains(selectedCorner)) {
            LOGGER.error("Cannot drag locked corner");
            return null;
        }
        
        List<PortalCorner> lockedCorners = info.lockedCorners().stream().toList();
        
        int lockedCornerNum = lockedCorners.size();
        
        if (lockedCornerNum == 0) {
            return PortalCorner.performDragWithNoLockedCorner(
                originalThisSideState, selectedCorner, cursorPos
            );
        }
        else if (lockedCornerNum == 1) {
            return PortalCorner.performDragWith1LockedCorner(
                originalThisSideState,
                lockedCorners.get(0), lockedCorners.get(0).getPos(originalThisSideState),
                selectedCorner, cursorPos
            );
        }
        else if (lockedCornerNum == 2) {
            return PortalCorner.performDragWith2LockedCorners(
                originalThisSideState,
                lockedCorners.get(0), lockedCorners.get(0).getPos(originalThisSideState),
                lockedCorners.get(1), lockedCorners.get(1).getPos(originalThisSideState),
                selectedCorner, cursorPos
            );
        }
        else {
            LOGGER.error("Locked too many corners");
            return null;
        }
    }
    
    public static class RemoteCallables {
        public static void finish(
            ServerPlayer player,
            ProtoPortal protoPortal
        ) {
            if (!checkPermission(player)) return;
            
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
            
            giveDeletingPortalCommandStickIfNotPresent(player);
        }
        
        public static void requestApplyDrag(
            ServerPlayer player,
            UUID portalId,
            Vec3 cursorPos,
            DraggingInfo draggingInfo
        ) {
            if (!checkPermission(player)) return;
            
            Entity entity = ((ServerLevel) player.level).getEntity(portalId);
            
            if (!(entity instanceof Portal portal)) {
                LOGGER.error("Cannot find portal {}", portalId);
                return;
            }
            
            DraggingSession session = draggingSessionMap.get(player);
            
            if (session != null && session.portalId.equals(portalId)) {
                // reuse session
            }
            else {
                session = new DraggingSession(
                    player.level.dimension(),
                    portalId,
                    portal.getPortalState(),
                    draggingInfo,
                    draggingInfo.selectedCorner.getPos(portal)
                );
                draggingSessionMap.put(player, session);
//                LOGGER.info("Portal dragging session created");
            }
            
            UnilateralPortalState newThisSideState = applyDrag(
                session.originalState.getThisSideState(), cursorPos, draggingInfo
            );
            if (validateDraggedPortalState(session.originalState, newThisSideState, player)) {
                portal.setThisSideState(newThisSideState);
                portal.reloadAndSyncToClient();
                portal.rectifyClusterPortals(true);
                
                session.lastCursorPos = cursorPos;
            }
            else {
                player.sendSystemMessage(Component.literal("Invalid dragging"));
            }
        }
        
        public static void undoDrag(ServerPlayer player) {
            DraggingSession session = draggingSessionMap.get(player);
            
            if (session == null) {
                player.sendSystemMessage(Component.literal("Cannot undo"));
                return;
            }
            
            Portal portal = session.getPortal();
            
            if (portal == null) {
                LOGGER.error("Cannot find portal {}", session.portalId);
                return;
            }
            
            portal.setPortalState(session.originalState);
            portal.reloadAndSyncToClient();
            portal.rectifyClusterPortals(true);
            
            draggingSessionMap.remove(player);
        }
        
        public static void finishDragging(ServerPlayer player) {
            DraggingSession session = draggingSessionMap.remove(player);
            Portal portal = session.getPortal();
            
            if (portal != null) {
                portal.reloadAndSyncToClient();
            }
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
        
        if (originalState.fromPos.distanceTo(newThisSideState.position()) > 128) {
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
    
    private static void giveDeletingPortalCommandStickIfNotPresent(ServerPlayer player) {
        CommandStickItem.Data stickData = CommandStickItem.commandStickTypeRegistry.get(
            new ResourceLocation("imm_ptl:eradicate_portal_cluster")
        );
        
        if (stickData == null) {
            return;
        }
        
        ItemStack stack = new ItemStack(CommandStickItem.instance);
        stack.setTag(stickData.toTag());
        
        if (!player.getInventory().contains(stack)) {
            player.getInventory().add(stack);
        }
    }
    
    public static boolean isDragging(ServerPlayer player) {
        return draggingSessionMap.containsKey(player);
    }
}
