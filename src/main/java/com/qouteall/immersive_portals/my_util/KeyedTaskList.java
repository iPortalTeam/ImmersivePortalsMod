package com.qouteall.immersive_portals.my_util;

import java.util.HashMap;

public class KeyedTaskList<K> {
    public static interface Task<Key> {
        boolean runAndGetIsSucceeded(Key key);
    }
    
    private HashMap<K, Task<K>> tasks = new HashMap<>();
    
    public KeyedTaskList() {
    }
    
    synchronized public void addTask(K key, Task<K> func) {
        tasks.put(key, func);
    }
    
    synchronized public void processTasks() {
        HashMap<K, Task<K>> oldTasks = this.tasks;
        tasks = new HashMap<>();
        
        oldTasks.forEach((key, func) -> {
            if (!func.runAndGetIsSucceeded(key)) {
                tasks.putIfAbsent(key, func);
            }
        });
    }
    
    synchronized public boolean isTaskExist(K key) {
        return tasks.containsKey(key);
    }
}
