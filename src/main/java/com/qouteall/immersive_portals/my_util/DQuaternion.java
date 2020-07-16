package com.qouteall.immersive_portals.my_util;


import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

/**
 * Quaternion but in double and immutable
 * Minecraft's quaternion {@link Quaternion} uses float and has precision issues
 * Going through a flipping portal looking downward can rotate the view a little bit using float quaternion
 */
public class DQuaternion {
    public final double x;
    public final double y;
    public final double z;
    public final double w;
    
    public DQuaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
    
    public static DQuaternion fromMcQuaternion(Quaternion quaternion) {
        return new DQuaternion(
            quaternion.getX(), quaternion.getY(), quaternion.getZ(), quaternion.getW()
        );
    }
    
    public Vec3d getRotatingAxis() {
        return new Vec3d(x, y, z);
    }
    
    public Quaternion toMcQuaternion() {
        return new Quaternion(
            (float) x, (float) y, (float) z, (float) w
        );
    }
    
    public static DQuaternion rotationByDegrees(
        Vec3d rotatingAxis,
        double degrees
    ) {
        return rotationByRadians(
            rotatingAxis, Math.toRadians(degrees)
        );
    }
    
    public static DQuaternion rotationByRadians(
        Vec3d axis,
        double rotationAngle
    ) {
        double s = Math.sin(rotationAngle / 2.0F);
        return new DQuaternion(
            axis.getX() * s,
            axis.getY() * s,
            axis.getZ() * s,
            Math.cos(rotationAngle / 2.0F)
        );
    }
    
    public Vec3d rotate(Vec3d vec) {
        DQuaternion result = this.hamiltonProduct(new DQuaternion(vec.x, vec.y, vec.z, 0))
            .hamiltonProduct(getConjugated());
        
        return result.getRotatingAxis();
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public double getW() {
        return w;
    }
    
    public DQuaternion hamiltonProduct(DQuaternion other) {
        double x1 = this.getX();
        double y1 = this.getY();
        double z1 = this.getZ();
        double w1 = this.getW();
        double x2 = other.getX();
        double y2 = other.getY();
        double z2 = other.getZ();
        double w2 = other.getW();
        return new DQuaternion(
            w1 * x2 + x1 * w2 + y1 * z2 - z1 * y2,
            w1 * y2 - x1 * z2 + y1 * w2 + z1 * x2,
            w1 * z2 + x1 * y2 - y1 * x2 + z1 * w2,
            w1 * w2 - x1 * x2 - y1 * y2 - z1 * z2
        );
    }
    
    public DQuaternion getConjugated() {
        return new DQuaternion(
            -x, -y, -z, w
        );
    }
    
    public DQuaternion multiply(double val) {
        return new DQuaternion(
            x * val, y * val, z * val, w * val
        );
    }
    
    public DQuaternion add(DQuaternion q) {
        return new DQuaternion(
            x + q.x, y + q.y, z + q.z, w + q.w
        );
    }
    
    public double dotProduct(DQuaternion q) {
        return getX() * q.getX() +
            getY() * q.getY() +
            getZ() * q.getZ() +
            getW() * q.getW();
    }
    
    public DQuaternion getNormalized() {
        double lenSq = dotProduct(this);
        if (lenSq != 0) {
            // no fastInverseSqrt. precision is the most important
            double len = Math.sqrt(lenSq);
            return this.multiply(1.0 / len);
        }
        else {
            return new DQuaternion(0, 0, 0, 0);
        }
    }
    
    public static DQuaternion getCameraRotation(double pitch, double yaw) {
        DQuaternion r1 = rotationByDegrees(new Vec3d(1, 0, 0), pitch);
        DQuaternion r2 = rotationByDegrees(new Vec3d(0, 1, 0), yaw + 180);
        DQuaternion result = r1.hamiltonProduct(r2);
        return result;
    }
    
    // should be the same as the above
    public static DQuaternion getCameraRotation1(double pitch, double yaw) {
        double p = Math.toRadians(pitch) / 2;
        double y = Math.toRadians(yaw) / 2;
        return new DQuaternion(
            -Math.sin(p) * Math.sin(y),
            Math.cos(p) * Math.cos(y),
            Math.sin(p) * Math.cos(y),
            -Math.cos(p) * Math.sin(y)
        );
    }
    
    public static boolean isClose(DQuaternion a, DQuaternion b, double valve) {
        if (a.getW() * b.getW() < 0) {
            a = a.multiply(-1);
        }
        double da = a.getX() - b.getX();
        double db = a.getY() - b.getY();
        double dc = a.getZ() - b.getZ();
        double dd = a.getW() - b.getW();
        return da * da + db * db + dc * dc + dd * dd < valve;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DQuaternion that = (DQuaternion) o;
        return Double.compare(that.x, x) == 0 &&
            Double.compare(that.y, y) == 0 &&
            Double.compare(that.z, z) == 0 &&
            Double.compare(that.w, w) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }
    
    @Override
    public String toString() {
        return String.format("DQuaternion{x=%s, y=%s, z=%s, w=%s}", x, y, z, w);
    }
    
    //a quaternion is a 4d vector on 4d sphere
    public static DQuaternion interpolate(
        DQuaternion a,
        DQuaternion b,
        double t
    ) {
        
        double dot = a.dotProduct(b);
        
        if (dot < 0.0f) {
            a = a.multiply(-1);
            dot = -dot;
        }
        
        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.
            
            return a.multiply(1 - t).add(b.multiply(t)).getNormalized();
        }
        
        double theta_0 = Math.acos(dot);
        double theta = theta_0 * t;
        double sin_theta = Math.sin(theta);
        double sin_theta_0 = Math.sin(theta_0);
        
        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;
        double s1 = sin_theta / sin_theta_0;
        
        return a.multiply(s0).add(b.multiply(s1));
    }
    
    /**
     * {@link DQuaternion#getCameraRotation1(double, double)}
     */
    public static Pair<Double, Double> getPitchYawFromRotation(DQuaternion quaternion) {
        double x = quaternion.getX();
        double y = quaternion.getY();
        double z = quaternion.getZ();
        double w = quaternion.getW();
        
        double cosYaw = 2 * (y * y + z * z) - 1;
        double sinYaw = -(x * z + y * w) * 2;
        
        double cosPitch = 1 - 2 * (x * x + z * z);
        double sinPitch = (x * w + y * z) * 2;
        
        return new Pair<>(
            Math.toDegrees(Math.atan2(sinPitch, cosPitch)),
            Math.toDegrees(Math.atan2(sinYaw, cosYaw))
        );
    }
}
