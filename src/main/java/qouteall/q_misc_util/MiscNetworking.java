package qouteall.q_misc_util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.q_misc_util.dimension.DimIntIdMap;
import qouteall.q_misc_util.dimension.DimensionIntId;

public class MiscNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final ResourceLocation id_stcRemote =
        new ResourceLocation("imm_ptl", "remote_stc");
    public static final ResourceLocation id_ctsRemote =
        new ResourceLocation("imm_ptl", "remote_cts");
    
    public static record DimIdSyncPacket(
        CompoundTag dimIntIdTag,
        CompoundTag dimTypeTag
    ) implements FabricPacket {
        public static final PacketType<DimIdSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl", "dim_int_id_sync"),
            DimIdSyncPacket::read
        );
        
        public static DimIdSyncPacket createFromServer(MinecraftServer server) {
            DimIntIdMap rec = DimensionIntId.getServerMap(server);
            CompoundTag dimIntIdTag = rec.toTag(dim -> true);
            
            RegistryAccess registryManager = server.registryAccess();
            Registry<DimensionType> dimensionTypes = registryManager.registryOrThrow(Registries.DIMENSION_TYPE);
            
            CompoundTag dimIdToDimTypeIdTag = new CompoundTag();
            for (ServerLevel world : server.getAllLevels()) {
                ResourceKey<Level> dimId = world.dimension();
                
                DimensionType dimType = world.dimensionType();
                ResourceLocation dimTypeId = dimensionTypes.getKey(dimType);
                
                if (dimTypeId == null) {
                    LOGGER.error("Cannot find dimension type for {}", dimId.location());
                    LOGGER.error(
                        "Registered dimension types {}", dimensionTypes.keySet()
                    );
                    dimTypeId = BuiltinDimensionTypes.OVERWORLD.location();
                }
                
                dimIdToDimTypeIdTag.putString(
                    dimId.location().toString(),
                    dimTypeId.toString()
                );
            }
            
            return new DimIdSyncPacket(dimIntIdTag, dimIdToDimTypeIdTag);
        }
        
        public static Packet<ClientCommonPacketListener> createPacket(MinecraftServer server) {
            return ServerPlayNetworking.createS2CPacket(
                DimIdSyncPacket.createFromServer(server)
            );
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeNbt(dimIntIdTag);
            buf.writeNbt(dimTypeTag);
        }
        
        public static DimIdSyncPacket read(FriendlyByteBuf buf) {
            CompoundTag idMapTag = buf.readNbt();
            CompoundTag typeTag = buf.readNbt();
            
            return new DimIdSyncPacket(idMapTag, typeTag);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        public void handleOnNetworkingThread(ClientGamePacketListener packetListener) {
            DimIntIdMap rec = DimIntIdMap.fromTag(dimIntIdTag);
            LOGGER.info("Client received dim id sync packet\n{}", rec);
            DimensionIntId.clientRecord = rec;
            
            ImmutableMap.Builder<ResourceKey<Level>, ResourceKey<DimensionType>> builder =
                new ImmutableMap.Builder<>();
            
            for (String key : dimTypeTag.getAllKeys()) {
                ResourceKey<Level> dimId = ResourceKey.create(
                    Registries.DIMENSION,
                    new ResourceLocation(key)
                );
                String dimTypeId = dimTypeTag.getString(key);
                ResourceKey<DimensionType> dimType = ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    new ResourceLocation(dimTypeId)
                );
                builder.put(dimId, dimType);
            }
            
            var dimTypeMap = builder.build();
            ClientWorldLoader.dimIdToDimTypeId = dimTypeMap;
            LOGGER.info(
                "Client accepted dimension type mapping {}",
                dimTypeMap
            );
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
