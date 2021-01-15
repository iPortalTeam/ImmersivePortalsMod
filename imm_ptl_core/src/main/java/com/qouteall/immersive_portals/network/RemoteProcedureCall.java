package com.qouteall.immersive_portals.network;

import com.qouteall.hiding_in_the_bushes.O_O;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class RemoteProcedureCall {
    public static void tellClientToInvoke(
        ServerPlayerEntity player,
        String methodPath,
        Object... arguments
    ) {
        if (O_O.isForge()) {
            throw new RuntimeException("Not yet supported on the Forge version");
        }
        
        CustomPayloadS2CPacket packet =
            ImplRemoteProcedureCall.createS2CPacket(methodPath, arguments);
        player.networkHandler.sendPacket(packet);
    }
    
    @Environment(EnvType.CLIENT)
    public static void tellServerToInvoke(
        String methodPath,
        Object... arguments
    ) {
        if (O_O.isForge()) {
            throw new RuntimeException("Not yet supported on the Forge version");
        }
        
        CustomPayloadC2SPacket packet =
            ImplRemoteProcedureCall.createC2SPacket(methodPath, arguments);
        MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
    }
}
