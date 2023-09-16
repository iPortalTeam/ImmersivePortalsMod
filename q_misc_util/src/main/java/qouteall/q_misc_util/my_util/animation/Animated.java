package qouteall.q_misc_util.my_util.animation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Sphere;
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
    private TimingFunction timingFunction;
    
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
    
    /**
     * It can be only called during rendering.
     * No need to call this every time the "underlying value" changes.
     * Calling this method with the same value multiple times will not affect the animation progress.
     */
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
    
    public void setTimingFunction(TimingFunction timingFunction) {
        setTarget(getTarget(), duration);
        this.timingFunction = timingFunction;
    }
    
    public static final TypeInfo<Vec3> VEC3_NULLABLE_TYPE_INFO = new TypeInfo<Vec3>() {
        @Override
        public Vec3 interpolate(Vec3 start, Vec3 end, double progress) {
            return start.lerp(end, progress);
        }
        
        @Override
        public boolean isClose(Vec3 a, Vec3 b) {
            return a.distanceToSqr(b) < 0.01;
        }
        
        // the empty is null
    };
    
    // TODO rename to VEC3_DEFAULT_ZERO_TYPE_INFO
    public static final TypeInfo<Vec3> VEC_3_TYPE_INFO = new TypeInfo<Vec3>() {
        @Override
        public Vec3 interpolate(Vec3 start, Vec3 end, double progress) {
            return start.lerp(end, progress);
        }
        
        @Override
        public boolean isClose(Vec3 a, Vec3 b) {
            return a.distanceToSqr(b) < 0.01;
        }
        
        @Override
        public Vec3 getEmpty() {
            return Vec3.ZERO;
        }
    };
    
    // TODO rename to DOUBLE_DEFAULT_ZERO_TYPE_INFO
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = new TypeInfo<Double>() {
        @Override
        public Double interpolate(Double start, Double end, double progress) {
            if (start == null) {
                start = 0.0;
            }
            
            if (end == null) {
                end = 0.0;
            }
            
            return start + (end - start) * progress;
        }
        
        @Override
        public boolean isClose(Double a, Double b) {
            if (a == null) {
                a = 0.0;
            }
            if (b == null) {
                b = 0.0;
            }
            
            return Math.abs(a - b) < 0.001;
        }
        
        @Override
        public Double getEmpty() {
            return 0.0;
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
            
            return a.plane().value().pos().distanceToSqr(b.plane().value().pos()) < 0.01
                && a.plane().value().normal().distanceToSqr(b.plane().value().normal()) < 0.01
                && Math.abs(a.scale() - b.scale()) < 0.01;
        }
        
        @Override
        public RenderedPlane getEmpty() {
            return RenderedPlane.NONE;
        }
    };
    
    public static final TypeInfo<RenderedSphere> RENDERED_SPHERE_TYPE_INFO = new TypeInfo<RenderedSphere>() {
        @Override
        public RenderedSphere interpolate(RenderedSphere start, RenderedSphere end, double progress) {
            Validate.notNull(start);
            Validate.notNull(end);
            
            if (start.sphere() == null && end.sphere() == null) {
                return start;
            }
            
            double destScale = start.scale() + (end.scale() - start.scale()) * progress;
            
            if (destScale < 0.01) {
                return RenderedSphere.NONE;
            }
            
            if (start.sphere() == null) {
                return new RenderedSphere(
                    end.sphere(),
                    end.orientation(),
                    destScale
                );
            }
            
            if (end.sphere() == null) {
                return new RenderedSphere(
                    start.sphere(),
                    start.orientation(),
                    destScale
                );
            }
            
            if (start.sphere().dimension() != end.sphere().dimension()) {
                return end;
            }
            
            return new RenderedSphere(
                new WithDim<>(
                    start.sphere().dimension(),
                    Sphere.interpolate(start.sphere().value(), end.sphere().value(), progress)
                ),
                DQuaternion.interpolate(start.orientation(), end.orientation(), progress),
                destScale
            );
        }
        
        @Override
        public boolean isClose(RenderedSphere a, RenderedSphere b) {
            if (a.sphere() == null && b.sphere() == null) {
                return true;
            }
            
            if (a.sphere() == null || b.sphere() == null) {
                return false;
            }
            
            if (a.sphere().dimension() != b.sphere().dimension()) {
                return false;
            }
            
            return a.sphere().value().center().distanceToSqr(b.sphere().value().center()) < 0.01
                && Math.abs(a.sphere().value().radius() - b.sphere().value().radius()) < 0.01
                && Math.abs(a.scale() - b.scale()) < 0.01;
        }
        
        @Override
        public RenderedSphere getEmpty() {
            return RenderedSphere.NONE;
        }
    };
    
    public static final TypeInfo<RenderedPoint> RENDERED_POINT_TYPE_INFO = new TypeInfo<RenderedPoint>() {
        @Override
        public RenderedPoint interpolate(RenderedPoint start, RenderedPoint end, double progress) {
            if (start.pos() == null && end.pos() == null) {
                return start;
            }
            
            double destScale = start.scale() + (end.scale() - start.scale()) * progress;
            
            if (destScale < 0.01) {
                return RenderedPoint.EMPTY;
            }
            
            if (start.pos() == null) {
                return new RenderedPoint(
                    end.pos(),
                    destScale
                );
            }
            
            if (end.pos() == null) {
                return new RenderedPoint(
                    start.pos(),
                    destScale
                );
            }
            
            if (start.pos().dimension() != end.pos().dimension()) {
                return end;
            }
            
            return new RenderedPoint(
                new WithDim<>(
                    start.pos().dimension(),
                    start.pos().value().lerp(end.pos().value(), progress)
                ),
                destScale
            );
        }
        
        @Override
        public boolean isClose(RenderedPoint a, RenderedPoint b) {
            if (a.pos() == null && b.pos() == null) {
                return true;
            }
            
            if (a.pos() == null || b.pos() == null) {
                return false;
            }
            
            if (a.pos().dimension() != b.pos().dimension()) {
                return false;
            }
            
            return a.pos().value().distanceToSqr(b.pos().value()) < 0.01
                && Math.abs(a.scale() - b.scale()) < 0.01;
        }
        
        @Override
        public RenderedPoint getEmpty() {
            return RenderedPoint.EMPTY;
        }
    };
    
    public static final TypeInfo<RenderedLineSegment> RENDERED_LINE_SEGMENT_TYPE_INFO = new TypeInfo<RenderedLineSegment>() {
        @Override
        public RenderedLineSegment interpolate(RenderedLineSegment start, RenderedLineSegment end, double progress) {
            if (start.lineSegment() == null && end.lineSegment() == null) {
                return start;
            }
            
            double destScale = start.scale() + (end.scale() - start.scale()) * progress;
            
            if (destScale < 0.01) {
                return RenderedLineSegment.EMPTY;
            }
            
            if (start.lineSegment() == null) {
                return new RenderedLineSegment(
                    end.lineSegment(),
                    destScale
                );
            }
            
            if (end.lineSegment() == null) {
                return new RenderedLineSegment(
                    start.lineSegment(),
                    destScale
                );
            }
            
            if (start.lineSegment().dimension() != end.lineSegment().dimension()) {
                return end;
            }
            
            return new RenderedLineSegment(
                new WithDim<>(
                    start.lineSegment().dimension(),
                    start.lineSegment().value().interpolate(start.lineSegment().value(), end.lineSegment().value(), progress)
                ),
                destScale
            );
            
        }
        
        @Override
        public boolean isClose(RenderedLineSegment a, RenderedLineSegment b) {
            if (a.lineSegment() == null && b.lineSegment() == null) {
                return true;
            }
            
            if (a.lineSegment() == null || b.lineSegment() == null) {
                return false;
            }
            
            if (a.lineSegment().dimension() != b.lineSegment().dimension()) {
                return false;
            }
            
            return a.lineSegment().value().isClose(b.lineSegment().value(), 0.01)
                && Math.abs(a.scale() - b.scale()) < 0.01;
        }
        
        @Override
        public RenderedLineSegment getEmpty() {
            return RenderedLineSegment.EMPTY;
        }
    };
}
