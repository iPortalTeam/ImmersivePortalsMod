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
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimensionIdRecord;
import qouteall.q_misc_util.dimension.DimensionTypeSync;
import qouteall.q_misc_util.mixin.client.IEClientPacketListener_Misc;

import java.util.Set;

public class MiscNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final ResourceLocation id_stcRemote =
        new ResourceLocation("imm_ptl", "remote_stc");
    public static final ResourceLocation id_ctsRemote =
        new ResourceLocation("imm_ptl", "remote_cts");
    
    public static record DimSyncPacket(
        CompoundTag idMapTag,
        CompoundTag typeMapTag
    ) implements FabricPacket {
        public static final PacketType<DimSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl", "dim_sync"),
            DimSyncPacket::read
        );
        
        public static DimSyncPacket createFromServer(MinecraftServer server) {
            CompoundTag idMapTag = DimensionIdRecord.recordToTag(
                DimensionIdRecord.serverRecord,
                dim -> server.getLevel(dim) != null
            );
            
            CompoundTag typeMapTag = DimensionTypeSync.createTagFromServerWorldInfo(server);
            
            return new DimSyncPacket(idMapTag, typeMapTag);
        }
        
        public static Packet<ClientCommonPacketListener> createPacket(MinecraftServer server) {
            return ServerPlayNetworking.createS2CPacket(
                MiscNetworking.DimSyncPacket.createFromServer(server)
            );
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeNbt(idMapTag);
            buf.writeNbt(typeMapTag);
        }
        
        public static DimSyncPacket read(FriendlyByteBuf buf) {
            CompoundTag idMapTag = buf.readNbt();
            CompoundTag typeMapTag = buf.readNbt();
            return new DimSyncPacket(idMapTag, typeMapTag);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        public void handleOnNetworkingThread(ClientGamePacketListener packetListener) {
            DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMapTag);
            
            DimensionTypeSync.acceptTypeMapData(typeMapTag);
            
            LOGGER.info("Received Dimension Id Sync\n{}", DimensionIdRecord.clientRecord);
            
            // it's used for command completion
            Set<ResourceKey<Level>> dimIdSet = DimensionIdRecord.clientRecord.getDimIdSet();
            ((IEClientPacketListener_Misc) packetListener).ip_setLevels(dimIdSet);
            
            MiscHelper.executeOnRenderThread(() -> {
                DimensionAPI.CLIENT_DIMENSION_UPDATE_EVENT.invoker().run(dimIdSet);
            });
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
            DimSyncPacket.TYPE.getId(),
            (client, handler, buf, responseSender) -> {
                // must be handled early
                // should not be handled in client main thread, otherwise it may be late
                DimSyncPacket dimSyncPacket = DimSyncPacket.TYPE.read(buf);
                dimSyncPacket.handleOnNetworkingThread(handler);
            }
        );
    }
    
    public static void init() {
    
    }
}
