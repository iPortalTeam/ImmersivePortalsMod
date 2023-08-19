package qouteall.q_misc_util.my_util;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Plane(Vec3 pos, Vec3 normal) {
    public Plane(Vec3 pos, Vec3 normal) {
        this.pos = pos;
        this.normal = normal.normalize();
    }
    
    public double getDistanceTo(Vec3 point) {
        return normal.dot(point.subtract(pos));
    }
    
    public double getDistanceTo(double x, double y, double z) {
        return normal.x() * (x - pos.x()) + normal.y() * (y - pos.y()) + normal.z() * (z - pos.z());
    }
    
    @NotNull
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
    
    public Plane getOpposite() {
        return new Plane(pos, normal.scale(-1));
    }
    
    public @Nullable Vec3 rayTrace(Vec3 origin, Vec3 vec) {
        double t = rayTraceGetT(origin, vec);
        
        if (Double.isNaN(t)) {
            return null;
        }
        
        if (t < 0) {
            return null;
        }
        
        return origin.add(vec.scale(t));
    }
    
    /**
     * @param lineOrigin the origin of the line
     * @param lineVec    the direction of the line (not necessarily normalized)
     * @return the t value of the line equation (lineOrigin + lineVec * t) that intersects with the plane
     * or NaN if the line is parallel to the plane
     */
    public double rayTraceGetT(Vec3 lineOrigin, Vec3 lineVec) {
        double lineVecProjectToNormal = normal.dot(lineVec);
        if (Math.abs(lineVecProjectToNormal) < 0.00001) {
            return Double.NaN;
        }
        return -getDistanceTo(lineOrigin) / lineVecProjectToNormal;
    }
    
    /**
     * Same as the above but avoids allocation (Waiting for Project Valhalla)
     */
    public double rayTraceGetT(
        double lineOriginX, double lineOriginY, double lineOriginZ,
        double lineVecX, double lineVecY, double lineVecZ
    ) {
        double lineVecProjectToNormal =
            normal.x() * lineVecX + normal.y() * lineVecY + normal.z() * lineVecZ;
        if (Math.abs(lineVecProjectToNormal) < 0.00001) {
            return Double.NaN;
        }
        return -getDistanceTo(lineOriginX, lineOriginY, lineOriginZ) / lineVecProjectToNormal;
    }
    
    public @Nullable Vec3 intersectionWithLineSegment(Vec3 lineP1, Vec3 lineP2) {
        Vec3 lineVec = lineP2.subtract(lineP1);
        double t = rayTraceGetT(lineP1, lineVec);
        
        if (Double.isNaN(t)) {
            return null;
        }
        
        if (t < 0 || t > 1) {
            return null;
        }
        
        return lineP1.add(lineVec.scale(t));
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
    public String toString() {
        return "Plane{" +
            "pos=" + pos +
            ", normal=" + normal +
            '}';
    }
    
    public static Plane interpolate(Plane a, Plane b, double progress) {
        Vec3 pos = a.pos.lerp(b.pos, progress);
        Vec3 normal = a.normal.lerp(b.normal, progress);
        return new Plane(pos, normal);
    }
    
    public Plane getParallelPlane(Vec3 pos) {
        return new Plane(pos, normal);
    }
}
