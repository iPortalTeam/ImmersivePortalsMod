package qouteall.imm_ptl.core.miscellaneous;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.commands.PortalDebugCommands;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.WeakHashMap;

// Java does not provide a program-accessible interface to tell GC pause time.
// (There are GC logs, but not accessible from within program)
// so I can only roughly measure it.
public class GcMonitor {
    private static boolean memoryNotEnough = false;
    
    private static final WeakHashMap<GarbageCollectorMXBean, Long> lastCollectCount =
        new WeakHashMap<>();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(3);
    private static final LimitedLogger limitedLogger2 = new LimitedLogger(3);
    
    private static long lastUpdateTime = 0;
    private static long lastLongPauseTime = 0;
    
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static void initClient() {
        IPGlobal.preGameRenderSignal.connect(GcMonitor::update);
    }
    
    public static void initCommon() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MinecraftServer server = MiscHelper.getServer();
            if (server != null) {
                if (server.isDedicatedServer()) {
                    update();
                }
            }
        });
    }
    
    private static void update() {
        double longPauseThresholdSeconds = 0.3;
        if (PortalDebugCommands.toMiB(Runtime.getRuntime().maxMemory()) < 2049) {
            // if only allocated 2048 MB, be more sensitive
            longPauseThresholdSeconds = 0.1;
        }
        
        long currTime = System.nanoTime();
        if (currTime - lastUpdateTime > Helper.secondToNano(longPauseThresholdSeconds)) {
            lastLongPauseTime = currTime;
        }
        lastUpdateTime = currTime;
        
        for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long currCount = garbageCollectorMXBean.getCollectionCount();
            
            Long lastCount = lastCollectCount.get(garbageCollectorMXBean);
            lastCollectCount.put(garbageCollectorMXBean, currCount);
            
            if (lastCount != null) {
                if (lastCount != currCount) {
                    check();
                }
            }
        }
        
    }
    
    private static void check() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usage = ((double) usedMemory) / maxMemory;
        
        double timeFromLongPause = System.nanoTime() - lastLongPauseTime;
        
        if (PortalDebugCommands.toMiB(freeMemory) < 300 && timeFromLongPause < Helper.secondToNano(2)) {
            if (memoryNotEnough) {
                // show message the second time
                
                if (!O_O.isDedicatedServer()) {
                    informMemoryNotEnoughClient();
                }
            }
            
            limitedLogger2.invoke(() -> {
                Helper.err(
                    "Memory not enough. Try to Shrink loading distance or allocate more memory." +
                        " If this happens with low loading distance, it usually indicates memory leak"
                );
                
                long maxMemory1 = Runtime.getRuntime().maxMemory();
                long totalMemory1 = Runtime.getRuntime().totalMemory();
                long freeMemory1 = Runtime.getRuntime().freeMemory();
                long usedMemory1 = totalMemory1 - freeMemory1;
                
                // When using ZGC, the memory usage amount is decreased with a delay
                
                Helper.err(String.format(
                    "Memory: % 2d%% %03d/%03dMB", usedMemory1 * 100L / maxMemory1,
                    PortalDebugCommands.toMiB(usedMemory1), PortalDebugCommands.toMiB(maxMemory1)
                ));
            });
            
            memoryNotEnough = true;
        }
        else {
            memoryNotEnough = false;
        }
    }
    
    @Environment(EnvType.CLIENT)
    @OnlyIn(Dist.CLIENT)
    private static void informMemoryNotEnoughClient() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (client.player.tickCount > 40) {
                limitedLogger.invoke(() -> {
                    CHelper.printChat(
                        Component.translatable("imm_ptl.memory_not_enough").append(
                            McHelper.getLinkText("https://filmora.wondershare.com/game-recording/how-to-allocate-more-ram-to-minecraft.html")
                        )
                    );
                });
            }
        }
    }
    
    public static boolean isMemoryNotEnough() {
        return memoryNotEnough;
    }
}
