package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.WeakHashMap;

//dynamically adjust the player's loading distance
//if a player is loading many chunks through portals then his loading distance will decrease
//if a player is travelling too fast (through or not through portal) then his loading distance will decrease
//if the server memory is tight then loading distance will decrease
public class ServerPerformanceAdjust {
    private static class PlayerProfile {
        public int loadingChunkNum = 0;
        
        public int chunkLoadingFactor = 0;
        
        public void tick() {
            //exponentially decrease
            chunkLoadingFactor = ((int) (((double) chunkLoadingFactor) * 0.999));
        }
        
        public void onNewChunkLoaded() {
            chunkLoadingFactor++;
            loadingChunkNum++;
        }
        
        public void onNewChunkUnloaded() {
            loadingChunkNum--;
        }
        
        public boolean shouldLoadFewerChunks() {
            return isTravellingFast() || isLoadingTooMuchChunks();
        }
        
        public boolean isLoadingTooMuchChunks() {
            int valve = McHelper.getRenderDistanceOnServer() * McHelper.getRenderDistanceOnServer() * 20;
            return loadingChunkNum > valve;
        }
        
        public boolean isTravellingFast() {
            int valve = McHelper.getRenderDistanceOnServer() * McHelper.getRenderDistanceOnServer() * 30;
            return chunkLoadingFactor > valve;
        }
    }
    
    private static double memoryUsageFactor = 0;
    private static boolean isMemoryTight = false;
    private static WeakHashMap<ServerPlayerEntity, PlayerProfile> playerProfileMap = new WeakHashMap<>();
    
    public static void init() {
        NewChunkTrackingGraph.beginWatchChunkSignal.connect(
            (player, chunkPos) -> getPlayerProfile(player).onNewChunkLoaded()
        );
        NewChunkTrackingGraph.endWatchChunkSignal.connect(
            (player, chunkPos) -> getPlayerProfile(player).onNewChunkUnloaded()
        );
        ModMain.postServerTickSignal.connect(ServerPerformanceAdjust::tick);
    }
    
    private static PlayerProfile getPlayerProfile(ServerPlayerEntity player) {
        return playerProfileMap.computeIfAbsent(
            player, k -> new PlayerProfile()
        );
    }
    
    private static void tick() {
        playerProfileMap.values().forEach(PlayerProfile::tick);
        
        //exponentially decrease
        memoryUsageFactor *= 0.999;
        
        Runtime runtime = Runtime.getRuntime();
        double freeMemory = ((double) runtime.freeMemory()) / runtime.totalMemory();
        
        if (freeMemory < 0.3) {
            memoryUsageFactor += 1;
        }
        
        if (freeMemory < 0.2) {
            memoryUsageFactor += 1;
        }
        
        if (!isMemoryTight) {
            if (memoryUsageFactor > 300) {
                Helper.log("Server Memory Tight. Reduce Loading Distance");
                isMemoryTight = true;
            }
        }
        else {
            if (memoryUsageFactor < 100) {
                Helper.log("Server Memory Enough. Return to Normal Loading Distance");
                isMemoryTight = false;
            }
        }
    }
    
    public static int getPlayerLoadingDistance(ServerPlayerEntity player) {
        int distance = McHelper.getRenderDistanceOnServer();
        if (isMemoryTight) {
            return distance / 2;
        }
        PlayerProfile profile = getPlayerProfile(player);
        if (profile.shouldLoadFewerChunks()) {
            return distance / 2;
        }
        else {
            return distance;
        }
    }
    
    public static boolean getIsMemoryTight() {
        return isMemoryTight;
    }
    
}
