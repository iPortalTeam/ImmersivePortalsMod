package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

/**
 * TODO make sure that network latency does not make client game time jump
 * {@link net.minecraft.client.multiplayer.ClientPacketListener#handleSetTime(ClientboundSetTimePacket)}
 */
public class StableClientTimer {
    
    private static boolean initialized = false;
    private static long clientGameTime = 0;
    
    public static void tick() {
        if (!initialized) {
            initialized = true;
            clientGameTime = Minecraft.getInstance().level.getGameTime();
        }
    }
}
