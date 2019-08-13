package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.MyNetworkClient;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;

public class RedirectedMessageManager {
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
    
        return new CustomPayloadS2CPacket(MyNetworkClient.id_stcRedirected, buf);
    }
    
    private static Packet createEmptyPacketByType(
        int messageType
    ) {
        try {
            return NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, messageType);
        }
        catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static void processRedirectedMessage(
        PacketContext context,
        PacketByteBuf buf
    ) {
        int dimensionId = buf.readInt();
        int messageType = buf.readInt();
        DimensionType dimension = DimensionType.byRawId(dimensionId);
        Packet packet = createEmptyPacketByType(messageType);
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            assert false;
            throw new IllegalArgumentException();
        }
    
        processRedirectedPacket(dimension, packet);
    }
    
    private static void processRedirectedPacket(DimensionType dimension, Packet packet) {
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld clientWorld = Helper.loadClientWorld(dimension);
            
            assert clientWorld != null;
            
            assert clientWorld.getChunkManager() instanceof MyClientChunkManager;
            
            ClientPlayNetworkHandler netHandler = ((IEClientWorld) clientWorld).getNetHandler();
            
            if ((netHandler).getWorld() != clientWorld) {
                ((IEClientPlayNetworkHandler) netHandler).setWorld(clientWorld);
            }
            
            packet.apply(netHandler);
        });
    }
    
    public static void sendRedirectedMessage(
        ServerPlayerEntity player,
        DimensionType dimension,
        Packet packet
    ) {
        player.networkHandler.sendPacket(createRedirectedMessage(dimension, packet));
    }
}
