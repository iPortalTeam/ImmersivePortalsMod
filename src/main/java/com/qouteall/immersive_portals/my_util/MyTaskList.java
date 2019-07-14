package com.qouteall.immersive_portals.my_util;

import java.util.ArrayDeque;
import java.util.Queue;

//NOTE if the task returns true, it will be deleted
//if the task returns false, it will be invoked again at next time
public class MyTaskList {
    public interface MyTask {
        public boolean runAndGetIsSucceeded();
    }
    
    private Queue<MyTask> tasks = new ArrayDeque<>();
    
    //NOTE this method could be invoked while a task is running
    public synchronized void addTask(MyTask task) {
        tasks.add(task);
    }
    
    public synchronized void processTasks() {
        Queue<MyTask> oldTasks = this.tasks;
        this.tasks = new ArrayDeque<>();
    
        oldTasks.stream().filter(
            task -> !task.runAndGetIsSucceeded()
        ).forEach(
            task -> this.tasks.add(task)
        );
        
    }
}
