package com.qouteall.immersive_portals.far_scenery;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReverseListIterator<T> implements Iterator<T> {
    ListIterator<T> iterator;
    
    public ReverseListIterator(List<T> list) {
        iterator = list.listIterator();
    }
    
    @Override
    public boolean hasNext() {
        return iterator.hasPrevious();
    }
    
    @Override
    public T next() {
        return iterator.previous();
    }
}
