package qouteall.q_misc_util.my_util;

import org.jetbrains.annotations.Nullable;

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
    
    public static boolean rangeIntersects(
        double r1Start, double r1End,
        double r2Start, double r2End
    ) {
        return Math.max(r1Start, r2Start) <= Math.min(r1End, r2End);
    }
    
    // push a range out of another range
    public static double getPushRangeMovement(
        double rangeToPushStart, double rangeToPushEnd,
        double colliderStart, double colliderEnd
    ) {
        // if two ranges does not overlap, return 0
        if (rangeToPushStart >= colliderEnd) {
            return 0;
        }
        if (rangeToPushEnd <= colliderStart) {
            return 0;
        }
        
        double toPushMid = (rangeToPushStart + rangeToPushEnd) / 2;
        double colliderMid = (colliderStart + colliderEnd) / 2;
        
        if (toPushMid > colliderMid) {
            // push to right
            return colliderEnd - rangeToPushStart;
        }
        else {
            // push to left
            return colliderStart - rangeToPushEnd;
        }
    }
    
    // confine a range to be inside another range
    public static double getConfineRangeMovement(
        double rangeToConfineStart, double rangeToConfineEnd,
        double barrierStart, double barrierEnd
    ) {
        if (rangeToConfineStart < barrierStart) {
            return barrierStart - rangeToConfineStart;
        }
        
        if (rangeToConfineEnd > barrierEnd) {
            return barrierEnd - rangeToConfineEnd;
        }
        
        return 0;
    }
}
