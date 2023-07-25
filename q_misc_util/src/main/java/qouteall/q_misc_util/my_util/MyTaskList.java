package qouteall.q_misc_util.my_util;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.slf4j.Logger;
import qouteall.q_misc_util.Helper;

import java.util.Iterator;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

//NOTE if the task returns true, it will be deleted
//if the task returns false, it will be invoked again at next time
public class MyTaskList {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public interface MyTask {
        public boolean runAndGetIsFinished();
        
        public default void onCancelled() {}
    }
    
    private final ObjectList<MyTask> tasks = new ObjectArrayList<>();
    private final ObjectList<MyTask> tasksToAdd = new ObjectArrayList<>();
    
    // this method could be invoked while a task is running
    public synchronized void addTask(MyTask task) {
        tasksToAdd.add(task);
    }
    
    public void addOneShotTask(Runnable runnable) {
        addTask(() -> {
            runnable.run();
            return true;
        });
    }
    
    public synchronized void processTasks() {
        tasks.addAll(tasksToAdd);
        tasksToAdd.clear();
        
        Helper.removeIf(tasks, task -> {
            try {
                return task.runAndGetIsFinished();
            }
            catch (Throwable e) {
                LOGGER.error("Failed to process task {}", task, e);
                return true;
            }
        });
    }
    
    public synchronized void forceClearTasks() {
        for (MyTask task : tasks) {
            task.onCancelled();
        }
        
        for (MyTask task : tasksToAdd) {
            task.onCancelled();
        }
        
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
            
            @Override
            public void onCancelled() {
                if (!aFinished) {
                    a.onCancelled();
                }
                b.onCancelled();
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
            
            @Override
            public void onCancelled() {
                task.onCancelled();
            }
        };
    }
    
    public static MyTask withCancelCondition(BooleanSupplier shouldCancel, MyTask task) {
        return new MyTask() {
            @Override
            public boolean runAndGetIsFinished() {
                if (shouldCancel.getAsBoolean()) {
                    return true;
                }
                
                return task.runAndGetIsFinished();
            }
            
            @Override
            public void onCancelled() {
                task.onCancelled();
            }
        };
    }
    
    public static MyTask withDelayCondition(BooleanSupplier shouldDelay, MyTask task) {
        return new MyTask() {
            @Override
            public boolean runAndGetIsFinished() {
                if (shouldDelay.getAsBoolean()) {
                    return false;
                }
                
                return task.runAndGetIsFinished();
            }
            
            @Override
            public void onCancelled() {
                task.onCancelled();
            }
        };
    }
    
    public static MyTask withTimeDelayedFromNow(double seconds, MyTask task) {
        long startTime = System.nanoTime();
        return withDelayCondition(
            () -> System.nanoTime() - startTime < Helper.secondToNano(seconds),
            task
        );
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
            
            @Override
            public void onCancelled() {
                task.onCancelled();
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
            
            @Override
            public void onCancelled() {
                task.onCancelled();
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
            
            @Override
            public void onCancelled() {
                if (began) {
                    endAction.run();
                }
                task.onCancelled();
            }
        };
    }
    
    public static MyTask withMicroLifecycle(
        Runnable beginAction, Runnable endAction, MyTask task
    ) {
        return new MyTask() {
            @Override
            public boolean runAndGetIsFinished() {
                beginAction.run();
                boolean finished = task.runAndGetIsFinished();
                endAction.run();
                return finished;
            }
            
            @Override
            public void onCancelled() {
                endAction.run();
                task.onCancelled();
            }
        };
    }
    
    // NOTE tasks should be finite, unless it will deadloop during cancellation
    public static MyTask chainTasks(Iterator<MyTask> tasks) {
        PeekingIterator<MyTask> peekingIterator = Iterators.peekingIterator(tasks);
        return new MyTask() {
            @Override
            public boolean runAndGetIsFinished() {
                if (peekingIterator.hasNext()) {
                    MyTask curr = peekingIterator.peek();
                    boolean finished = curr.runAndGetIsFinished();
                    if (finished) {
                        peekingIterator.next();
                    }
                }
                
                return !peekingIterator.hasNext();
            }
            
            @Override
            public void onCancelled() {
                while (peekingIterator.hasNext()) {
                    MyTask curr = peekingIterator.next();
                    curr.onCancelled();
                }
            }
        };
    }
    
    public static MyTask repeat(int number, Supplier<MyTask> taskConstructor) {
        return chainTasks(
            Stream.generate(taskConstructor)
                .limit(number)
                .iterator()
        );
    }
}
