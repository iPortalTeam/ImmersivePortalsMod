package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.server.MinecraftServer;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;

public class ServerPerformanceMonitor {
    
    private static final int sampleNum = 20;
    
    private static PerformanceLevel level = PerformanceLevel.bad;
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(50);
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(ServerPerformanceMonitor::tick);
    }
    
    private static long lastUpdateTime = 0;
    
    private static void tick() {
        if (!IPGlobal.enableServerPerformanceAdjustment) {
            level = PerformanceLevel.good;
            return;
        }
        
        MinecraftServer server = MiscHelper.getServer();
        if (server == null) {
            return;
        }
        
        if (!server.isRunning()) {
            return;
        }
        
        long currTime = System.nanoTime();
        
        // update every 20 seconds
        if (currTime - lastUpdateTime < Helper.secondToNano(20)) {
            return;
        }
        else {
            lastUpdateTime = currTime;
        }
        
        float tickTime = server.getAverageTickTime();
        PerformanceLevel newLevel = PerformanceLevel.getServerPerformanceLevel(tickTime);
        if (newLevel != level) {
            level = newLevel;
            limitedLogger.log("Server performance level: " + newLevel);
        }
    }
    
    public static PerformanceLevel getLevel() {
        return level;
    }
    
}
