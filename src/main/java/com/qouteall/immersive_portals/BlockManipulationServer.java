package com.qouteall.immersive_portals;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

public class BlockManipulationServer {
    public static void processPlayerAction(
        DimensionType dimension,
        PlayerActionC2SPacket packet,
        ServerPlayerEntity player
    ) {
        if (shouldFinishMining(dimension, packet, player)) {
            if (verifyPacket(dimension, packet, player)) {
                doDestroyBlock(dimension, packet, player);
            }
            else {
                Helper.log("Rejected cross portal block breaking packet");
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
    
    private static boolean verifyPacket(
        DimensionType dimension,
        PlayerActionC2SPacket packet,
        ServerPlayerEntity player
    ) {
        Vec3d pos = new Vec3d(packet.getPos());
        Vec3d playerPos = player.getPos();
        return McHelper.getServerPortalsNearby(
            player,
            20
        ).anyMatch(portal ->
            portal.dimensionTo == dimension &&
                portal.applyTransformationToPoint(playerPos).squaredDistanceTo(pos) < 100
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
}
