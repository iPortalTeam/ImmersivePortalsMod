package qouteall.q_misc_util.my_util;

import net.minecraft.world.phys.Vec3;

public record LineSegment(Vec3 start, Vec3 end) {
    public LineSegment interpolate(LineSegment a, LineSegment b, double progress) {
        return new LineSegment(
            a.start().lerp(b.start(), progress),
            a.end().lerp(b.end(), progress)
        );
    }
    
    public boolean isClose(LineSegment value, double v) {
        return start().distanceToSqr(value.start()) < v * v &&
            end().distanceToSqr(value.end()) < v * v;
    }
}
