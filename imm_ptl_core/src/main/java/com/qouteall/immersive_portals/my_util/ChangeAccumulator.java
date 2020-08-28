package com.qouteall.immersive_portals.my_util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ChangeAccumulator<KEY> {
    private Set<KEY> changedKeys = new HashSet<>();
    private Consumer<KEY> updatingFunction;
    
    public ChangeAccumulator(Consumer<KEY> updatingFunction) {
        this.updatingFunction = updatingFunction;
    }
    
    public synchronized void notifyChanged(KEY key){
        changedKeys.add(key);
    }
    
    public synchronized void processChanges(){
        changedKeys.forEach(updatingFunction);
        changedKeys.clear();
    }
}
