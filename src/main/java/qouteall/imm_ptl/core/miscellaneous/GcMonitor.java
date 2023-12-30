package qouteall.imm_ptl.core.miscellaneous;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.commands.PortalDebugCommands;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.CountDownInt;
import qouteall.q_misc_util.my_util.MyTaskList;

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
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static final CountDownInt MESSAGE_LIMIT = new CountDownInt(3);
    private static final CountDownInt LOG_LIMIT = new CountDownInt(3);
    
    private static long lastUpdateTime = 0;
    private static long lastLongPauseTime = 0;
    
    public static final String LINK = "https://filmora.wondershare.com/game-recording/how-to-allocate-more-ram-to-minecraft.html";
    
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        IPGlobal.PRE_GAME_RENDER_EVENT.register(GcMonitor::update);
        
        long maxMemory = Runtime.getRuntime().maxMemory();
        long maxMemoryMB = PortalDebugCommands.toMiB(maxMemory);
        if (maxMemoryMB <= 2048) {
            IPGlobal.CLIENT_TASK_LIST.addTask(MyTaskList.withDelayCondition(
                () -> Minecraft.getInstance().level == null,
                MyTaskList.oneShotTask(() -> {
                    if (IPConfig.getConfig().shouldDisplayWarning("low_max_memory")) {
                        CHelper.printChat(
                            Component.translatable("imm_ptl.low_max_memory", maxMemoryMB)
                                .withStyle(ChatFormatting.RED)
                                .append(McHelper.getLinkText(LINK))
                                .append(
                                    IPMcHelper.getDisableWarningText("low_max_memory")
                                )
                        );
                    }
                })
            ));
        }
    }
    
    public static void initCommon() {
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            if (server.isDedicatedServer()) {
                update();
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
            
            if (LOG_LIMIT.tryDecrement()) {
                LOGGER.error(
                    "Memory seems not enough. Try to Shrink loading distance or allocate more memory."
                );
                
                long maxMemory1 = Runtime.getRuntime().maxMemory();
                long totalMemory1 = Runtime.getRuntime().totalMemory();
                long freeMemory1 = Runtime.getRuntime().freeMemory();
                long usedMemory1 = totalMemory1 - freeMemory1;
                
                // When using ZGC, the memory usage amount is decreased with a delay
                
                LOGGER.info(String.format(
                    "Memory: % 2d%% %03d/%03dMB", usedMemory1 * 100L / maxMemory1,
                    PortalDebugCommands.toMiB(usedMemory1), PortalDebugCommands.toMiB(maxMemory1)
                ));
                
                if (LOG_LIMIT.isZero()) {
                    LOGGER.info("Memory warning logging reached limit.");
                }
            }
            
            memoryNotEnough = true;
        }
        else {
            memoryNotEnough = false;
        }
    }
    
    @Environment(EnvType.CLIENT)
    private static void informMemoryNotEnoughClient() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            if (client.player.tickCount > 40) {
                if (MESSAGE_LIMIT.tryDecrement()) {
                    CHelper.printChat(
                        Component.translatable("imm_ptl.memory_not_enough").append(
                            McHelper.getLinkText(LINK)
                        )
                    );
                }
            }
        }
    }
    
    public static boolean isMemoryNotEnough() {
        return memoryNotEnough;
    }
}
