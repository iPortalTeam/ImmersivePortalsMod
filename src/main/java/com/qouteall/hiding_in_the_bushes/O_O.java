package com.qouteall.hiding_in_the_bushes;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;

public class O_O {
    public static boolean isReachEntityAttributesPresent;
    
    @Environment(EnvType.CLIENT)
    public static void onPlayerChangeDimensionClient(
        DimensionType from, DimensionType to
    ) {
        RequiemCompat.onPlayerTeleportedClient();
    }
    
    @Environment(EnvType.CLIENT)
    public static void segregateClientEntity(
        ClientWorld fromWorld,
        Entity entity
    ) {
        ((IEClientWorld_MA) fromWorld).removeEntityWhilstMaintainingCapability(entity);
        entity.removed = false;
    }
    
    public static void segregateServerEntity(
        ServerWorld fromWorld,
        Entity entity
    ) {
        fromWorld.removeEntity(entity);
        entity.removed = false;
    }
    
    public static void segregateServerPlayer(
        ServerWorld fromWorld,
        ServerPlayerEntity player
    ) {
        fromWorld.removePlayer(player);
        player.removed = false;
    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayerEntity player,
        DimensionType from,
        DimensionType to
    ) {
        RequiemCompat.onPlayerTeleportedServer(player);
    }
    
    public static void loadConfigFabric() {
        MyConfig.onConfigChanged(MyConfig.readConfigFromFile());
    }
    
    public static boolean isObsidian(IWorld world, BlockPos obsidianPos) {
        return world.getBlockState(obsidianPos) == Blocks.OBSIDIAN.getDefaultState();
    }
}
