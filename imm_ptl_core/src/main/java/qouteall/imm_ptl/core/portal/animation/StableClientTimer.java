package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

/**
 * Sometimes the client time jumps forward and backward. (This even happens in singleplayer).
 * We want to make the time to be stable (don't flow backward or jump too fast)
 * but still close to the time synchronized from server.
 * {@link net.minecraft.client.multiplayer.ClientPacketListener#handleSetTime(ClientboundSetTimePacket)}
 */
@Environment(EnvType.CLIENT)
public class StableClientTimer {
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    private static final int tickCorrectionLimit = 5;
    private static final int timeToleranceTicks = 100;
    
    private static boolean initialized = false;
    private static long stableTickTime = 0;
    private static float stablePartialTicks = 0;
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(StableClientTimer::cleanup);
    }
    
    public static long getStableTickTime() {
        return stableTickTime;
    }
    
    public static float getStablePartialTicks() {
        return stablePartialTicks;
    }
    
    private static void cleanup() {
        initialized = false;
        stableTickTime = 0;
        stablePartialTicks = 0;
    }
    
    private static void reset(long worldGameTime, float partialTicks) {
        stableTickTime = worldGameTime;
        stablePartialTicks = partialTicks;
    }
    
    // updated after every tick and before every frame
    public static void update(long worldGameTime, float partialTicks) {
        if (!initialized) {
            initialized = true;
            stableTickTime = worldGameTime;
            stablePartialTicks = partialTicks;
        }
        
        double deltaTickTime =
            (worldGameTime - stableTickTime) + partialTicks - stablePartialTicks;
        
        if (deltaTickTime < 0) {
            // Time flows backward
            
            if (deltaTickTime < -timeToleranceTicks) {
                limitedLogger.err(
                    "Rest the client stable timer because it's too far behind the server time."
                );
                reset(worldGameTime, partialTicks);
                return;
            }
            
            // Firstly try to assume that the game time is one tick late, then two ticks
            for (int tickCorrection = 1; tickCorrection <= tickCorrectionLimit; tickCorrection++) {
                double newDeltaTime =
                    (tickCorrection + worldGameTime - stableTickTime) + partialTicks - stablePartialTicks;
                if (newDeltaTime >= 0 && newDeltaTime < 1) {
                    stableTickTime = tickCorrection + worldGameTime;
                    stablePartialTicks = partialTicks;
                    return;
                }
            }
            
            // the time can't be simply corrected
            // this may happen when the server is lagging
            // only move time a little
            stablePartialTicks += 0.1;
            if (stablePartialTicks > 1) {
                stablePartialTicks -= 1;
                stableTickTime++;
            }
        }
        else if (deltaTickTime > 1) {
            // time flows too fast
            if (deltaTickTime > timeToleranceTicks) {
                limitedLogger.err("Rest the client stable timer because it's too far ahead the server time.");
                reset(worldGameTime, partialTicks);
                return;
            }
            
            for (int tickCorrection = 1; tickCorrection <= tickCorrectionLimit; tickCorrection++) {
                double newDeltaTime =
                    (-tickCorrection + worldGameTime - stableTickTime) + partialTicks - stablePartialTicks;
                if (newDeltaTime <= 1 && newDeltaTime > 0) {
                    stableTickTime = worldGameTime;
                    stablePartialTicks = partialTicks;
                    return;
                }
            }
            
            // the time can't be simply corrected
            // this may happen when the server time is too fast (very rare)
            // only flow 0.1 portion of the delta time per frame
            stablePartialTicks += (0.1f * deltaTickTime);
            if (stablePartialTicks > 1) {
                stablePartialTicks -= 1;
                stableTickTime++;
            }
        }
        else {
            // the time is normal
            stableTickTime = worldGameTime;
            stablePartialTicks = partialTicks;
        }
    }
}
