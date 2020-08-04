package com.qouteall.immersive_portals.network;

import com.google.common.collect.Queues;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.thread.ReentrantThreadExecutor;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientNetworkingTaskList {
    private static final ConcurrentLinkedQueue<Runnable> clientNetworkingTaskList =
        Queues.newConcurrentLinkedQueue();
    
    public static void processClientNetworkingTasks() {
        for (; ; ) {
            Runnable runnable = clientNetworkingTaskList.poll();
            if (runnable == null) {
                return;
            }
            runnable.run();
        }
    }
    
    /**
     * {@link ReentrantThreadExecutor#shouldExecuteAsync()}
     * The execution may get delayed on the render thread
     */
    public static void executeOnRenderThread(Runnable runnable) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.isOnThread()) {
            runnable.run();
        }
        else {
            client.execute(runnable);
        }
    }
    
    public static void executeOnMyTaskList(Runnable runnable) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.isOnThread()) {
            runnable.run();
        }
        else {
            clientNetworkingTaskList.add(runnable);
        }
    }
}
