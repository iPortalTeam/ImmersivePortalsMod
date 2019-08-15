package com.qouteall.immersive_portals;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;

public class MyNetworkServer {
    public static final Identifier id_ctsTeleport =
        new Identifier("immersive_portals", "teleport");
    public static final Identifier id_stcCustom =
        new Identifier("immersive_portals", "stc_custom");
    public static final Identifier id_stcSpawnEntity =
        new Identifier("immersive_portals", "spawn_entity");
    public static final Identifier id_stcDimensionConfirm =
        new Identifier("immersive_portals", "dimension_confirm");
    public static final Identifier id_stcRedirected =
        new Identifier("immersive_portals", "redirected");
    public static final Identifier id_stcSpawnLoadingIndicator =
        new Identifier("immersive_portals", "spawn_loading_indicator");
    
    static void processCtsTeleport(PacketContext context, PacketByteBuf buf) {
        DimensionType dimensionBefore = DimensionType.byRawId(buf.readInt());
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        int portalEntityId = buf.readInt();
        
        ModMain.serverTaskList.addTask(() -> {
            SGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.getPlayer(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
            
            return true;
        });
    }
    
    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsTeleport,
            MyNetworkServer::processCtsTeleport
        );
    }
    
    public static CustomPayloadS2CPacket createRedirectedMessage(
        DimensionType dimension,
        Packet packet
    ) {
        int messageType = 0;
        try {
            messageType = NetworkState.PLAY.getPacketId(NetworkSide.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimension.getRawId());
        buf.writeInt(messageType);
        
        try {
            packet.write(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        return new CustomPayloadS2CPacket(id_stcRedirected, buf);
    }
    
    public static Packet createEmptyPacketByType(
        int messageType
    ) {
        try {
            return NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, messageType);
        }
        catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static void sendRedirectedMessage(
        ServerPlayerEntity player,
        DimensionType dimension,
        Packet packet
    ) {
        player.networkHandler.sendPacket(createRedirectedMessage(dimension, packet));
    }
}
