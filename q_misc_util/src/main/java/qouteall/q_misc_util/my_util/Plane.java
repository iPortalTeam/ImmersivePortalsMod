package qouteall.q_misc_util.my_util;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Objects;

public class Plane {
    public final Vec3 pos;
    public final Vec3 normal;
    
    public Plane(Vec3 pos, Vec3 normal) {
        this.pos = pos;
        this.normal = normal;
    }
    
    public double getDistanceTo(Vec3 point) {
        return normal.dot(point.subtract(pos));
    }
    
    public Vec3 getProjection(Vec3 point) {
        return point.subtract(normal.scale(getDistanceTo(point)));
    }
    
    public Vec3 getReflection(Vec3 point) {
        return point.subtract(normal.scale(2 * getDistanceTo(point)));
    }
    
    public boolean isPointOnPositiveSide(Vec3 point) {
        return getDistanceTo(point) > 0;
    }
    
    public Plane move(double distance) {
        return new Plane(pos.add(normal.scale(distance)), normal);
    }
    
    @Nullable
    public Vec3 raytrace(Vec3 origin, Vec3 vec) {
        double denominator = normal.dot(vec);
        if (Math.abs(denominator) < 0.0001) {
            return null;
        }
        double t = getDistanceTo(origin) / denominator;
        if (t <= 0) {
            return null;
        }
        return origin.add(vec.scale(t));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plane plane = (Plane) o;
        return pos.equals(plane.pos) &&
            normal.equals(plane.normal);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pos, normal);
    }
    
    @Override
    public String toString() {
        return "Plane{" +
            "pos=" + pos +
            ", normal=" + normal +
            '}';
    }
}
