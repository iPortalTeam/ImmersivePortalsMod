package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class IPNetworkAdapt {
    private static boolean serverHasIP = true;
    
    public static void setServerHasIP(boolean cond) {
        if (serverHasIP) {
            if (!cond) {
                warnServerMissingIP();
            }
        }
        
        serverHasIP = cond;
    }
    
    public static boolean doesServerHasIP() {
        return serverHasIP;
    }
    
    private static void warnServerMissingIP() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().inGameHud.addChatMessage(
                MessageType.SYSTEM,
                new LiteralText(
                    "You logged into a server that doesn't have Immersive Portals mod." +
                        " Issues may arise. It's recommended to uninstall IP before joining a vanilla server"
                ),
                Util.NIL_UUID
            );
        });
    }
}
