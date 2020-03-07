package com.qouteall.immersive_portals;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClassicalPortalConverter {
    
    public static interface PortalConverter {
        void onPlayerTeleported(
            ServerWorld oldWorld,
            ServerPlayerEntity player,
            BlockPos portalBlockPos
        );
    }
    
    private static Map<Block, PortalConverter> converterMap = new HashMap<>();
    
    public static void onPlayerChangeDimension(
        ServerPlayerEntity player,
        ServerWorld oldWorld,
        Vec3d oldPos
    ) {
        BlockPos playerPos = new BlockPos(oldPos);
        Iterator<BlockPos> iterator = BlockPos.stream(
            playerPos.add(-2, -2, -2),
            playerPos.add(2, 2, 2)
        ).iterator();
        while (iterator.hasNext()) {
            BlockPos blockPos = iterator.next();
            Block block = oldWorld.getBlockState(blockPos).getBlock();
            PortalConverter portalConverter = converterMap.get(block);
            if (portalConverter != null) {
                portalConverter.onPlayerTeleported(
                    oldWorld, player, blockPos
                );
                return;
            }
        }
    }
    
    public static void init() {
        converterMap.put(
            Blocks.END_PORTAL,
            (oldWorld, player, portalBlockPos) -> {
                //TODO finish it
            }
        );
    }
    
    
}
