package qouteall.imm_ptl.core;

import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.metrics.MetricCategory;

import java.util.function.Supplier;

public final class ProfilerCompat {
    private static final ProfilerFiller NOOP = new ProfilerFiller() {
        @Override
        public void startTick() {
        }

        @Override
        public void endTick() {
        }

        @Override
        public void push(String name) {
        }

        @Override
        public void push(Supplier<String> nameSupplier) {
        }

        @Override
        public void pop() {
        }

        @Override
        public void popPush(String name) {
        }

        @Override
        public void popPush(Supplier<String> nameSupplier) {
        }

        @Override
        public void markForCharting(MetricCategory metricCategory) {
        }

        @Override
        public void incrementCounter(String name, int amount) {
        }

        @Override
        public void incrementCounter(Supplier<String> nameSupplier, int amount) {
        }
    };

    private ProfilerCompat() {
    }

    public static ProfilerFiller getProfiler() {
        return Profiler.get();
    }
    
    public static void push(String name) {
        Profiler.get().push(name);
    }
    
    public static void pop() {
        Profiler.get().pop();
    }
    
    public static void popPush(String name) {
        Profiler.get().popPush(name);
    }
}
