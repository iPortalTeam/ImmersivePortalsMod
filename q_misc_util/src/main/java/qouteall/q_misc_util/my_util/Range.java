package qouteall.q_misc_util.my_util;

import javax.annotation.Nullable;

public record Range(double start, double end) {
    
    public static Range createUnordered(double p1, double p2) {
        return new Range(Math.min(p1, p2), Math.max(p1, p2));
    }
    
    @Nullable
    public static Range intersection(Range r1, Range r2) {
        Range range = new Range(
            Math.max(r1.start, r2.start),
            Math.min(r1.end, r2.end)
        );
        if (range.start >= range.end) {
            return null;
        }
        return range;
    }
    
    @Nullable
    public Range intersect(Range another) {
        return intersection(this, another);
    }
}
