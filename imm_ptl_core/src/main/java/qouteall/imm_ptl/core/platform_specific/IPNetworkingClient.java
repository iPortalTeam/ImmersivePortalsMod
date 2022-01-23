package qouteall.imm_ptl.core.platform_specific;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.dimension_sync.DimensionIdRecord;
import qouteall.imm_ptl.core.dimension_sync.DimensionTypeSync;
import qouteall.imm_ptl.core.network.IPCommonNetworkClient;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class IPNetworkingClient {
    
    private static Minecraft client = Minecraft.getInstance();
    
    public static void init() {
        
        ClientPlayNetworking.registerGlobalReceiver(
            IPNetworking.id_stcRedirected,
            (c, handler, buf, responseSender) -> {
                processRedirectedMessage(buf);
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            IPNetworking.id_stcDimSync,
            (c, handler, buf, responseSender) -> {
                processDimSync(buf);
                
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            IPNetworking.id_stcSpawnEntity,
            (c, handler, buf, responseSender) -> {
                processStcSpawnEntity(null, buf);
                
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            IPNetworking.id_stcDimensionConfirm,
            (c, handler, buf, responseSender) -> {
                processStcDimensionConfirm(null, buf);
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            IPNetworking.id_stcUpdateGlobalPortal,
            (c, handler, buf, responseSender) -> {
                processGlobalPortalUpdate(buf);
            }
        );
        
       
        
    }
    
    private static void processStcSpawnEntity(PacketContext context, FriendlyByteBuf buf) {
        String entityTypeString = buf.readUtf();
        
        int entityId = buf.readInt();
        
        ResourceKey<Level> dim = DimId.readWorldId(buf, true);
        
        CompoundTag compoundTag = buf.readNbt();
        
        IPCommonNetworkClient.processEntitySpawn(entityTypeString, entityId, dim, compoundTag);
    }
    
    private static void processStcDimensionConfirm(PacketContext context, FriendlyByteBuf buf) {
        
        ResourceKey<Level> dimension = DimId.readWorldId(buf, true);
        Vec3 pos = new Vec3(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MiscHelper.executeOnRenderThread(() -> {
            IPCGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimension, pos,
                false
            );
        });
    }
    
    public static void processRedirectedMessage(
        FriendlyByteBuf buf
    ) {
        ResourceKey<Level> dimension = DimId.readWorldId(buf, true);
        int messageType = buf.readInt();
        Packet packet = createPacketByType(messageType,buf);
        
        IPCommonNetworkClient.processRedirectedPacket(dimension, packet);
    }
    
    public static void processDimSync(
        FriendlyByteBuf buf
    ) {
        CompoundTag idMap = buf.readNbt();
        
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMap);
        
        CompoundTag typeMap = buf.readNbt();
        
        DimensionTypeSync.acceptTypeMapData(typeMap);
        
        Helper.log("Received Dimension Int Id Sync");
        Helper.log("\n" + DimensionIdRecord.clientRecord);
    }
    
    private static void processGlobalPortalUpdate(FriendlyByteBuf buf) {
        ResourceKey<Level> dimension = DimId.readWorldId(buf, true);
        CompoundTag compoundTag = buf.readNbt();
        MiscHelper.executeOnRenderThread(() -> {
            GlobalPortalStorage.receiveGlobalPortalSync(dimension, compoundTag);
        });
    }
    
    public static Packet createCtsPlayerAction(
        ResourceKey<Level> dimension,
        ServerboundPlayerActionPacket packet
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, true);
        packet.write(buf);
        return new ServerboundCustomPayloadPacket(IPNetworking.id_ctsPlayerAction, buf);
    }
    
    public static Packet createCtsRightClick(
        ResourceKey<Level> dimension,
        ServerboundUseItemOnPacket packet
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, true);
        packet.write(buf);
        return new ServerboundCustomPayloadPacket(IPNetworking.id_ctsRightClick, buf);
    }
    
    public static Packet createCtsTeleport(
        ResourceKey<Level> dimensionBefore,
        Vec3 posBefore,
        UUID portalEntityId
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionBefore, true);
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUUID(portalEntityId);
        return new ServerboundCustomPayloadPacket(IPNetworking.id_ctsTeleport, buf);
    }
    
    private static Packet createPacketByType(
        int messageType, FriendlyByteBuf buf
    ) {
        return ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, messageType, buf);
    }
}
