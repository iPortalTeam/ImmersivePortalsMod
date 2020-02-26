package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEChunkTicketManager;
import com.qouteall.immersive_portals.ducks.IEMetricsData;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import com.qouteall.immersive_portals.ducks.IEServerChunkManager;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.MetricsData;

import java.util.Arrays;

//dynamically adjust the player's loading distance
//if a player is loading many chunks through portals then his loading distance will decrease
//if a player is travelling too fast (through or not through portal) then his loading distance will decrease
//if the server memory is tight then loading distance will decrease
public class ServerPerformanceAdjust {
    
    
    private static double serverLagFactor = 0;
    private static boolean isServerLagging = false;
    
    public static void init() {
        ModMain.postServerTickSignal.connect(ServerPerformanceAdjust::tick);
    }
    
    private static void tick() {
    
        //exponentially decrease
        serverLagFactor *= 0.998;
    
        MetricsData profile = ((IEMinecraftServer) McHelper.getServer()).getMetricsDataNonClientOnly();
        long[] samples = ((IEMetricsData) profile).getSamplesNonClientOnly();
        double averageTickTimeNano = Arrays.stream(samples).average().orElse(0);
        if (averageTickTimeNano > Helper.secondToNano(1.0 / 20)) {
            serverLagFactor += 1;
        }
    
        if (!isServerLagging) {
            if (serverLagFactor > 100) {
                Helper.log("Server is lagging. Reduce Portal Loading Distance");
                isServerLagging = true;
            }
        }
        else {
            if (serverLagFactor < 30) {
                Helper.log("Server is not lagging now. Return to Normal Portal Loading Distance");
                isServerLagging = false;
            }
        }
    }
    
    public static int getGeneralLoadingDistance() {
        int distance = McHelper.getRenderDistanceOnServer();
        if (isServerLagging) {
            return distance / 3;
        }
        return distance;
    }
    
    public static boolean getIsServerLagging() {
        return isServerLagging;
    }
    
    @Deprecated
    private static void updateTicketManagerWatchDistance() {
        int newLoadingDistance = getGeneralLoadingDistance();
        
        McHelper.getServer().getWorlds().forEach(world -> {
            ChunkTicketManager ticketManager =
                ((IEServerChunkManager) world.getChunkManager()).getTicketManager();
            
            ((IEChunkTicketManager) ticketManager).mySetWatchDistance(newLoadingDistance);
        });
    }
}
