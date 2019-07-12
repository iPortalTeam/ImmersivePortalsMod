package com.qouteall.immersive_portals.my_util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.stream.Collectors;

//NOTE if the task returns true, it will be deleted
//if the task returns false, it will be invoked again at next time
public class MyTaskList {
    public interface MyTask {
        public boolean runAndGetIsSucceeded();
    }
    
    private Queue<MyTask> tasks = new ArrayDeque<>();
    
    public synchronized void addTask(MyTask task) {
        tasks.add(task);
    }
    
    public synchronized void processTasks() {
        tasks = tasks.stream().filter(
            task -> !task.runAndGetIsSucceeded()
        ).collect(Collectors.toCollection(ArrayDeque::new));
        
    }
}
