package qouteall.q_misc_util.my_util;

import qouteall.q_misc_util.Helper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Similar to event, connect means subscribe, emit means posting event
public class Signal {
    private List<Runnable> funcList = new ArrayList<>();
    private boolean isEmitting = false;
    
    public synchronized void emit() {
        isEmitting = true;
        try {
            funcList.forEach(runnable -> runnable.run());
        }
        finally {
            isEmitting = false;
        }
    }
    
    //NOTE the func should not capture owner
    public synchronized  <T> void connectWithWeakRef(T owner, Consumer<T> func) {
        //NOTE using weak hash map was a mistake
        //https://stackoverflow.com/questions/8051912/will-a-weakhashmaps-entry-be-collected-if-the-value-contains-the-only-strong-re
        
        WeakReference<T> weakRef = new WeakReference<>(owner);
        Helper.SimpleBox<Runnable> boxOfRunnable = new Helper.SimpleBox<>(null);
        boxOfRunnable.obj = () -> {
            T currentTarget = weakRef.get();
            if (currentTarget != null) {
                func.accept(currentTarget);
            }
            else {
                disconnect(boxOfRunnable.obj);
            }
        };
        connect(boxOfRunnable.obj);
    }
    
    public synchronized void connect(Runnable func) {
        copyDataWhenEmitting();
        funcList.add(func);
    }
    
    public synchronized void disconnect(Runnable func) {
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
