package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.exposer.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.exposer.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.server.network.packet.CustomPayloadC2SPacket;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class MyNetworkClient {
    
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
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsTeleport, buf);
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
            ClientWorld world = CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimensionType);
            
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
    
    private static void processStcDimensionConfirm(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MinecraftClient.getInstance().execute(() -> {
            CGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimension, pos
            );
        });
    }
    
    private static void processSpawnLoadingIndicator(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld world = CGlobal.clientWorldLoader.getDimension(dimension);
            if (world == null) {
                return;
            }
            
            LoadingIndicatorEntity indicator = new LoadingIndicatorEntity(world);
            indicator.setPosition(pos.x, pos.y, pos.z);
            
            world.addEntity(233333333, indicator);
        });
    }
    
    public static void init() {
        
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcCustom,
            MyNetworkClient::handleCustomPacketStc
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcSpawnEntity,
            MyNetworkClient::processStcSpawnEntity
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcDimensionConfirm,
            MyNetworkClient::processStcDimensionConfirm
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcRedirected,
            MyNetworkClient::processRedirectedMessage
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcSpawnLoadingIndicator,
            MyNetworkClient::processSpawnLoadingIndicator
        );
    }
    
    public static void processRedirectedMessage(
        PacketContext context,
        PacketByteBuf buf
    ) {
        int dimensionId = buf.readInt();
        int messageType = buf.readInt();
        DimensionType dimension = DimensionType.byRawId(dimensionId);
        Packet packet = MyNetwork.createEmptyPacketByType(messageType);
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
}
