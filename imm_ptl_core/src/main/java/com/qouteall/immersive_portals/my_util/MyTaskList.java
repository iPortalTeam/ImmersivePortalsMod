package com.qouteall.immersive_portals.my_util;

import com.qouteall.immersive_portals.Helper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

//NOTE if the task returns true, it will be deleted
//if the task returns false, it will be invoked again at next time
public class MyTaskList {
    public interface MyTask {
        public boolean runAndGetIsSucceeded();
    }
    
    private final ObjectList<MyTask> tasks = new ObjectArrayList<>();
    private final ObjectList<MyTask> tasksToAdd = new ObjectArrayList<>();
    
    // this method could be invoked while a task is running
    public synchronized void addTask(MyTask task) {
        tasksToAdd.add(task);
    }
    
    public synchronized void processTasks() {
        tasks.addAll(tasksToAdd);
        tasksToAdd.clear();
        
        Helper.removeIf(tasks, task -> {
            try {
                return task.runAndGetIsSucceeded();
            }
            catch (Throwable e) {
                Helper.err("Failed to process task " + task);
                e.printStackTrace();
                return true;
            }
        });
    }
    
    public synchronized void forceClearTasks() {
        tasks.clear();
        tasksToAdd.clear();
    }
}
