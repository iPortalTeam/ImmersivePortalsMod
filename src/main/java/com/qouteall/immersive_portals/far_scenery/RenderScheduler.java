package com.qouteall.immersive_portals.far_scenery;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.MyTaskList;

public class RenderScheduler {
    private long passStartTimeNano = 0;
    private double timeValveSeconds;
    private long renderStartTimeNano;
    private HeuristicChaosEquationSolver equationSolver =
        new HeuristicChaosEquationSolver(10);
    
    public RenderScheduler() {
        double initialTimeValve = 1.0 / (60);
        
        equationSolver.addPriorKnowledge(
            0,
            23
        );
        equationSolver.addPriorKnowledge(
            23,
            -((23 * 23) / initialTimeValve) + 23
        );
        
        timeValveSeconds = initialTimeValve;
    }
    
    public void onRenderLaunch() {
        renderStartTimeNano = System.nanoTime();
    }
    
    public void onRenderSucceeded() {
        double expectedIntervalSeconds = 0.2;
        
        double intervalSeconds = Helper.nanoToSecond(System.nanoTime() - renderStartTimeNano);
        double newTimeValve = equationSolver.feedNewDataAndGetNewRoot(
            timeValveSeconds, intervalSeconds - expectedIntervalSeconds
        );
        Helper.log(String.format(
            "Render Finished %s %s %s",
            timeValveSeconds,
            intervalSeconds,
            newTimeValve
        ));
        
        timeValveSeconds = newTimeValve;
        timeValveSeconds = Math.max(1.0 / (60 * 500), timeValveSeconds);
    }
    
    public void onRenderPassStart() {
        passStartTimeNano = System.nanoTime();
    }
    
    private boolean shouldRenderNextSection() {
        long currTimeNano = System.nanoTime();
        long timeValveNano = Helper.secondToNano(timeValveSeconds);
        return currTimeNano - passStartTimeNano < timeValveNano;
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
