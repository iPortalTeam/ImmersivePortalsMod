package qouteall.imm_ptl.core.miscellaneous;

import qouteall.imm_ptl.core.commands.PortalDebugCommands;
import qouteall.q_misc_util.Helper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.WeakHashMap;

// Java does not provide a program-accessible interface to tell GC pause time
// so I can only roughly measure it
public class GcMonitor {
    private static boolean memoryNotEnough = false;
    
    private static final WeakHashMap<GarbageCollectorMXBean, Long> lastCollectCount =
        new WeakHashMap<>();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(3);
    private static final LimitedLogger limitedLogger2 = new LimitedLogger(3);
    
    private static long lastUpdateTime = 0;
    private static long lastLongPauseTime = 0;
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGlobal.preGameRenderSignal.connect(GcMonitor::update);
    }
    
    public static void initCommon() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MinecraftServer server = MiscHelper.getServer();
            if (server != null) {
                if (server.isDedicated()) {
                    update();
                }
            }
        });
    }
    
    private static void update() {
        long currTime = System.nanoTime();
        if (currTime - lastUpdateTime > Helper.secondToNano(0.3)) {
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
        
        if (usage > 0.8 && timeFromLongPause < Helper.secondToNano(2)) {
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
    private static void informMemoryNotEnoughClient() {
        limitedLogger.invoke(() -> {
            MinecraftClient.getInstance().inGameHud.addChatMessage(
                MessageType.SYSTEM,
                new TranslatableText("imm_ptl.memory_not_enough"),
                Util.NIL_UUID
            );
        });
    }
    
    public static boolean isMemoryNotEnough() {
        return memoryNotEnough;
    }
}
