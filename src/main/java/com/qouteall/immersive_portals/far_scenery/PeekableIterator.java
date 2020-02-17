package com.qouteall.immersive_portals.far_scenery;

import java.util.Iterator;

//normal iterator does not support the operation that
//get the first element without taking it
public class PeekableIterator<T> implements Iterator<T> {
    private Iterator<T> iterator;
    private T buffer;
    
    public PeekableIterator(Iterator<T> iter) {
        iterator = iter;
    }
    
    @Override
    public boolean hasNext() {
        if (buffer != null) {
            return true;
        }
        else {
            return iterator.hasNext();
        }
    }
    
    @Override
    public T next() {
        if (buffer != null) {
            T toReturn = this.buffer;
            buffer = null;
            return toReturn;
        }
        else {
            return iterator.next();
        }
    }
    
    //get the first element without taking it
    public T peek() {
        assert hasNext();
        if (buffer == null) {
            buffer = iterator.next();
        }
        
        return buffer;
    }
}
