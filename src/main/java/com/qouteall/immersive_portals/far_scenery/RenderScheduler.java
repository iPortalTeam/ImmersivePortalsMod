package com.qouteall.immersive_portals.far_scenery;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.MyTaskList;

public class RenderScheduler {
    private long startTime = 0;
    
    public RenderScheduler() {
    }
    
    public void onRenderStart() {
        startTime = System.nanoTime();
    }
    
    private boolean shouldRenderNextSection() {
        long currTime = System.nanoTime();
        long valve = Helper.secondToNano(1.0 / 120);
        return currTime - startTime > valve;
    }
    
    public MyTaskList.MyTask limitTaskTime(MyTaskList.MyTask task) {
        return () -> {
            if (!shouldRenderNextSection()) {
                return false;
            }
            return task.runAndGetIsSucceeded();
        };
    }
}
