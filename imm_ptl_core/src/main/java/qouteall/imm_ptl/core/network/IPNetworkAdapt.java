package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;

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
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().gui.handleChat(
                ChatType.SYSTEM,
                new TextComponent(
                    "You logged into a server that doesn't have Immersive Portals mod." +
                        " Issues may arise. It's recommended to uninstall IP before joining a vanilla server"
                ),
                Util.NIL_UUID
            );
        });
    }
}
