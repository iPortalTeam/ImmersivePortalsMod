package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

/**
 * Sometimes the client time jumps forward and backward. (This even happens in singleplayer).
 * We want to make the time to be stable (don't flow backward or jump too fast)
 * but still close to the time synchronized from server.
 * {@link net.minecraft.client.multiplayer.ClientPacketListener#handleSetTime(ClientboundSetTimePacket)}
 */
@Environment(EnvType.CLIENT)
public class StableClientTimer {
    
    private static boolean initialized = false;
    private static long stableTickTime = 0;
    private static float stablePartialTicks = 0;
    
    private static int timeFlowBackwardsCounter = 0;
    private static int timeFlowTooFastCounter = 0;
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(StableClientTimer::cleanup);
    }
    
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        
        if (client.level == null || client.player == null) {
            cleanup();
            return;
        }
        
        if (!initialized) {
            initialized = true;
            stableTickTime = client.level.getGameTime();
            stablePartialTicks = 0;
        }
        else {
            stableTickTime++;
        }
    }
    
    private static void cleanup() {
        initialized = false;
        stableTickTime = 0;
        stablePartialTicks = 0;
    }
    
    public static void updateFrame(long worldGameTime, float partialTicks) {
        if (!initialized) {
            initialized = true;
            stableTickTime = worldGameTime;
            stablePartialTicks = 0;
        }
        
        double deltaTime =
            (worldGameTime - stableTickTime) + partialTicks - stablePartialTicks;
        
        if (deltaTime < 0) {
            timeFlowBackwardsCounter++;
            if (timeFlowBackwardsCounter > 60) {
                Helper.err("Time flows backwards for too many times");
            }
            
            // cannot make time to flow backwards. flow forward a little
            stablePartialTicks += 0.1;
            if (stablePartialTicks > 1) {
                stablePartialTicks -= 1;
                stableTickTime++;
            }
        }
        else {
            timeFlowBackwardsCounter = 0;
            
            // cannot make time to jump for 5 ticks in one frame
            // the player cannot play in 4 FPS
            if (deltaTime > 5) {
                timeFlowTooFastCounter++;
                if (timeFlowTooFastCounter > 60) {
                    Helper.err("Time flows too fast for too many times");
                }
                
                // only flow 0.1 portion of the delta time per frame
                // the time may jump back 1 tick later so only flow a little is usually not a big deal
                stablePartialTicks += (0.1f * deltaTime);
                if (stablePartialTicks > 1) {
                    stablePartialTicks -= 1;
                    stableTickTime++;
                }
            }
            else {
                timeFlowTooFastCounter = 0;
                
                // the time is normal
                stableTickTime = worldGameTime;
                stablePartialTicks = partialTicks;
            }
            
        }
    }
}
