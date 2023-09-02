package qouteall.q_misc_util.my_util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class GeometryUtil {
    
    public static double getAngle(
        double dx1, double dy1, double dx2, double dy2
    ) {
        double l1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double l2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
        
        if (l1 == 0 || l2 == 0) {
            return 0;
        }
        
        double dot = (dx1 / l1) * (dx2 / l2) + (dy1 / l1) * (dy2 / l2);
        
        return Math.acos(dot);
    }
    
    public static boolean isOppositeVec(double v1x, double v1y, double v2x, double v2y) {
        double d1l = Math.sqrt(v1x * v1x + v1y * v1y);
        double d2l = Math.sqrt(v2x * v2x + v2y * v2y);
        
        if (d1l == 0 || d2l == 0) {
            return true;
        }
        
        double dot = (v1x / d1l) * (v2x / d2l) + (v1y / d1l) * (v2y / d2l);
        
        return Math.abs(dot - (-1)) < 0.00001;
    }
    
    /**
     * Uses the Separating Axis Theorem (SAT) to determine whether two triangles intersect.
     */
    public static boolean triangleIntersects(
        double t0x0, double t0y0, double t0x1, double t0y1, double t0x2, double t0y2,
        double t1x0, double t1y0, double t1x1, double t1y1, double t1x2, double t1y2
    ) {
        BiDoublePredicate separatesByAxis = (axisX, axisY) ->
            triangleAxisSeparatesAlongAxis(
                t0x0, t0y0, t0x1, t0y1, t0x2, t0y2,
                t1x0, t1y0, t1x1, t1y1, t1x2, t1y2,
                axisX, axisY
            );
        
        double vec00x = t0x1 - t0x0;
        double vec00y = t0y1 - t0y0;
        double vec01x = t0x2 - t0x1;
        double vec01y = t0y2 - t0y1;
        double vec02x = t0x0 - t0x2;
        double vec02y = t0y0 - t0y2;
        
        double vec10x = t1x1 - t1x0;
        double vec10y = t1y1 - t1y0;
        double vec11x = t1x2 - t1x1;
        double vec11y = t1y2 - t1y1;
        double vec12x = t1x0 - t1x2;
        double vec12y = t1y0 - t1y2;
        
        double axis00x = -vec00y;
        double axis00y = vec00x;
        double axis01x = -vec01y;
        double axis01y = vec01x;
        double axis02x = -vec02y;
        double axis02y = vec02x;
        
        double axis10x = -vec10y;
        double axis10y = vec10x;
        double axis11x = -vec11y;
        double axis11y = vec11x;
        double axis12x = -vec12y;
        double axis12y = vec12x;
        
        boolean canSeparate = separatesByAxis.test(axis00x, axis00y) ||
            separatesByAxis.test(axis01x, axis01y) ||
            separatesByAxis.test(axis02x, axis02y) ||
            separatesByAxis.test(axis10x, axis10y) ||
            separatesByAxis.test(axis11x, axis11y) ||
            separatesByAxis.test(axis12x, axis12y);
        
        return !canSeparate;
    }
    
    private static boolean triangleAxisSeparatesAlongAxis(
        double t0x0, double t0y0, double t0x1, double t0y1, double t0x2, double t0y2,
        double t1x0, double t1y0, double t1x1, double t1y1, double t1x2, double t1y2,
        double axisX, double axisY
    ) {
        double t0p0 = t0x0 * axisX + t0y0 * axisY;
        double t0p1 = t0x1 * axisX + t0y1 * axisY;
        double t0p2 = t0x2 * axisX + t0y2 * axisY;
        
        double t1p0 = t1x0 * axisX + t1y0 * axisY;
        double t1p1 = t1x1 * axisX + t1y1 * axisY;
        double t1p2 = t1x2 * axisX + t1y2 * axisY;
        
        double t0min = Math.min(Math.min(t0p0, t0p1), t0p2);
        double t0max = Math.max(Math.max(t0p0, t0p1), t0p2);
        
        double t1min = Math.min(Math.min(t1p0, t1p1), t1p2);
        double t1max = Math.max(Math.max(t1p0, t1p1), t1p2);
        
        return t0max - t1min < 0 || t1max - t0min < 0;
    }
    
    /**
     * Uses the Separating Axis Theorem (SAT) to determine whether a triangle intersects with an AABB.
     * The triangle must be counter-clockwise.
     */
    public static boolean triangleIntersectsWithAABB(
        double t0x0, double t0y0, double t0x1, double t0y1, double t0x2, double t0y2,
        double minX, double minY, double maxX, double maxY
    ) {
        // firstly test the AABB's 4 edges
        
        double tMinX = Math.min(Math.min(t0x0, t0x1), t0x2);
        double tMaxX = Math.max(Math.max(t0x0, t0x1), t0x2);
        double tMinY = Math.min(Math.min(t0y0, t0y1), t0y2);
        double tMaxY = Math.max(Math.max(t0y0, t0y1), t0y2);
        
        if (tMaxX < minX || tMinX > maxX || tMaxY < minY || tMinY > maxY) {
            return false;
        }
        
        // then test the triangle's 3 edges
        
        return !isAABBOnLeftSideOfLine(
            minX, minY, maxX, maxY, t0x0, t0y0, t0x1 - t0x0, t0y1 - t0y0
        ) && !isAABBOnLeftSideOfLine(
            minX, minY, maxX, maxY, t0x1, t0y1, t0x2 - t0x1, t0y2 - t0y1
        ) && !isAABBOnLeftSideOfLine(
            minX, minY, maxX, maxY, t0x2, t0y2, t0x0 - t0x2, t0y0 - t0y2
        );
    }
    
    private static boolean isAABBOnLeftSideOfLine(
        double minX, double minY, double maxX, double maxY,
        double originX, double originY, double lineVecX, double lineVecY
    ) {
        // rotate the line vec by 90 degrees counter-clockwise
        // if the triangle is counter-clockwise, the facing vec is pointing inwards
        double facingVecX = -lineVecY;
        double facingVecY = lineVecX;
        
        double testingPointX = facingVecX > 0 ? minX : maxX;
        double testingPointY = facingVecY > 0 ? minY : maxY;
        
        double dx = testingPointX - originX;
        double dy = testingPointY - originY;
        
        double dot = dx * facingVecX + dy * facingVecY;
        
        return dot > 0;
    }
    
    @FunctionalInterface
    public static interface BiDoublePredicate {
        boolean test(double x, double y);
    }
    
    public static record Line2D(double linePX, double linePY, double dirX, double dirY) {
        
        public static Line2D fromTwoPoints(
            double x1, double y1,
            double x2, double y2
        ) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            return new Line2D(x1, y1, dx, dy);
        }
        
        // the side vec is the direction vec rotated by 90 degrees counter-clockwise
        // for a counter-clockwise triangle, the side vec is pointing inwards
        public double getSideVecX() {
            return -dirY;
        }
        
        public double getSideVecY() {
            return dirX;
        }
        
        public int testSide(double x, double y) {
            double dx = x - linePX;
            double dy = y - linePY;
            double dot = dx * getSideVecX() + dy * getSideVecY();
            if (dot > 0.00001) {
                return 1;
            }
            else if (dot < -0.00001) {
                return -1;
            }
            else {
                return 0;
            }
        }
        
        public boolean testSideBool(double x, double y) {
            double dx = x - linePX;
            double dy = y - linePY;
            double dot = dx * getSideVecX() + dy * getSideVecY();
            return dot > 0;
        }
        
        /**
         * 1) px = p10x + t1 * v1x
         * py = p10y + t1 * v1y
         * 2) px = p20x + t2 * v2x
         * py = p20y + t2 * v2y
         * p1x + t1 * v1x = p2x + t2 * v2x
         * p1y + t1 * v1y = p2y + t2 * v2y
         * <p>
         * t1 * v1x - t2 * v2x = p2x - p1x
         * t1 * v1y - t2 * v2y = p2y - p1y
         * <p>
         * t1 * v1x * v2y - t2 * v2x * v2y = (p2x - p1x) * v2y
         * t1 * v1y * v2x - t2 * v2y * v2x = (p2y - p1y) * v2x
         * <p>
         * t1 = ( (p2x - p1x) * v2y - (p2y - p1y) * v2x ) / ( v1x * v2y - v1y * v2x )
         *
         * @return the t value of this line. If the line is parallel to the other line, return NaN.
         */
        public double getIntersectionWithLine(Line2D other) {
            double det = dirX * other.dirY - dirY * other.dirX;
            
            if (Math.abs(det) < 1e-8) {
                return Double.NaN;
            }
            
            double t = ((other.linePX - linePX) * other.dirY -
                (other.linePY - linePY) * other.dirX) / det;
            
            return t;
        }
        
        // if not in projection, will return NaN
        public double getDistanceToLineIfWithinProjection(double x, double y) {
            double vx = x - linePX;
            double vy = y - linePY;
            
            double dotWithLineVec = vx * dirX + vy * dirY;
            double dotWithSideVec = vx * getSideVecX() + vy * getSideVecY();
            
            double lineLen = Math.sqrt(dirX * dirX + dirY * dirY); // side vec has the same length
            
            double lenAlongLineVec = dotWithLineVec / lineLen;
            double lenAlongSideVec = dotWithSideVec / lineLen;
            
            if (lenAlongLineVec < 0 || lenAlongLineVec > lineLen) {
                return Double.NaN;
            }
            
            return Math.abs(lenAlongSideVec);
        }
        
    }
    
    public static Vec3 selectCoordFromAABB(
        AABB box, boolean xPositive, boolean yPositive, boolean zPositive
    ) {
        return new Vec3(
            xPositive ? box.maxX : box.minX,
            yPositive ? box.maxY : box.minY,
            zPositive ? box.maxZ : box.minZ
        );
    }
    
    /**
     * @param box    the AABB
     * @param plane  the plane
     * @param planeX the X axis of the plane
     * @param planeY the Y axis of the plane
     * @return the intersection polygon.
     */
    public static ObjectArrayList<Vec2d> getSlicePolygonOfCube(
        AABB box, Plane plane, Vec3 planeX, Vec3 planeY, double scaleX, double scaleY
    ) {
        Vec3 planeNormal = plane.normal();
        
        // the 12 edges of the cube, each edge has 2 vertices, each vertex is 3 values
        double[] boxEdges = new double[]{
            // edges along X axis (YZ: 00, 01, 10, 11)
            box.minX, box.minY, box.minZ,
            box.maxX, box.minY, box.minZ,
            box.minX, box.minY, box.maxZ,
            box.maxX, box.minY, box.maxZ,
            box.minX, box.maxY, box.minZ,
            box.maxX, box.maxY, box.minZ,
            box.minX, box.maxY, box.maxZ,
            box.maxX, box.maxY, box.maxZ,
            
            // edges along Y axis (XZ: 00, 01, 10, 11)
            box.minX, box.minY, box.minZ,
            box.minX, box.maxY, box.minZ,
            box.minX, box.minY, box.maxZ,
            box.minX, box.maxY, box.maxZ,
            box.maxX, box.minY, box.minZ,
            box.maxX, box.maxY, box.minZ,
            box.maxX, box.minY, box.maxZ,
            box.maxX, box.maxY, box.maxZ,
            
            // edges along Z axis (XY: 00, 01, 10, 11)
            box.minX, box.minY, box.minZ,
            box.minX, box.minY, box.maxZ,
            box.minX, box.maxY, box.minZ,
            box.minX, box.maxY, box.maxZ,
            box.maxX, box.minY, box.minZ,
            box.maxX, box.minY, box.maxZ,
            box.maxX, box.maxY, box.minZ,
            box.maxX, box.maxY, box.maxZ,
        };
        
        ObjectArrayList<Vec2d> result = new ObjectArrayList<>();
        
        double xSum = 0;
        double ySum = 0;
        
        for (int i = 0; i < 12; i++) {
            int ei = i * 6;
            
            double p0x = boxEdges[ei + 0]; double p0y = boxEdges[ei + 1]; double p0z = boxEdges[ei + 2];
            double p1x = boxEdges[ei + 3]; double p1y = boxEdges[ei + 4]; double p1z = boxEdges[ei + 5];
            double vx = p1x - p0x; double vy = p1y - p0y; double vz = p1z - p0z;
            
            double t = plane.rayTraceGetT(p0x, p0y, p0z, vx, vy, vz);
            
            if (Double.isNaN(t) || t < 0 || t > 1) {
                continue;
            }
            
            double x = p0x + vx * t; double y = p0y + vy * t; double z = p0z + vz * t;
            
            double dx = x - plane.pos().x(); double dy = y - plane.pos().y(); double dz = z - plane.pos().z();
            
            double localX = (planeX.x() * dx + planeX.y() * dy + planeX.z() * dz) / scaleX;
            double localY = (planeY.x() * dx + planeY.y() * dy + planeY.z() * dz) / scaleY;
            
            result.add(new Vec2d(localX, localY));
            xSum += localX;
            ySum += localY;
        }
        
        if (result.isEmpty()) {
            return result;
        }
        
        double avgX = xSum / result.size();
        double avgY = ySum / result.size();
        
        // sort them by atan2 angle from the center
        result.sort(Comparator.comparingDouble(vec -> Math.atan2(vec.y() - avgY, vec.x() - avgX)));
        
        return result;
    }
}
