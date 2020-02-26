package com.qouteall.immersive_portals.my_util;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectBuffer<T> {
    private ArrayDeque<T> objects = new ArrayDeque<>();
    private int cacheSize;
    private Supplier<T> creator;
    private Consumer<T> destroyer;
    
    public ObjectBuffer(int cacheSize, Supplier<T> creator, Consumer<T> destroyer) {
        this.cacheSize = cacheSize;
        this.creator = creator;
        this.destroyer = destroyer;
    }
    
    public void reserveObjects(int num) {
        int requirement = cacheSize - objects.size();
        int supply = Math.max(0, Math.min(num, requirement));
        
        for (int i = 0; i < supply; i++) {
            objects.addFirst(creator.get());
        }
    }
    
    public T takeObject() {
        if (objects.isEmpty()) {
            return creator.get();
        }
        else {
            return objects.pollFirst();
        }
    }
    
    public void returnObject(T t) {
        if (objects.size() >= cacheSize) {
            destroyer.accept(t);
        }
        else {
            objects.addFirst(t);
        }
    }
    
    public void destroyAll() {
        objects.forEach(destroyer);
    }
}
