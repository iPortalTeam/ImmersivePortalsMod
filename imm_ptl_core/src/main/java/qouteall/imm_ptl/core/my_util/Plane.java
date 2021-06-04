package qouteall.imm_ptl.core.my_util;

import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class Plane {
    public final Vec3d pos;
    public final Vec3d normal;
    
    public Plane(Vec3d pos, Vec3d normal) {
        this.pos = pos;
        this.normal = normal;
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
