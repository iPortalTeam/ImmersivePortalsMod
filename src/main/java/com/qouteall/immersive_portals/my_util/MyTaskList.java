package com.qouteall.immersive_portals.my_util;

import java.util.ArrayDeque;
import java.util.Queue;

//NOTE if the task returns true, it will be deleted
//if the task returns false, it will be invoked again at next time
public class MyTaskList {
    public interface MyTask {
        public boolean runAndGetIsSucceeded();
    }
    
    private final Queue<MyTask> tasks = new ArrayDeque<>();
    private final Queue<MyTask> tasksToAdd = new ArrayDeque<>();
    
    //NOTE this method could be invoked while a task is running
    public synchronized void addTask(MyTask task) {
        tasksToAdd.add(task);
    }
    
    public synchronized void processTasks() {
        tasks.addAll(tasksToAdd);
        tasksToAdd.clear();
    
        tasks.removeIf(task -> task.runAndGetIsSucceeded());
    }
    
    public synchronized void forceClearTasks() {
        tasks.clear();
        tasksToAdd.clear();
    }
}
