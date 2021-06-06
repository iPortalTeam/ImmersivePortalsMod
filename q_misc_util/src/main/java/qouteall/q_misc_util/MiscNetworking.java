package qouteall.q_misc_util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

public class MiscNetworking {
    public static final Identifier id_stcRemote =
        new Identifier("imm_ptl", "remote_stc");
    public static final Identifier id_ctsRemote =
        new Identifier("imm_ptl", "remote_cts");
    
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
}
