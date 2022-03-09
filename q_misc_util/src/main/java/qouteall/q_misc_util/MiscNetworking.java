package qouteall.q_misc_util;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import qouteall.q_misc_util.dim_sync.DimensionIdRecord;
import qouteall.q_misc_util.dim_sync.DimensionTypeSync;

public class MiscNetworking {
    public static final ResourceLocation id_stcRemote =
        new ResourceLocation("imm_ptl", "remote_stc");
    public static final ResourceLocation id_ctsRemote =
        new ResourceLocation("imm_ptl", "remote_cts");
    
    public static final ResourceLocation id_stcDimSync =
        new ResourceLocation("imm_ptl", "dim_sync");
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            MiscNetworking.id_stcRemote,
            (c, handler, buf, responseSender) -> {
                MiscHelper.executeOnRenderThread(
                    ImplRemoteProcedureCall.clientReadFunctionAndArguments(buf)
                );
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            MiscNetworking.id_stcDimSync,
            (c, handler, buf, responseSender) -> {
                // no need to make it run on render thread
                processDimSync(buf);
            }
        );
    }
    
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(
            MiscNetworking.id_ctsRemote,
            (server, player, handler, buf, responseSender) -> {
                MiscHelper.executeOnServerThread(
                    ImplRemoteProcedureCall.serverReadFunctionAndArguments(player, buf)
                );
            }
        );
    }
    
    public static Packet createDimSyncPacket() {
        Validate.notNull(DimensionIdRecord.serverRecord);
        
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        CompoundTag idMapTag = DimensionIdRecord.recordToTag(DimensionIdRecord.serverRecord);
        buf.writeNbt(idMapTag);
        
        CompoundTag typeMapTag = DimensionTypeSync.createTagFromServerWorldInfo();
        buf.writeNbt(typeMapTag);
        
        return new ClientboundCustomPayloadPacket(id_stcDimSync, buf);
    }
    
    @Environment(EnvType.CLIENT)
    private static void processDimSync(
        FriendlyByteBuf buf
    ) {
        CompoundTag idMap = buf.readNbt();
        
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMap);
        
        CompoundTag typeMap = buf.readNbt();
        
        DimensionTypeSync.acceptTypeMapData(typeMap);
        
        Helper.log("Received Dimension Int Id Sync");
        Helper.log("\n" + DimensionIdRecord.clientRecord);
    }
}
