package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.List;

public class BlockManipulationServer {
    
    public static void processBreakBlock(
        DimensionType dimension,
        PlayerActionC2SPacket packet,
        ServerPlayerEntity player
    ) {
        if (shouldFinishMining(dimension, packet, player)) {
            if (canPlayerReach(dimension, player, packet.getPos())) {
                doDestroyBlock(dimension, packet, player);
            }
            else {
                Helper.log("Rejected cross portal block breaking packet " + player);
            }
        }
    }
    
    private static boolean shouldFinishMining(
        DimensionType dimension,
        PlayerActionC2SPacket packet,
        ServerPlayerEntity player
    
    ) {
        if (packet.getAction() == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            return canInstantMine(
                McHelper.getServer().getWorld(dimension),
                player,
                packet.getPos()
            );
        }
        else {
            return packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK;
        }
    }
    
    private static boolean canPlayerReach(
        DimensionType dimension,
        ServerPlayerEntity player,
        BlockPos requestPos
    ) {
        Vec3d pos = new Vec3d(requestPos);
        Vec3d playerPos = player.getPos();
        double multiplier = HandReachTweak.getActualHandReachMultiplier(player);
        double distanceSquare = 6 * 6 * multiplier * multiplier;
        if (player.dimension == dimension) {
            if (playerPos.squaredDistanceTo(pos) < distanceSquare) {
                return true;
            }
        }
        return McHelper.getServerPortalsNearby(
            player,
            20
        ).anyMatch(portal ->
            portal.dimensionTo == dimension &&
                portal.transformPointRough(playerPos).squaredDistanceTo(pos) < distanceSquare
        );
    }
    
    private static void doDestroyBlock(
        DimensionType dimension,
        PlayerActionC2SPacket packet,
        ServerPlayerEntity player
    ) {
        ServerWorld destWorld = McHelper.getServer().getWorld(dimension);
        ServerWorld oldWorld = player.interactionManager.world;
        player.interactionManager.setWorld(destWorld);
        player.interactionManager.tryBreakBlock(
            packet.getPos()
        );
        player.interactionManager.setWorld(oldWorld);
    }
    
    private static boolean canInstantMine(
        ServerWorld world,
        ServerPlayerEntity player,
        BlockPos pos
    ) {
        if (player.isCreative()) {
            return true;
        }
        
        float progress = 1.0F;
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isAir()) {
            blockState.onBlockBreakStart(world, pos, player);
            progress = blockState.calcBlockBreakingDelta(player, world, pos);
        }
        return !blockState.isAir() && progress >= 1.0F;
    }
    
    public static Pair<BlockHitResult, DimensionType> getHitResultForPlacingNew(
        World world,
        BlockHitResult blockHitResult
    ) {
        Direction side = blockHitResult.getSide();
        Vec3d sideVec = new Vec3d(side.getVector());
        Vec3d hitCenter = new Vec3d(blockHitResult.getBlockPos()).add(0.5, 0.5, 0.5);
        
        List<GlobalTrackedPortal> globalPortals = McHelper.getGlobalPortals(world);
        
        GlobalTrackedPortal portal = globalPortals.stream().filter(p ->
            p.getContentDirection().dotProduct(sideVec) > 0.9
                && p.isPointInPortalProjection(hitCenter)
                && p.getDistanceToPlane(hitCenter) < 0.6
        ).findFirst().orElse(null);
        
        if (portal == null) {
            return new Pair<>(blockHitResult, world.dimension.getType());
        }
        
        Vec3d newCenter = portal.transformPoint(hitCenter.add(sideVec));
        BlockPos placingBlockPos = new BlockPos(newCenter);
        
        BlockHitResult newHitResult = new BlockHitResult(
            Vec3d.ZERO,
            side.getOpposite(),
            placingBlockPos,
            blockHitResult.isInsideBlock()
        );
        
        return new Pair<>(newHitResult, portal.dimensionTo);
    }
    
    public static void processRightClickBlock(
        DimensionType dimension,
        PlayerInteractBlockC2SPacket packet,
        ServerPlayerEntity player
    ) {
        Hand hand = packet.getHand();
        BlockHitResult blockHitResult = packet.getHitY();
        
        ServerWorld world = McHelper.getServer().getWorld(dimension);
        
        doProcessRightClick(dimension, player, hand, blockHitResult);
    }
    
    public static void doProcessRightClick(
        DimensionType dimension,
        ServerPlayerEntity player,
        Hand hand,
        BlockHitResult blockHitResult
    ) {
        ItemStack itemStack = player.getStackInHand(hand);
        
        MinecraftServer server = McHelper.getServer();
        ServerWorld targetWorld = server.getWorld(dimension);
        
        BlockPos blockPos = blockHitResult.getBlockPos();
        Direction direction = blockHitResult.getSide();
        player.updateLastActionTime();
        if (targetWorld.canPlayerModifyAt(player, blockPos)) {
            if (!canPlayerReach(dimension, player, blockPos)) {
                Helper.log("Reject cross portal block placing packet " + player);
                return;
            }
            
            World oldWorld = player.world;
            
            player.world = targetWorld;
            try {
                ActionResult actionResult = player.interactionManager.interactBlock(
                    player,
                    targetWorld,
                    itemStack,
                    hand,
                    blockHitResult
                );
                if (actionResult.shouldSwingHand()) {
                    player.swingHand(hand, true);
                }
            }
            finally {
                player.world = oldWorld;
            }
        }
        
        MyNetwork.sendRedirectedMessage(
            player,
            dimension,
            new BlockUpdateS2CPacket(targetWorld, blockPos)
        );
        
        BlockPos offseted = blockPos.offset(direction);
        if (offseted.getY() >= 0 && offseted.getY() <= 256) {
            MyNetwork.sendRedirectedMessage(
                player,
                dimension,
                new BlockUpdateS2CPacket(targetWorld, offseted)
            );
        }
    }
}
