package com.qouteall.modloader_agnostic_api;

import com.qouteall.immersive_portals.compat.RequiemCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

public class MAA {
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
        ((IEClientWorldMAA) fromWorld).removeEntityWhilstMaintainingCapability(entity);
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
}
