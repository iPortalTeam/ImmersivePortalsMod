package qouteall.imm_ptl.core.platform_specific;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
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
    
    private static MinecraftClient client = MinecraftClient.getInstance();
    
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
    
    private static void processStcSpawnEntity(PacketContext context, PacketByteBuf buf) {
        String entityTypeString = buf.readString();
        
        int entityId = buf.readInt();
        
        RegistryKey<World> dim = DimId.readWorldId(buf, true);
        
        NbtCompound compoundTag = buf.readNbt();
        
        IPCommonNetworkClient.processEntitySpawn(entityTypeString, entityId, dim, compoundTag);
    }
    
    private static void processStcDimensionConfirm(PacketContext context, PacketByteBuf buf) {
        
        RegistryKey<World> dimension = DimId.readWorldId(buf, true);
        Vec3d pos = new Vec3d(
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
        PacketByteBuf buf
    ) {
        RegistryKey<World> dimension = DimId.readWorldId(buf, true);
        int messageType = buf.readInt();
        Packet packet = createPacketByType(messageType,buf);
        
        IPCommonNetworkClient.processRedirectedPacket(dimension, packet);
    }
    
    public static void processDimSync(
        PacketByteBuf buf
    ) {
        NbtCompound idMap = buf.readNbt();
        
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMap);
        
        NbtCompound typeMap = buf.readNbt();
        
        DimensionTypeSync.acceptTypeMapData(typeMap);
        
        Helper.log("Received Dimension Int Id Sync");
        Helper.log("\n" + DimensionIdRecord.clientRecord);
    }
    
    private static void processGlobalPortalUpdate(PacketByteBuf buf) {
        RegistryKey<World> dimension = DimId.readWorldId(buf, true);
        NbtCompound compoundTag = buf.readNbt();
        MiscHelper.executeOnRenderThread(() -> {
            GlobalPortalStorage.receiveGlobalPortalSync(dimension, compoundTag);
        });
    }
    
    public static Packet createCtsPlayerAction(
        RegistryKey<World> dimension,
        PlayerActionC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, true);
        packet.write(buf);
        return new CustomPayloadC2SPacket(IPNetworking.id_ctsPlayerAction, buf);
    }
    
    public static Packet createCtsRightClick(
        RegistryKey<World> dimension,
        PlayerInteractBlockC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, true);
        packet.write(buf);
        return new CustomPayloadC2SPacket(IPNetworking.id_ctsRightClick, buf);
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
        return new CustomPayloadC2SPacket(IPNetworking.id_ctsTeleport, buf);
    }
    
    private static Packet createPacketByType(
        int messageType, PacketByteBuf buf
    ) {
        return NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, messageType, buf);
    }
}
