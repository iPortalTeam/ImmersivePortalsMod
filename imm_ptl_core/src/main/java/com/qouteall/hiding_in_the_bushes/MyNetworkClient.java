package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.dimension_sync.DimensionIdRecord;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.network.CommonNetworkClient;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class MyNetworkClient {
    
    private static MinecraftClient client = MinecraftClient.getInstance();
    
    public static void init() {
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcRedirected,
            MyNetworkClient::processRedirectedMessage
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcDimSync,
            MyNetworkClient::processDimSync
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
            MyNetwork.id_stcUpdateGlobalPortal,
            MyNetworkClient::processGlobalPortalUpdate
        );
        
    }
    
    private static void processStcSpawnEntity(PacketContext context, PacketByteBuf buf) {
        String entityTypeString = buf.readString();
        
        int entityId = buf.readInt();
        
        RegistryKey<World> dim = DimId.readWorldId(buf, true);
        
        CompoundTag compoundTag = buf.readCompoundTag();
    
        CommonNetworkClient.processEntitySpawn(entityTypeString, entityId, dim, compoundTag);
    }
    
    private static void processStcDimensionConfirm(PacketContext context, PacketByteBuf buf) {
        
        RegistryKey<World> dimension = DimId.readWorldId(buf, true);
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        CHelper.executeOnRenderThread(() -> {
            CGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimension, pos,
                false
            );
        });
    }
    
    public static void processRedirectedMessage(
        PacketContext context,
        PacketByteBuf buf
    ) {
        RegistryKey<World> dimension = DimId.readWorldId(buf, true);
        int messageType = buf.readInt();
        Packet packet = createEmptyPacketByType(messageType);
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        CommonNetworkClient.processRedirectedPacket(dimension, packet);
    }
    
    public static void processDimSync(
        PacketContext context, PacketByteBuf buf
    ) {
        CompoundTag idMap = buf.readCompoundTag();
        
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMap);
        
        CompoundTag typeMap = buf.readCompoundTag();
        
        DimensionTypeSync.acceptTypeMapData(typeMap);
        
        Helper.log("Received Dimension Int Id Sync");
        Helper.log("\n" + DimensionIdRecord.clientRecord);
    }
    
    private static void processGlobalPortalUpdate(PacketContext context, PacketByteBuf buf) {
        RegistryKey<World> dimension = DimId.readWorldId(buf, true);
        CompoundTag compoundTag = buf.readCompoundTag();
        CHelper.executeOnRenderThread(() -> {
            GlobalPortalStorage.receiveGlobalPortalSync(dimension, compoundTag);
        });
    }
    
    public static Packet createCtsPlayerAction(
        RegistryKey<World> dimension,
        PlayerActionC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, true);
        try {
            packet.write(buf);
        }
        
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsPlayerAction, buf);
    }
    
    public static Packet createCtsRightClick(
        RegistryKey<World> dimension,
        PlayerInteractBlockC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, true);
        try {
            packet.write(buf);
        }
        
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsRightClick, buf);
    }
    
    public static Packet createCtsTeleport(
        RegistryKey<World> dimensionBefore,
        Vec3d posBefore,
        UUID portalEntityId
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionBefore, true);
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUuid(portalEntityId);
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsTeleport, buf);
    }
    
    private static Packet createEmptyPacketByType(
        int messageType
    ) {
        return NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, messageType);
    }
}
