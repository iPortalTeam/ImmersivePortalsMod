package com.qouteall.immersive_portals.render;

import java.util.ArrayDeque;

public class FPSMonitor {
    private static ArrayDeque<Integer> fpsHistory = new ArrayDeque<>();
    private static int averageFps = 60;
    private static int minimumFps = 60;
    private static final int sampleNum = 10;
    
    public static void updateEverySecond(int newFps) {
        fpsHistory.addLast(newFps);
        
        if (fpsHistory.size() > sampleNum) {
            fpsHistory.removeFirst();
        }
        
        averageFps = (int) fpsHistory.stream().mapToInt(i -> i).average().orElse(60);
        minimumFps = (int) fpsHistory.stream().mapToInt(i -> i).min().orElse(60);
    }
    
    public static int getAverageFps() {
        return averageFps;
    }
    
    public static int getMinimumFps(){
        return minimumFps;
    }
}
