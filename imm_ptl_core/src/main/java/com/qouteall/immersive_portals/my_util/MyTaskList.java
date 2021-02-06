package com.qouteall.immersive_portals.my_util;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.qouteall.immersive_portals.Helper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Iterator;
import java.util.function.BooleanSupplier;

//NOTE if the task returns true, it will be deleted
//if the task returns false, it will be invoked again at next time
public class MyTaskList {
    public interface MyTask {
        public boolean runAndGetIsFinished();
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
                return task.runAndGetIsFinished();
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
    
    public static MyTask oneShotTask(Runnable runnable) {
        return () -> {
            runnable.run();
            return true;
        };
    }
    
    public static MyTask nullTask() {
        return () -> true;
    }
    
    public static MyTask chainTask(MyTask a, MyTask b) {
        return new MyTask() {
            boolean aFinished = false;
            
            @Override
            public boolean runAndGetIsFinished() {
                if (!aFinished) {
                    boolean finished = a.runAndGetIsFinished();
                    if (finished) {
                        aFinished = true;
                    }
                    else {
                        return false;
                    }
                }
                return b.runAndGetIsFinished();
            }
        };
    }
    
    public static MyTask withDelay(int iterations, MyTask task) {
        return new MyTask() {
            private int counter = iterations;
            
            @Override
            public boolean runAndGetIsFinished() {
                if (counter > 0) {
                    counter--;
                    return false;
                }
                else {
                    return task.runAndGetIsFinished();
                }
            }
        };
    }
    
    public static MyTask withCancelCondition(BooleanSupplier shouldCancel, MyTask task) {
        return () -> {
            if (shouldCancel.getAsBoolean()) {
                return true;
            }
            
            return task.runAndGetIsFinished();
        };
    }
    
    public static MyTask withDelayCondition(BooleanSupplier shouldDelay, MyTask task) {
        return () -> {
            if (shouldDelay.getAsBoolean()) {
                return false;
            }
            
            return task.runAndGetIsFinished();
        };
    }
    
    public static MyTask withRetryNumberLimit(int retryNumberLimit, MyTask task, Runnable onLimitReached) {
        return new MyTask() {
            private int retryNumber = 0;
            
            @Override
            public boolean runAndGetIsFinished() {
                boolean finished = task.runAndGetIsFinished();
                if (finished) {
                    return true;
                }
                else {
                    retryNumber += 1;
                    if (retryNumber > retryNumberLimit) {
                        onLimitReached.run();
                        return true;
                    }
                    return false;
                }
            }
        };
    }
    
    public static MyTask withInterval(int interval, MyTask task) {
        return new MyTask() {
            int i = 0;
            
            @Override
            public boolean runAndGetIsFinished() {
                if (i < interval) {
                    i++;
                    return false;
                }
                i = 0;
                return task.runAndGetIsFinished();
            }
        };
    }
    
    public static MyTask withMacroLifecycle(
        Runnable beginAction, Runnable endAction, MyTask task
    ) {
        return new MyTask() {
            boolean began = false;
            
            @Override
            public boolean runAndGetIsFinished() {
                if (!began) {
                    began = true;
                    beginAction.run();
                }
                boolean finished = task.runAndGetIsFinished();
                if (finished) {
                    endAction.run();
                }
                return finished;
            }
        };
    }
    
    public static MyTask withMicroLifecycle(
        Runnable beginAction, Runnable endAction, MyTask task
    ) {
        return () -> {
            beginAction.run();
            boolean finished = task.runAndGetIsFinished();
            endAction.run();
            return finished;
        };
    }
    
    public static MyTask chainTasks(Iterator<MyTask> tasks) {
        PeekingIterator<MyTask> peekingIterator = Iterators.peekingIterator(tasks);
        return () -> {
            if (peekingIterator.hasNext()) {
                MyTask curr = peekingIterator.peek();
                boolean finished = curr.runAndGetIsFinished();
                if (finished) {
                    peekingIterator.next();
                }
            }
            
            return !peekingIterator.hasNext();
        };
    }
}
