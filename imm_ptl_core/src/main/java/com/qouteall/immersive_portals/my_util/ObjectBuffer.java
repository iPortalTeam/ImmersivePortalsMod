package com.qouteall.immersive_portals.my_util;

import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ObjectBuffer<T> {
    private ArrayDeque<T> objects = new ArrayDeque<>();
    private int cacheSize;
    private Supplier<T> creator;
    private Consumer<T> destroyer;
    
    private int currentConsumption = 0;
    
    public ObjectBuffer(int cacheSize, Supplier<T> creator, Consumer<T> destroyer) {
        this.cacheSize = cacheSize;
        this.creator = creator;
        this.destroyer = destroyer;
    }
    
    public void setCacheSize(int newVal) {
        cacheSize = newVal;
        while (objects.size() > cacheSize) {
            T obj = objects.pollFirst();
            destroyer.accept(obj);
        }
    }
    
    public void reserveObjects(int num) {
        int requirement = cacheSize - objects.size();
        int supply = Math.max(0, Math.min(num, requirement));
        
        for (int i = 0; i < supply; i++) {
            objects.addFirst(creator.get());
        }
        
        currentConsumption = 0;
    }
    
    public void reserveObjectsByRatio(double ratio) {
        reserveObjects((int) (cacheSize * ratio));
    }
    
    public T takeObject() {
        if (objects.isEmpty()) {
            currentConsumption++;
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
        objects.clear();
    }
}
