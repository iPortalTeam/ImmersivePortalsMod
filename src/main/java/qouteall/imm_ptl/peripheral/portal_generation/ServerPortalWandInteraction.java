package qouteall.imm_ptl.peripheral.portal_generation;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalManipulation;
import qouteall.imm_ptl.peripheral.CommandStickItem;
import qouteall.q_misc_util.my_util.DQuaternion;

public class ServerPortalWandInteraction {
    
    private static final double SIZE_LIMIT = 64;
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static class RemoteCallables {
        public static void finish(
            ServerPlayer player,
            ResourceKey<Level> firstSideDimension,
            Vec3 firstSideLeftBottom,
            Vec3 firstRightRightBottom,
            Vec3 firstSideLeftUp,
            ResourceKey<Level> secondSideDimension,
            Vec3 secondSideLeftBottom,
            Vec3 secondSideRightBottom,
            Vec3 secondSideLeftUp
        ) {
            if (!canPlayerUsePortalWand(player)) {
                player.sendSystemMessage(Component.literal("You cannot use portal wand"));
                LOGGER.error("Player cannot use portal wand {}", player);
                return;
            }
            
            Vec3 firstSideHorizontalAxis = firstRightRightBottom.subtract(firstSideLeftBottom);
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
            McHelper.spawnServerEntity(flippedPortal);
            McHelper.spawnServerEntity(reversePortal);
            McHelper.spawnServerEntity(parallelPortal);
            
            player.sendSystemMessage(Component.translatable("imm_ptl.wand.finished"));
            
            giveDeletingPortalCommandStickIfNotPresent(player);
        }
    }
    
    private static boolean canPlayerUsePortalWand(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
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
}
