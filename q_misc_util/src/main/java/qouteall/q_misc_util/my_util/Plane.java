package qouteall.q_misc_util.my_util;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class Plane {
    public final Vec3 pos;
    public final Vec3 normal;
    
    public Plane(Vec3 pos, Vec3 normal) {
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
