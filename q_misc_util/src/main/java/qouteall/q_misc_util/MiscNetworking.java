package qouteall.q_misc_util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import qouteall.q_misc_util.api.DimensionAPI;
import qouteall.q_misc_util.dimension.DimensionIdRecord;
import qouteall.q_misc_util.dimension.DimensionTypeSync;
import qouteall.q_misc_util.mixin.client.IEClientPacketListener_Misc;

import java.util.Set;
import java.util.function.Supplier;

public class MiscNetworking {
    public static final ResourceLocation id_stcRemote =
        new ResourceLocation("imm_ptl", "remote_stc");
    public static final ResourceLocation id_ctsRemote =
        new ResourceLocation("imm_ptl", "remote_cts");
    
    public static final ResourceLocation id_stcDimSync =
        new ResourceLocation("imm_ptl", "dim_sync");
    
    // no need to make this client only
    public static boolean handleMiscUtilPacketClientSide(
        ResourceLocation id,
        Supplier<FriendlyByteBuf> buf,
        ClientGamePacketListener networkHandler
    ) {
        /*if (id.equals(id_stcRemote)) {
            MiscHelper.executeOnRenderThread(
                ImplRemoteProcedureCall.clientReadPacketAndGetHandler(buf.get())
            );
            return true;
        }*/
        if (id.equals(id_stcDimSync)) {
            processDimSync(buf.get(), networkHandler);
            return true;
        }
        return false;
    }
    
    public static boolean handleMiscUtilPacketServerSide(
        ResourceLocation id,
        ServerPlayer player,
        FriendlyByteBuf buf
    ) {
//        if (id.equals(id_ctsRemote)) {
//            MiscHelper.executeOnServerThread(
//                ImplRemoteProcedureCall.serverReadPacketAndGetHandler(player, buf)
//            );
//            return true;
//        }
        return false;
    }
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
    
    }
    
    public static void init() {
    
    }
    
    public static Packet createDimSyncPacket() {
        Validate.notNull(DimensionIdRecord.serverRecord);
        
        return new ClientboundCustomPayloadPacket(new CustomPacketPayload() {
            @Override
            public void write(FriendlyByteBuf buf) {
                CompoundTag idMapTag = DimensionIdRecord.recordToTag(
                    DimensionIdRecord.serverRecord,
                    dim -> MiscHelper.getServer().getLevel(dim) != null
                );
                buf.writeNbt(idMapTag);
                
                CompoundTag typeMapTag = DimensionTypeSync.createTagFromServerWorldInfo();
                buf.writeNbt(typeMapTag);
            }
            
            @Override
            public @NotNull ResourceLocation id() {
                return id_stcDimSync;
            }
        });
        
//        return new ClientboundCustomPayloadPacket(id_stcDimSync, buf);
    }
    
    @Environment(EnvType.CLIENT)
    private static void processDimSync(
        FriendlyByteBuf buf,
        ClientGamePacketListener packetListener
    ) {
        CompoundTag idMap = buf.readNbt();
        
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMap);
        
        CompoundTag typeMap = buf.readNbt();
        
        MiscHelper.executeOnRenderThread(() -> {
            DimensionTypeSync.acceptTypeMapData(typeMap);
            
            Helper.log("Received Dimension Int Id Sync");
            Helper.log("\n" + DimensionIdRecord.clientRecord);
            
            // it's used for command completion
            Set<ResourceKey<Level>> dimIdSet = DimensionIdRecord.clientRecord.getDimIdSet();
            ((IEClientPacketListener_Misc) packetListener).ip_setLevels(dimIdSet);
            
            DimensionAPI.clientDimensionUpdateEvent.invoker().run(dimIdSet);
        });
    }
}
