package qouteall.imm_ptl.core;

import net.minecraft.core.Direction;

public class DirectionHelper {
    public static Direction nearestDirection(double x, double y, double z) {
        Direction best = Direction.NORTH;
        double bestDot = -Double.MAX_VALUE;

        for (Direction d : Direction.values()) {
            int dx = d.getStepX();
            int dy = d.getStepY();
            int dz = d.getStepZ();
            double dot = x * dx + y * dy + z * dz;
            if (dot > bestDot) {
                bestDot = dot;
                best = d;
            }
        }
        return best;
    }
}
