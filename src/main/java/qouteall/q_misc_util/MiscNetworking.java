package qouteall.q_misc_util;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import qouteall.q_misc_util.dimension.DimIntIdMap;
import qouteall.q_misc_util.dimension.DimensionIntId;

public class MiscNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final ResourceLocation id_stcRemote =
        new ResourceLocation("imm_ptl", "remote_stc");
    public static final ResourceLocation id_ctsRemote =
        new ResourceLocation("imm_ptl", "remote_cts");
    
    public static record DimIdSyncPacket(
        CompoundTag idMapTag
    ) implements FabricPacket {
        public static final PacketType<DimIdSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl", "dim_int_id_sync"),
            DimIdSyncPacket::read
        );
        
        public static DimIdSyncPacket createFromServer(MinecraftServer server) {
            DimIntIdMap rec = DimensionIntId.getServerMap(server);
            CompoundTag tag = DimIntIdMap.recordToTag(rec, dim -> true);
            
            return new DimIdSyncPacket(tag);
        }
        
        public static Packet<ClientCommonPacketListener> createPacket(MinecraftServer server) {
            return ServerPlayNetworking.createS2CPacket(
                DimIdSyncPacket.createFromServer(server)
            );
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeNbt(idMapTag);
        }
        
        public static DimIdSyncPacket read(FriendlyByteBuf buf) {
            CompoundTag idMapTag = buf.readNbt();
            return new DimIdSyncPacket(idMapTag);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        public void handleOnNetworkingThread(ClientGamePacketListener packetListener) {
            DimensionIntId.clientRecord = DimIntIdMap.tagToRecord(idMapTag);
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
//        ClientPlayNetworking.registerGlobalReceiver(
//            DimSyncPacket.TYPE,
//            (packet, player, responseSender) -> {
//                packet.handleOnNetworkingThread(player.connection);
//            }
//        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            DimIdSyncPacket.TYPE.getId(),
            (client, handler, buf, responseSender) -> {
                // must be handled early
                // should not be handled in client main thread, otherwise it may be late
                DimIdSyncPacket dimIdSyncPacket = DimIdSyncPacket.TYPE.read(buf);
                dimIdSyncPacket.handleOnNetworkingThread(handler);
            }
        );
    }
    
    public static void init() {
    
    }
}
