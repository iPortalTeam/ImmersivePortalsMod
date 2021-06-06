package qouteall.imm_ptl.core.miscellaneous;

import qouteall.imm_ptl.core.Helper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.O_O;
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

public class GcMonitor {
    private static boolean memoryNotEnough = false;
    
    private static final WeakHashMap<GarbageCollectorMXBean, Long> lastCollectCount =
        new WeakHashMap<>();
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(3);
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGlobal.preGameRenderSignal.connect(GcMonitor::update);
    }
    
    public static void initCommon() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MinecraftServer server = McHelper.getServer();
            if (server != null) {
                if (server.isDedicated()) {
                    update();
                }
            }
        });
    }
    
    private static void update() {
        
        for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long currCount = garbageCollectorMXBean.getCollectionCount();
            
            Long lastCount = lastCollectCount.get(garbageCollectorMXBean);
            lastCollectCount.put(garbageCollectorMXBean, currCount);
            
            if (lastCount != null) {
                if (lastCount != currCount) {
                    onGced();
                }
            }
        }
    }
    
    private static void onGced() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double usage = ((double) usedMemory) / maxMemory;
        
        if (usage > 0.8) {
            if (memoryNotEnough) {
                // show message the second time
                
                if (!O_O.isDedicatedServer()) {
                    informMemoryNotEnoughClient();
                }
            }
            
            Helper.err(
                "Memory not enough. Try to Shrink loading distance or allocate more memory." +
                    " If this happens with low loading distance, it usually indicates memory leak"
            );
            
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
