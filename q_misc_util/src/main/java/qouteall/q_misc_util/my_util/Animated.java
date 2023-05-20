package qouteall.q_misc_util.my_util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Animated<T> {
    public final TypeInfo<T> typeInfo;
    public final TimeSupplier timeSupplier;
    
    @Nullable
    public T startValue;
    @Nullable
    public T endValue;
    public long startTime = 0;
    
    public long duration = 0;
    public final TimingFunction timingFunction;
    
    public static interface TypeInfo<T>{
        T interpolate(T start, T end, double progress);
    
        boolean isClose(T a, T b);
    }
    
    public static interface TimeSupplier {
        long getTime();
    }
    
    public static interface TimingFunction {
        double apply(double progress);
    }
    
    public Animated(
        TypeInfo<T> typeInfo,
        TimeSupplier timeSupplier, TimingFunction timingFunction,
        @Nullable T initialValue
    ) {
        this.typeInfo = typeInfo;
        this.timeSupplier = timeSupplier;
        this.timingFunction = timingFunction;
        this.startValue = initialValue;
        this.endValue = initialValue;
    }
    
    public void setTarget(@Nullable T value, long newDuration) {
        if (endValue != null && value != null) {
            if (typeInfo.isClose(endValue, value)) {
                return;
            }
        }
        
        if (value == null) {
            startValue = null;
            endValue = null;
            startTime = 0;
        }
        else {
            long time = timeSupplier.getTime();
            if (startValue == null) {
                startValue = value;
                endValue = value;
                startTime = time;
            }
            else {
                startValue = getCurrent();
                endValue = value;
                startTime = time;
            }
            duration = newDuration;
        }
    }
    
    public void clearTarget() {
        setTarget(null, 0);
    }
    
    @Nullable
    public T getTarget() {
        return endValue;
    }
    
    @Nullable
    public T getCurrent() {
        if (startValue == null || endValue == null) {
            return null;
        }
    
        if (startTime == 0 || duration == 0) {
            return startValue;
        }
        
        double progress = Mth.clamp(
            (timeSupplier.getTime() - startTime) / (double) duration, 0, 1
        );
        
        return typeInfo.interpolate(
            startValue, endValue, timingFunction.apply(progress)
        );
    }
    
    public static final TypeInfo<Vec3> VEC_3_TYPE_INFO = new TypeInfo<Vec3>() {
        @Override
        public Vec3 interpolate(Vec3 start, Vec3 end, double progress) {
            return start.lerp(end, progress);
        }
        
        @Override
        public boolean isClose(Vec3 a, Vec3 b) {
            return a.distanceToSqr(b) < 0.01;
        }
    };
    
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = new TypeInfo<Double>() {
        @Override
        public Double interpolate(Double start, Double end, double progress) {
            return start + (end - start) * progress;
        }
        
        @Override
        public boolean isClose(Double a, Double b) {
            return Math.abs(a - b) < 0.01;
        }
    };
}
