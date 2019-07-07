package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.world_syncing.RedirectedMessageManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class MyNetwork {
    public static final Identifier id_ctsTeleport =
        new Identifier("immersive_portals", "teleport");
    public static final Identifier id_stcDimensionConfirm =
        new Identifier("immersive_portals", "dimension_confirm");
    public static final Identifier id_stcRedirected =
        new Identifier("immersive_portals", "redirected");
    
    public static CustomPayloadS2CPacket createCtsTeleport(
        int portalEntityId
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(portalEntityId);
        return new CustomPayloadS2CPacket(id_ctsTeleport, buf);
    }
    
    private static void processCtsTeleport(PacketContext context, PacketByteBuf buf) {
        int portalEntityId = buf.readInt();
        
        assert false;
    }
    
    public static CustomPayloadS2CPacket createStcDimensionConfirm(
        ServerPlayerEntity playerEntity
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        return new CustomPayloadS2CPacket(id_stcDimensionConfirm, buf);
    }
    
    private static void processStcDimensionConfirm(PacketContext context, PacketByteBuf buf) {
        assert false;
    }
    
    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsTeleport,
            MyNetwork::processCtsTeleport
        );
    
        ClientSidePacketRegistry.INSTANCE.register(
            id_stcDimensionConfirm,
            MyNetwork::processStcDimensionConfirm
        );
    
        ClientSidePacketRegistry.INSTANCE.register(
            id_stcRedirected,
            RedirectedMessageManager::processRedirectedMessage
        );
    }
}
