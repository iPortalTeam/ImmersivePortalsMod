package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.imm_ptl.core.CHelper;

@Environment(EnvType.CLIENT)
@OnlyIn(Dist.CLIENT)
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
            CHelper.printChat(Component.literal(
                        "You logged into a server that doesn't have Immersive Portals mod." +
                            " Issues may arise. It's recommended to uninstall IP before joining a vanilla server"
                    ));
        });
    }
}
