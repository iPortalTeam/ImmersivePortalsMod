package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.server.MinecraftServer;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;

public class ServerPerformanceMonitor {
    
    private static final int sampleNum = 20;
    
    private static PerformanceLevel level = PerformanceLevel.bad;
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(50);
    
    public static void init() {
        IPGlobal.postServerTickSignal.connect(ServerPerformanceMonitor::tick);
    }
    
    private static void tick() {
        MinecraftServer server = MiscHelper.getServer();
        if (server == null) {
            return;
        }
        
        if (!server.isRunning()) {
            return;
        }
        
        float tickTime = server.getTickTime();
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
