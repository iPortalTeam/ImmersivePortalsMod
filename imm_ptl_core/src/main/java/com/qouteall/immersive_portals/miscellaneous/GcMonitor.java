package com.qouteall.immersive_portals.miscellaneous;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
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
        ModMain.preGameRenderSignal.connect(GcMonitor::update);
    }
    
    public static void initCommon() {
        ModMain.postServerTickSignal.connect(() -> {
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
            memoryNotEnough = true;
            Helper.err(
                "Memory not enough. Try to Shrink loading distance or allocate more memory." +
                    " If this happens with low loading distance, it usually indicates memory leak"
            );
            
            if (!O_O.isDedicatedServer()) {
                informMemoryNotEnoughClient();
            }
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
