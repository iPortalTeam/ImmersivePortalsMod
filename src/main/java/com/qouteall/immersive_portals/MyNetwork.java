package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.RedirectedMessageManager;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

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
    public static final Identifier id_stcSpawnLoadingIndicator =
        new Identifier("immersive_portals", "spawn_loading_indicator");
    
    //you can input a lambda expression and it will be invoked remotely
    //but java serialization is not stable
    @Deprecated
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
    
    @Deprecated
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
        DimensionType dimensionBefore,
        Vec3d posBefore,
        int portalEntityId
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimensionBefore.getRawId());
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeInt(portalEntityId);
        return new CustomPayloadC2SPacket(id_ctsTeleport, buf);
    }
    
    private static void processCtsTeleport(PacketContext context, PacketByteBuf buf) {
        DimensionType dimensionBefore = DimensionType.byRawId(buf.readInt());
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        int portalEntityId = buf.readInt();
    
        ModMain.serverTaskList.addTask(() -> {
            Globals.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.getPlayer(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
        
            return true;
        });
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static CustomPayloadS2CPacket createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(EntityType.getId(entityType).toString());
        buf.writeInt(entity.getEntityId());
        buf.writeInt(entity.dimension.getRawId());
        CompoundTag tag = new CompoundTag();
        entity.toTag(tag);
        buf.writeCompoundTag(tag);
        return new CustomPayloadS2CPacket(id_stcSpawnEntity, buf);
    }
    
    private static void processStcSpawnEntity(PacketContext context, PacketByteBuf buf) {
        String entityTypeString = buf.readString();
        int entityId = buf.readInt();
        DimensionType dimensionType = DimensionType.byRawId(buf.readInt());
        CompoundTag compoundTag = buf.readCompoundTag();
    
        Optional<EntityType<?>> entityType = EntityType.get(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
    
        ModMain.clientTaskList.addTask(() -> {
            ClientWorld world = Globals.clientWorldLoader.getOrCreateFakedWorld(dimensionType);
            
            if (world.getEntityById(entityId) != null) {
                Helper.err(String.format(
                    "duplicate entity %s %s %s",
                    ((Integer) entityId).toString(),
                    entityType.get(),
                    compoundTag
                ));
                return true;
            }
            
            Entity entity = entityType.get().create(
                world
            );
            entity.fromTag(compoundTag);
            entity.setEntityId(entityId);
            entity.updateTrackedPosition(entity.x, entity.y, entity.z);
            world.addEntity(entityId, entity);
            
            return true;
        });
    }
    
    public static CustomPayloadS2CPacket createStcDimensionConfirm(
        DimensionType dimensionType,
        Vec3d pos
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimensionType.getRawId());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new CustomPayloadS2CPacket(id_stcDimensionConfirm, buf);
    }
    
    private static void processStcDimensionConfirm(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
    
        MinecraftClient.getInstance().execute(() -> {
            Globals.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimension, pos
            );
        });
    }
    
    public static CustomPayloadS2CPacket createSpawnLoadingIndicator(
        DimensionType dimensionType,
        Vec3d pos
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimensionType.getRawId());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new CustomPayloadS2CPacket(id_stcSpawnLoadingIndicator, buf);
    }
    
    private static void processSpawnLoadingIndicator(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld world = Globals.clientWorldLoader.getDimension(dimension);
            if (world == null) {
                return;
            }
            
            LoadingIndicatorEntity indicator = new LoadingIndicatorEntity(world);
            indicator.setPosition(pos.x, pos.y, pos.z);
            
            world.addEntity(233333333, indicator);
        });
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
    
        ClientSidePacketRegistry.INSTANCE.register(
            id_stcSpawnLoadingIndicator,
            MyNetwork::processSpawnLoadingIndicator
        );
    }
}
