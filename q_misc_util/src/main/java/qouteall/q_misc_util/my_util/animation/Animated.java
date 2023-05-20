package qouteall.q_misc_util.my_util.animation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;

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
    
    public static interface TypeInfo<T> {
        T interpolate(T start, T end, double progress);
        
        boolean isClose(T a, T b);
        
        default T getEmpty() {
            return null;
        }
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
            // don't reset animation when the target does not change
            if (typeInfo.isClose(endValue, value)) {
                endValue = value;
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
        setTarget(typeInfo.getEmpty(), duration);
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
        
        if (startTime == 0) {
            return startValue;
        }
        
        if (duration == 0) {
            return endValue;
        }
        
        double progress = Mth.clamp(
            (timeSupplier.getTime() - startTime) / (double) duration, 0, 1
        );
        
        if (progress >= 0.999) {
            return endValue;
        }
        
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
    
    public static final TypeInfo<DQuaternion> QUATERNION_TYPE_INFO = new TypeInfo<DQuaternion>() {
        @Override
        public DQuaternion interpolate(DQuaternion start, DQuaternion end, double progress) {
            return DQuaternion.interpolate(start, end, progress);
        }
        
        @Override
        public boolean isClose(DQuaternion a, DQuaternion b) {
            return DQuaternion.isClose(a, b, 0.01);
        }
    };
    
    public static final TypeInfo<RenderedPlane> RENDERED_PLANE_TYPE_INFO = new TypeInfo<RenderedPlane>() {
        @Override
        public RenderedPlane interpolate(RenderedPlane start, RenderedPlane end, double progress) {
            // it needs to handle these cases:
            // expanding a plane
            // closing a plane
            // changing the plane (same dimension)
            // changing the plane (not the same dimension)
            
            Validate.notNull(start);
            Validate.notNull(end);
            
            if (start.plane() == null && end.plane() == null) {
                return start;
            }
            
            double destScale = start.scale() + (end.scale() - start.scale()) * progress;
            
            if (destScale < 0.01) {
                return RenderedPlane.NONE;
            }
            
            if (start.plane() == null) {
                return new RenderedPlane(
                    end.plane(),
                    destScale
                );
            }
            
            if (end.plane() == null) {
                return new RenderedPlane(
                    start.plane(),
                    destScale
                );
            }
            
            if (start.plane().dimension() != end.plane().dimension()) {
                return end;
            }
            
            return new RenderedPlane(
                new WithDim<>(
                    start.plane().dimension(),
                    Plane.interpolate(start.plane().value(), end.plane().value(), progress)
                ),
                destScale
            );
        }
        
        @Override
        public boolean isClose(RenderedPlane a, RenderedPlane b) {
            if (a.plane() == null && b.plane() == null) {
                return true;
            }
            
            if (a.plane() == null || b.plane() == null) {
                return false;
            }
            
            if (a.plane().dimension() != b.plane().dimension()) {
                return false;
            }
            
            return a.plane().value().pos.distanceToSqr(b.plane().value().pos) < 0.01
                && a.plane().value().normal.distanceToSqr(b.plane().value().normal) < 0.01
                && Math.abs(a.scale() - b.scale()) < 0.01;
        }
        
        @Override
        public RenderedPlane getEmpty() {
            return RenderedPlane.NONE;
        }
    };
    
    public static final TypeInfo<RenderedRect> RENDERED_RECT_TYPE_INFO = new TypeInfo<RenderedRect>() {
        @Override
        public RenderedRect interpolate(RenderedRect start, RenderedRect end, double progress) {
            if (start.dimension() != end.dimension()) {
                return end;
            }
            
            start = start.turnToClosestTo(end.orientation());
            
            return new RenderedRect(
                start.dimension(),
                start.center().lerp(end.center(), progress),
                DQuaternion.interpolate(start.orientation(), end.orientation(), progress),
                Mth.lerp(progress, start.width(), end.width()),
                Mth.lerp(progress, start.height(), end.height())
            );
        }
        
        @Override
        public boolean isClose(RenderedRect a, RenderedRect b) {
            return a.dimension() == b.dimension() &&
                a.center().distanceToSqr(b.center()) < 0.01 &&
                DQuaternion.isClose(a.orientation(), b.orientation(), 0.01) &&
                Math.abs(a.width() - b.width()) < 0.01 &&
                Math.abs(a.height() - b.height()) < 0.01;
        }
    };
}
