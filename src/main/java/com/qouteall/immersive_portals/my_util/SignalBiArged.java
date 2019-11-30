package com.qouteall.immersive_portals.my_util;

import com.qouteall.immersive_portals.Helper;
import org.apache.logging.log4j.util.TriConsumer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class SignalBiArged<A, B> {
    private List<BiConsumer<A, B>> funcList = new ArrayList<>();
    private boolean isEmitting = false;
    
    public void emit(A a, B b) {
        isEmitting = true;
        try {
            funcList.forEach(runnable -> runnable.accept(a, b));
        }
        finally {
            isEmitting = false;
        }
    }
    
    //NOTE the func should not capture owner
    public <T> void connectWithWeakRef(T owner, TriConsumer<T, A, B> func) {
        //NOTE using weak hash map was a mistake
        //https://stackoverflow.com/questions/8051912/will-a-weakhashmaps-entry-be-collected-if-the-value-contains-the-only-strong-re
        
        WeakReference<T> weakRef = new WeakReference<>(owner);
        Helper.SimpleBox<BiConsumer<A, B>> boxOfRunnable = new Helper.SimpleBox<>(null);
        boxOfRunnable.obj = (a, b) -> {
            T currentTarget = weakRef.get();
            if (currentTarget != null) {
                func.accept(currentTarget, a, b);
            }
            else {
                disconnect(boxOfRunnable.obj);
            }
        };
        connect(boxOfRunnable.obj);
    }
    
    public void connect(BiConsumer<A, B> func) {
        copyDataWhenEmitting();
        funcList.add(func);
    }
    
    public void disconnect(BiConsumer<A, B> func) {
        copyDataWhenEmitting();
        boolean removed = funcList.remove(func);
        assert removed;
    }
    
    private void copyDataWhenEmitting() {
        if (isEmitting) {
            funcList = new ArrayList<>(funcList);
        }
    }
}
