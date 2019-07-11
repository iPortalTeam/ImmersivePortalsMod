package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.packet.CustomPayloadS2CPacket;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Optional;

public class MyNetwork {
    public static final Identifier id_stcCustom =
        new Identifier("immersive_portals", "stc_custom");
    public static final Identifier id_ctsTeleport =
        new Identifier("immersive_portals", "teleport");
    public static final Identifier id_stcSpawnEntity =
        new Identifier("immersive_portals", "spawn_entity");
    public static final Identifier id_stcDimensionConfirm =
        new Identifier("immersive_portals", "dimension_confirm");
    public static final Identifier id_stcRedirected =
        new Identifier("immersive_portals", "redirected");
    
    //NOTE you must assign pid to its class
    //or it will only work in single player mode
    public static CustomPayloadS2CPacket createCustomPacketStc(
        ICustomStcPacket serializable
    ) {
        //it copies the data twice but as the packet is small it's of no problem
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(byteArrayOutputStream);
            stream.writeObject(serializable);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(byteArrayOutputStream.toByteArray());
        
        PacketByteBuf buf = new PacketByteBuf(buffer);
        
        return new CustomPayloadS2CPacket(id_stcCustom, buf);
    }
    
    private static void handleCustomPacketStc(PacketContext context, PacketByteBuf buf) {
        ByteBuffer byteBuffer = buf.nioBuffer();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer.array());
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object obj = objectInputStream.readObject();
            ICustomStcPacket customStcPacket = (ICustomStcPacket) obj;
            customStcPacket.handle();
        }
        catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static CustomPayloadC2SPacket createCtsTeleport(
        int portalEntityId
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(portalEntityId);
        return new CustomPayloadC2SPacket(id_ctsTeleport, buf);
    }
    
    private static void processCtsTeleport(PacketContext context, PacketByteBuf buf) {
        int portalEntityId = buf.readInt();
        
        Globals.serverTeleportationManager.onPlayerTeleportedInClient(
            (ServerPlayerEntity) context.getPlayer(),
            portalEntityId
        );
    }
    
    public static CustomPayloadS2CPacket createStcSpawnEntity(
        EntityType entityType,
        Entity entity
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(EntityType.getId(entityType).toString());
        buf.writeInt(entity.getEntityId());
        CompoundTag tag = new CompoundTag();
        entity.toTag(tag);
        buf.writeCompoundTag(tag);
        return new CustomPayloadS2CPacket(id_stcSpawnEntity, buf);
    }
    
    private static void processStcSpawnEntity(PacketContext context, PacketByteBuf buf) {
        String entityTypeString = buf.readString();
        int entityId = buf.readInt();
        CompoundTag compoundTag = buf.readCompoundTag();
    
        Optional<EntityType<?>> entityType = EntityType.get(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
        
        ModMain.clientTaskList.addTask(()->{
            ClientWorld world = MinecraftClient.getInstance().world;
    
            if (world == null) {
                return false;
            }
            
            Entity entity = entityType.get().create(
                world
            );
            entity.fromTag(compoundTag);
            entity.setEntityId(entityId);
            entity.method_18003(entity.x, entity.y, entity.z);
            world.addEntity(entityId, entity);
            
            return true;
        });
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
            id_stcCustom,
            MyNetwork::handleCustomPacketStc
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            id_stcSpawnEntity,
            MyNetwork::processStcSpawnEntity
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
