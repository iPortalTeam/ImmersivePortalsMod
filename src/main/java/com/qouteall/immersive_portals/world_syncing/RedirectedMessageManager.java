package com.qouteall.immersive_portals.world_syncing;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_utils.Helper;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.client.network.packet.ChunkDeltaUpdateS2CPacket;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.client.network.packet.UnloadChunkS2CPacket;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RedirectedMessageManager {
    private static final Map<Integer, Supplier<Packet>> constructorMap = new HashMap<>();
    private static final Map<Class, Integer> idMap = new HashMap<>();
    
    private static <PACKET> void register(
        Class<PACKET> clazz,
        Supplier<PACKET> constructor,
        int id
    ) {
        constructorMap.put(id, (Supplier<Packet>) constructor);
        idMap.put(clazz, id);
    }
    
    static {
        register(ChunkDataS2CPacket.class, ChunkDataS2CPacket::new, 0);
        register(UnloadChunkS2CPacket.class, UnloadChunkS2CPacket::new, 1);
        register(ChunkDeltaUpdateS2CPacket.class, ChunkDeltaUpdateS2CPacket::new, 2);
        register(
            SPacketMultiBlockChange.class,
            SPacketMultiBlockChange::new,
            3
        );
        register(
            SPacketUpdateTileEntity.class,
            SPacketUpdateTileEntity::new,
            4
        );
        register(SPacketEntityAttach.class, SPacketEntityAttach::new, 5);
        register(SPacketSetPassengers.class, SPacketSetPassengers::new, 6);
        register(SPacketTimeUpdate.class, SPacketTimeUpdate::new, 7);
        register(SPacketChangeGameState.class, SPacketChangeGameState::new, 8);
        register(SPacketSetPassengers.class, SPacketSetPassengers::new, 9);
        register(SPacketEntityEffect.class, SPacketEntityEffect::new, 10);
        register(SPacketUseBed.class, SPacketUseBed::new, 11);
        register(SPacketEntityEquipment.class, SPacketEntityEquipment::new, 12);
        register(SPacketEntityVelocity.class, SPacketEntityVelocity::new, 13);
        register(SPacketEntityProperties.class, SPacketEntityProperties::new, 14);
        register(SPacketEntityMetadata.class, SPacketEntityMetadata::new, 15);
        register(SPacketEntity.Look.class, SPacketEntity.Look::new, 16);
        register(SPacketEntity.Move.class, SPacketEntity.Move::new, 17);
        register(SPacketEntity.RelMove.class, SPacketEntity.RelMove::new, 18);
        register(SPacketEntityHeadLook.class, SPacketEntityHeadLook::new, 19);
        register(SPacketSpawnMob.class, SPacketSpawnMob::new, 20);
        register(SPacketSpawnPlayer.class, SPacketSpawnPlayer::new, 21);
        register(SPacketSpawnObject.class, SPacketSpawnObject::new, 22);
        register(SPacketSpawnPainting.class, SPacketSpawnPainting::new, 23);
        register(SPacketSpawnExperienceOrb.class, SPacketSpawnExperienceOrb::new, 24);
        register(SPacketMaps.class, SPacketMaps::new, 25);
        register(SPacketEntityTeleport.class, SPacketEntityTeleport::new, 26);
        register(SPacketDestroyEntities.class, SPacketDestroyEntities::new, 27);
        register(SPacketEntityStatus.class, SPacketEntityStatus::new, 28);
        register(SPacketAnimation.class, SPacketAnimation::new, 29);
        register(SPacketCollectItem.class, SPacketCollectItem::new, 30);
    }
    
    public static CustomPayloadS2CPacket createRedirectedMessage(
        DimensionType dimension,
        Packet packet
    ) {
        Class<? extends Packet> packetClass = packet.getClass();
        if (!idMap.containsKey(packetClass)) {
            throw new IllegalArgumentException("Unregistered Message Type" + packetClass);
        }
        int messageType = idMap.get(packetClass);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimension.getRawId());
        buf.writeInt(messageType);
    
        try {
            packet.write(buf);
        }
        catch (IOException e) {
            assert false;
            throw new IllegalArgumentException();
        }
    
        return new CustomPayloadS2CPacket(MyNetwork.id_stcRedirected, buf);
    }
    
    static Packet createEmptyPacketByType(
        int messageType
    ) {
        if (!constructorMap.containsKey(messageType)) {
            throw new IllegalArgumentException("Unregistered Message Type" + messageType);
        }
        
        return constructorMap.get(messageType).get();
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
        
        
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld clientWorld = Helper.loadClientWorld(dimension);
            
            assert clientWorld != null;
            
            ClientPlayNetworkHandler netHandler = ((IEClientWorld) clientWorld).getNetHandler();
            
            if ((netHandler).getWorld() != clientWorld) {
                ((IEClientPlayNetworkHandler)netHandler).setWorld(clientWorld);
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
