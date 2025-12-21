package qouteall.q_misc_util;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.dimension.DimIntIdMap;
import qouteall.q_misc_util.dimension.DimensionIntId;

public class MiscNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static record DimIdSyncPacket(
        CompoundTag dimIntIdTag,
        CompoundTag dimTypeTag
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<DimIdSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(
                McHelper.newIdentifier("imm_ptl:dim_int_id_sync")
            );
        
        public static final StreamCodec<FriendlyByteBuf, DimIdSyncPacket> CODEC =
            StreamCodec.of(
                (b, p) -> p.write(b), DimIdSyncPacket::read
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
                Identifier dimTypeId = dimensionTypes.getKey(dimType);
                
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
        
        public void write(FriendlyByteBuf buf) {
            buf.writeNbt(dimIntIdTag);
            buf.writeNbt(dimTypeTag);
        }
        
        public static DimIdSyncPacket read(FriendlyByteBuf buf) {
            CompoundTag idMapTag = buf.readNbt();
            CompoundTag typeTag = buf.readNbt();
            
            return new DimIdSyncPacket(idMapTag, typeTag);
        }
        
        public void handle() {
            DimIntIdMap rec = DimIntIdMap.fromTag(dimIntIdTag);
            LOGGER.info("Client received dim id sync packet\n{}", rec);
            DimensionIntId.clientRecord = rec;
            
            ImmutableMap.Builder<ResourceKey<Level>, ResourceKey<DimensionType>> builder =
                new ImmutableMap.Builder<>();
            
            for (String key : dimTypeTag.getAllKeys()) {
                ResourceKey<Level> dimId = ResourceKey.create(
                    Registries.DIMENSION,
                    McHelper.newIdentifier(key)
                );
                String dimTypeId = dimTypeTag.getString(key);
                ResourceKey<DimensionType> dimType = ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    McHelper.newIdentifier(dimTypeId)
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
        
        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            DimIdSyncPacket.TYPE,
            (p, c) -> {
                p.handle();
            }
        );
    }
    
    public static void init() {
        PayloadTypeRegistry.playS2C().register(
            DimIdSyncPacket.TYPE, DimIdSyncPacket.CODEC
        );
    }
}
