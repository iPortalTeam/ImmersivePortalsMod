package qouteall.q_misc_util.my_util;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

public class Mesh2D {
    
    public static final int gridCountForOneSide = 1 << 30;
    
    public static long encodeToGrid(double x, double y) {
        int gridX = (int) Math.round((x * gridCountForOneSide));
        int gridY = (int) Math.round((y * gridCountForOneSide));
        
        int gridXClamped = Mth.clamp(gridX, -gridCountForOneSide, gridCountForOneSide);
        int gridYClamped = Mth.clamp(gridY, -gridCountForOneSide, gridCountForOneSide);
        
        // NOTE: don't directly convert int to long as it will fill the high bits with 1 for negative numbers
        return ((gridXClamped & 0xFFFFFFFFL) << 32) | (gridYClamped & 0xFFFFFFFFL);
    }
    
    // encoded grid coord to index in point list
    public final Long2IntOpenHashMap gridToPointIndex = new Long2IntOpenHashMap();
    
    // each 2 numbers represent a point
    public final DoubleArrayList pointCoords = new DoubleArrayList();
    
    // each 3 numbers represent a triangle. -1 means invalid triangle
    public final IntArrayList trianglePointIndexes = new IntArrayList();
    
    // the index represents points, the value is a list of triangle ids
    // the point is considered unused when the list is empty
    public final ObjectArrayList<IntArrayList> pointToTriangles = new ObjectArrayList<>();
    
    // when null, the quad tree is not being maintained
    // when not null, the quad tree is being maintained to keep consistent with the mesh
    public @Nullable QuadTree<IntArrayList> triangleLookup;
    
    public Mesh2D() {}
    
    public int addTriangle(
        double x1, double y1,
        double x2, double y2,
        double x3, double y3
    ) {
        int pointIndex1 = indexPoint(x1, y1);
        int pointIndex2 = indexPoint(x2, y2);
        int pointIndex3 = indexPoint(x3, y3);
        
        return addTriangle(pointIndex1, pointIndex2, pointIndex3);
    }
    
    public int addTriangle(int pointIndex1, int pointIndex2, int pointIndex3) {
        if (pointIndex1 == pointIndex2 || pointIndex2 == pointIndex3 || pointIndex3 == pointIndex1) {
            return -1;
        }
        
        int triangleIndex = getStoredTriangleNum();
        trianglePointIndexes.add(pointIndex1);
        trianglePointIndexes.add(pointIndex2);
        trianglePointIndexes.add(pointIndex3);
        
        pointToTriangles.get(pointIndex1).add(triangleIndex);
        pointToTriangles.get(pointIndex2).add(triangleIndex);
        pointToTriangles.get(pointIndex3).add(triangleIndex);
        
        notifyTriangleAddedForQuadTree(triangleIndex);
        
        return triangleIndex;
    }
    
    public int indexPoint(double x, double y) {
        Validate.isTrue(x >= -1.0001 && x < 1.0001);
        Validate.isTrue(y >= -1.0001 && y < 1.0001);
        long grid = encodeToGrid(x, y);
        int pointIndex = gridToPointIndex.getOrDefault(grid, -1);
        if (pointIndex == -1) {
            pointIndex = pointCoords.size() / 2;
            pointCoords.add(x);
            pointCoords.add(y);
            gridToPointIndex.put(grid, pointIndex);
            pointToTriangles.add(new IntArrayList());
        }
        return pointIndex;
    }
    
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
    
    public double getAngleFromOneCorner(
        int pointIndex, int triangleIndex
    ) {
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
        int p3Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
        
        int otherP1Index;
        int otherP2Index;
        
        if (pointIndex == p1Index) {
            otherP1Index = p2Index;
            otherP2Index = p3Index;
        }
        else if (pointIndex == p2Index) {
            otherP1Index = p1Index;
            otherP2Index = p3Index;
        }
        else if (pointIndex == p3Index) {
            otherP1Index = p1Index;
            otherP2Index = p2Index;
        }
        else {
            throw new IllegalArgumentException();
        }
        
        double x1 = pointCoords.getDouble(pointIndex * 2);
        double y1 = pointCoords.getDouble(pointIndex * 2 + 1);
        double x2 = pointCoords.getDouble(otherP1Index * 2);
        double y2 = pointCoords.getDouble(otherP1Index * 2 + 1);
        double x3 = pointCoords.getDouble(otherP2Index * 2);
        double y3 = pointCoords.getDouble(otherP2Index * 2 + 1);
        
        double dx1 = x2 - x1;
        double dy1 = y2 - y1;
        double dx2 = x3 - x1;
        double dy2 = y3 - y1;
        
        return getAngle(dx1, dy1, dx2, dy2);
    }
    
    public double getExposingAngle(int pointIndex) {
        IntArrayList triangles = pointToTriangles.get(pointIndex);
        if (triangles.size() == 0) {
            return 0;
        }
        
        double sumAngle = 0;
        IntIterator iter = triangles.intIterator();
        while (iter.hasNext()) {
            int triangleIndex = iter.nextInt();
            sumAngle += getAngleFromOneCorner(pointIndex, triangleIndex);
        }
        
        return sumAngle;
    }
    
    public boolean isPointInsideMesh(int pointIndex) {
        return Math.abs(getExposingAngle(pointIndex) - Math.PI * 2) < 0.00001;
    }
    
    public void removeTriangle(int triangleIndex) {
        notifyTriangleRemovedForQuadTree(triangleIndex);
        
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
        int p3Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
        
        pointToTriangles.get(p1Index).rem(triangleIndex); // removeInt takes index
        pointToTriangles.get(p2Index).rem(triangleIndex);
        pointToTriangles.get(p3Index).rem(triangleIndex);
        
        trianglePointIndexes.set(triangleIndex * 3, -1);
        trianglePointIndexes.set(triangleIndex * 3 + 1, -1);
        trianglePointIndexes.set(triangleIndex * 3 + 2, -1);
    }
    
    public boolean isTriangleValid(int triangleIndex) {
        if (triangleIndex == -1) {
            return false;
        }
        
        return trianglePointIndexes.getInt(triangleIndex * 3) != -1;
    }
    
    public boolean isPointUsed(int pointIndex) {
        Validate.isTrue(pointIndex != -1);
        
        return !pointToTriangles.get(pointIndex).isEmpty();
    }
    
    /**
     * @return number of points stored, including the unused ones
     */
    public int getStoredPointNum() {
        return pointCoords.size() / 2;
    }
    
    /**
     * @return number of triangles stored, including the invalid ones
     */
    public int getStoredTriangleNum() {
        return trianglePointIndexes.size() / 3;
    }
    
    public void updateTrianglePoint(
        int triangleIndex,
        int pointIndexInTriangle,
        int newPointIndex
    ) {
        notifyTriangleRemovedForQuadTree(triangleIndex);
        int oldPointIndex = trianglePointIndexes.getInt(triangleIndex * 3 + pointIndexInTriangle);
        pointToTriangles.get(oldPointIndex).rem(triangleIndex);
        pointToTriangles.get(newPointIndex).add(triangleIndex);
        trianglePointIndexes.set(triangleIndex * 3 + pointIndexInTriangle, newPointIndex);
        notifyTriangleAddedForQuadTree(triangleIndex);
    }
    
    public void collapseEdge(int destPointIndex, int movingPointIndex) {
        IntArrayList triangles1 = pointToTriangles.get(destPointIndex);
        IntArrayList triangles2 = pointToTriangles.get(movingPointIndex);
        
        IntOpenHashSet triangles = new IntOpenHashSet();
        triangles.addAll(triangles1);
        triangles.addAll(triangles2);
        
        IntIterator iter = triangles.intIterator();
        while (iter.hasNext()) {
            int triangleIndex = iter.nextInt();
            int p0IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3);
            int p1IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
            int p2IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
            
            if (p0IndexInTriangle == movingPointIndex) {
                updateTrianglePoint(triangleIndex, 0, destPointIndex);
                p0IndexInTriangle = destPointIndex;
            }
            
            if (p1IndexInTriangle == movingPointIndex) {
                updateTrianglePoint(triangleIndex, 1, destPointIndex);
                p1IndexInTriangle = destPointIndex;
            }
            
            if (p2IndexInTriangle == movingPointIndex) {
                updateTrianglePoint(triangleIndex, 2, destPointIndex);
                p2IndexInTriangle = destPointIndex;
            }
            
            if (p0IndexInTriangle == p1IndexInTriangle ||
                p0IndexInTriangle == p2IndexInTriangle ||
                p1IndexInTriangle == p2IndexInTriangle
            ) {
                removeTriangle(triangleIndex);
            }
        }
    }
    
    /**
     * @param startPointIndex The point index to start with.
     * @return If succeeded, return the point index that gets collapsed. -1 for failed.
     */
    private int tryToCollapseInnerEdge(int startPointIndex) {
        for (int i = startPointIndex; i < pointCoords.size() / 2; i++) {
            if (!isPointUsed(i)) {
                continue;
            }
            
            if (isPointInsideMesh(i)) {
                IntArrayList triangles = pointToTriangles.get(i);
                IntIterator iter = triangles.intIterator();
                while (iter.hasNext()) {
                    int triangleIndex = iter.nextInt();
                    int p0Index = trianglePointIndexes.getInt(triangleIndex * 3);
                    int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
                    int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
                    
                    if (isPointInsideMesh(p0Index) && isPointInsideMesh(p1Index)) {
                        collapseEdge(p0Index, p1Index);
                        return i;
                    }
                    else if (isPointInsideMesh(p1Index) && isPointInsideMesh(p2Index)) {
                        collapseEdge(p1Index, p2Index);
                        return i;
                    }
                    else if (isPointInsideMesh(p2Index) && isPointInsideMesh(p0Index)) {
                        collapseEdge(p2Index, p0Index);
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }
    
    /**
     * @param startPointIndex The point index to start with.
     * @return If succeeded, return the point index that gets collapsed. -1 for failed.
     */
    private int tryToCollapseOuterEdge(int startPointIndex) {
        for (int i = 0; i < pointCoords.size() / 2; i++) {
            if (!isPointUsed(i)) {
                continue;
            }
            
            double x = pointCoords.getDouble(i * 2);
            double y = pointCoords.getDouble(i * 2 + 1);
            
            double exposingAngle = getExposingAngle(i);
            
            if (Math.abs(exposingAngle - Math.PI) < 0.0001) {
                IntOpenHashSet adjacentVertices = adjacentVerticesFrom(i);
                
                IntIterator iter1 = adjacentVertices.intIterator();
                while (iter1.hasNext()) {
                    int p1 = iter1.nextInt();
                    
                    IntIterator iter2 = adjacentVertices.intIterator();
                    while (iter2.hasNext()) {
                        int p2 = iter2.nextInt();
                        
                        if (p1 < p2) {
                            double p1x = pointCoords.getDouble(p1 * 2);
                            double p1y = pointCoords.getDouble(p1 * 2 + 1);
                            double p2x = pointCoords.getDouble(p2 * 2);
                            double p2y = pointCoords.getDouble(p2 * 2 + 1);
                            
                            if (isCollinear(p1x - x, p1y - y, p2x - x, p2y - y)) {
                                collapseEdge(p1, i);
                                return i;
                            }
                        }
                    }
                }
            }
        }
        
        return -1;
    }
    
    public static boolean isCollinear(double v1x, double v1y, double v2x, double v2y) {
        double d1l = Math.sqrt(v1x * v1x + v1y * v1y);
        double d2l = Math.sqrt(v2x * v2x + v2y * v2y);
        
        if (d1l == 0 || d2l == 0) {
            return true;
        }
        
        double dot = (v1x / d1l) * (v2x / d2l) + (v1y / d1l) * (v2y / d2l);
        
        return Math.abs(dot - (-1)) < 0.0001;
    }
    
    /**
     * @param countLimit The maximum number of points to collapse.
     * @return The number of edges collapsed.
     */
    public int simplify(int countLimit) {
        int count = 0;
        
        int innerEdgeCollapsePointIndex = 0;
        int outerEdgeCollapsePointIndex = 0;
        
        for (; ; ) {
            if (count >= countLimit) {
                break;
            }
            
            if (innerEdgeCollapsePointIndex != -1) {
                innerEdgeCollapsePointIndex = tryToCollapseInnerEdge(innerEdgeCollapsePointIndex);
                if (innerEdgeCollapsePointIndex != -1) {
                    count++;
                }
            }
            else if (outerEdgeCollapsePointIndex != -1) {
                outerEdgeCollapsePointIndex = tryToCollapseOuterEdge(outerEdgeCollapsePointIndex);
                if (outerEdgeCollapsePointIndex != -1) {
                    count++;
                }
            }
            else {
                break;
            }
        }
        
        return count;
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
        
    }
    
    public static double select(int index, double n1, double n2, double n3) {
        return switch (index) {
            case 0 -> n1;
            case 1 -> n2;
            case 2 -> n3;
            default -> throw new IllegalArgumentException();
        };
    }
    
    public void subtractTriangleFromMesh(
        double tp0x, double tp0y,
        double tp1x, double tp1y,
        double tp2x, double tp2y
    ) {
        enableTriangleLookup();
        
        double cuttingBbMinX = Math.min(Math.min(tp0x, tp1x), tp2x);
        double cuttingBbMinY = Math.min(Math.min(tp0y, tp1y), tp2y);
        double cuttingBbMaxX = Math.max(Math.max(tp0x, tp1x), tp2x);
        double cuttingBbMaxY = Math.max(Math.max(tp0y, tp1y), tp2y);
        
        // avoid modifying while traversing
        IntArrayList relevantTriangles = new IntArrayList();
        traverseTrianglesByBB(
            cuttingBbMinX, cuttingBbMinY, cuttingBbMaxX, cuttingBbMaxY,
            ti -> {
                relevantTriangles.add(ti);
                return null;
            }
        );
        
        relevantTriangles.intStream().forEach(ti -> {
            subtractTriangleForOneTriangle(
                ti, tp0x, tp0y, tp1x, tp1y, tp2x, tp2y
            );
        });
    }
    
    /**
     * @param targetTriangleIndex The index of the triangle to subtract from.
     * @param tp0x,               tp0y, tp1x, tp1y, tp2x, tp2y The points of the triangle to subtract.
     * @return null if not subtracted, otherwise the indexes of the subtracted triangles.
     */
    private @Nullable IntArrayList subtractTriangleForOneTriangle(
        int targetTriangleIndex,
        double tp0x, double tp0y,
        double tp1x, double tp1y,
        double tp2x, double tp2y
    ) {
        IntArrayList relevantTriangles = new IntArrayList();
        relevantTriangles.add(targetTriangleIndex);
        
        IntArrayList tempTriangles = new IntArrayList();
        
        Line2D line01 = Line2D.fromTwoPoints(tp0x, tp0y, tp1x, tp1y);
        Line2D line12 = Line2D.fromTwoPoints(tp1x, tp1y, tp2x, tp2y);
        Line2D line20 = Line2D.fromTwoPoints(tp2x, tp2y, tp0x, tp0y);
        Line2D[] lines = new Line2D[]{line01, line12, line20};
        
        for (Line2D line : lines) {
            IntIterator iter = relevantTriangles.intIterator();
            while (iter.hasNext()) {
                int triangleIndex = iter.nextInt();
                IntArrayList r = dissectTriangleByLine(triangleIndex, line);
                if (r == null) {
                    tempTriangles.add(triangleIndex);
                }
                else {
                    tempTriangles.addAll(r);
                }
            }
            
            relevantTriangles.clear();
            IntArrayList swappingTemp = relevantTriangles;
            relevantTriangles = tempTriangles;
            tempTriangles = swappingTemp;
        }
        
        relevantTriangles.removeIf((int triangleIndex) -> {
            int p1Index = trianglePointIndexes.getInt(triangleIndex * 3);
            int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
            int p3Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
            
            double p0x = pointCoords.getDouble(p1Index * 2);
            double p0y = pointCoords.getDouble(p1Index * 2 + 1);
            double p1x = pointCoords.getDouble(p2Index * 2);
            double p1y = pointCoords.getDouble(p2Index * 2 + 1);
            double p2x = pointCoords.getDouble(p3Index * 2);
            double p2y = pointCoords.getDouble(p3Index * 2 + 1);
            
            double centerX = (p0x + p1x + p2x) / 3;
            double centerY = (p0y + p1y + p2y) / 3;
            
            boolean shouldRemove = line01.testSideBool(centerX, centerY) &&
                line12.testSideBool(centerX, centerY) &&
                line20.testSideBool(centerX, centerY);
            
            if (shouldRemove) {
                removeTriangle(triangleIndex);
            }
            
            return shouldRemove;
        });
        
        return relevantTriangles;
    }
    
    /**
     * @return null if not dissected, otherwise the indexes of the dissected triangles.
     */
    public @Nullable IntArrayList dissectTriangleByLine(
        int triangleIndex,
        Line2D line
    ) {
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
        int p3Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
        
        double p0x = pointCoords.getDouble(p1Index * 2);
        double p0y = pointCoords.getDouble(p1Index * 2 + 1);
        double p1x = pointCoords.getDouble(p2Index * 2);
        double p1y = pointCoords.getDouble(p2Index * 2 + 1);
        double p2x = pointCoords.getDouble(p3Index * 2);
        double p2y = pointCoords.getDouble(p3Index * 2 + 1);
        
        boolean side0 = line.testSideBool(p0x, p0y);
        boolean side1 = line.testSideBool(p1x, p1y);
        boolean side2 = line.testSideBool(p2x, p2y);
        
        if (side0 == side1 && side1 == side2) {
            return null;
        }
        
        if (side0 == side1) {
            return dissectTriangleByEdgesFromVertex(
                triangleIndex, line, 2
            );
        }
        
        if (side1 == side2) {
            return dissectTriangleByEdgesFromVertex(
                triangleIndex, line, 0
            );
        }
        
        if (side2 == side0) {
            return dissectTriangleByEdgesFromVertex(
                triangleIndex, line, 1
            );
        }
        
        throw new IllegalStateException();
    }
    
    /**
     * @param triangleIndex the index of the triangle to dissect
     * @param line          the line to dissect the triangle
     * @param v0            the index of the vertex in the triangle
     * @return null if not dissected, otherwise the indexes of the dissected triangles.
     */
    private @Nullable IntArrayList dissectTriangleByEdgesFromVertex(
        int triangleIndex,
        Line2D line,
        int v0
    ) {
        int v1 = (v0 + 1) % 3;
        int v2 = (v0 + 2) % 3;
        
        int p0Index = trianglePointIndexes.getInt(triangleIndex * 3 + v0);
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + v1);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + v2);
        
        double p0x = pointCoords.getDouble(p0Index * 2);
        double p0y = pointCoords.getDouble(p0Index * 2 + 1);
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        
        Line2D line01 = Line2D.fromTwoPoints(p0x, p0y, p1x, p1y);
        Line2D line02 = Line2D.fromTwoPoints(p0x, p0y, p2x, p2y);
        
        double t01 = line01.getIntersectionWithLine(line);
        double t02 = line02.getIntersectionWithLine(line);
        
        int intersectionPoint01 = -1;
        if (!Double.isNaN(t01) && t01 >= 0 && t01 <= 1) {
            // p0-p1 intersects with the line
            double ix = line01.linePX + t01 * line01.dirX;
            double iy = line01.linePY + t01 * line01.dirY;
            intersectionPoint01 = indexPoint(ix, iy);
            
            if (intersectionPoint01 == p0Index || intersectionPoint01 == p1Index) {
                intersectionPoint01 = -1;
            }
        }
        
        int intersectionPoint02 = -1;
        if (!Double.isNaN(t02) && t02 >= 0 && t02 <= 1) {
            // p0-p2 intersects with the line
            double ix = line02.linePX + t02 * line02.dirX;
            double iy = line02.linePY + t02 * line02.dirY;
            intersectionPoint02 = indexPoint(ix, iy);
            
            if (intersectionPoint02 == p0Index || intersectionPoint02 == p2Index) {
                intersectionPoint02 = -1;
            }
        }
        
        // has no intersection with two edges
        if (intersectionPoint01 == -1 && intersectionPoint02 == -1) {
            return null;
        }
        
        // has intersections with both two edges
        // dissect the triangle into a triangle and a quad
        // the quad is then dissected into two triangles
        if (intersectionPoint01 != -1 && intersectionPoint02 != -1) {
            removeTriangle(triangleIndex);
            
            int t1 = addTriangle(p0Index, intersectionPoint01, intersectionPoint02);
            
            int quadP0Index = p1Index;
            int quadP1Index = p2Index;
            int quadP2Index = intersectionPoint02;
            int quadP3Index = intersectionPoint01;
            
            //       p0
            //      /  \
            //     /    \
            //    /      \
            //   /        \
            //  /          \
            // p1----------p2
            // turn to
            //       p0
            //      /  \
            //     /    \
            //  quadP3 quadP2
            //   /        \
            //  /          \
            // quadP0-----quadP1
            
            // there are two ways of splitting a quad into two triangles
            // we disfavor thin and long triangles, so cut along the shorter diagonal
            int t2;
            int t3;
            if (getDistanceSq(quadP0Index, quadP2Index) < getDistanceSq(quadP1Index, quadP3Index)) {
                t2 = addTriangle(quadP0Index, quadP1Index, quadP2Index);
                t3 = addTriangle(quadP0Index, quadP2Index, quadP3Index);
            }
            else {
                t2 = addTriangle(quadP0Index, quadP1Index, quadP3Index);
                t3 = addTriangle(quadP1Index, quadP2Index, quadP3Index);
            }
            
            return IntArrayList.of(t1, t2, t3);
        }
        
        if (intersectionPoint01 != -1) {
            removeTriangle(triangleIndex);
            
            //       p0
            //      /  \
            //     /    \
            //    x      \
            //   /  -     \
            //  /      -   \
            // p1----------p2
            
            int t1 = addTriangle(p1Index, p2Index, intersectionPoint01);
            int t2 = addTriangle(p2Index, p0Index, intersectionPoint01);
            return IntArrayList.of(t1, t2);
        }
        
        if (intersectionPoint02 != -1) {
            removeTriangle(triangleIndex);
            
            //       p0
            //      /  \
            //     /    \
            //    /      x
            //   /    -   \
            //  /  -       \
            // p1----------p2
            
            int t1 = addTriangle(p0Index, p1Index, intersectionPoint02);
            int t2 = addTriangle(p1Index, p2Index, intersectionPoint02);
            return IntArrayList.of(t1, t2);
        }
        
        throw new IllegalStateException();
    }
    
    public IntOpenHashSet adjacentVerticesFrom(int pointIndex) {
        IntOpenHashSet result = new IntOpenHashSet();
        
        int triangleCount = getStoredTriangleNum();
        for (int i = 0; i < triangleCount; i++) {
            if (!isTriangleValid(i)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(i * 3);
            int p1Index = trianglePointIndexes.getInt(i * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(i * 3 + 2);
            
            if (p0Index == pointIndex) {
                result.add(p1Index);
                result.add(p2Index);
            }
            else if (p1Index == pointIndex) {
                result.add(p0Index);
                result.add(p2Index);
            }
            else if (p2Index == pointIndex) {
                result.add(p0Index);
                result.add(p1Index);
            }
        }
        
        return result;
    }
    
    private double getDistance(int p1Index, int p2Index) {
        return Math.sqrt(getDistanceSq(p1Index, p2Index));
    }
    
    private double getDistanceSq(int p1Index, int p2Index) {
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        
        return (p1x - p2x) * (p1x - p2x) + (p1y - p2y) * (p1y - p2y);
    }
    
    
    /**
     * Compact the triangle array.
     *
     * @return the number of triangles moved
     */
    public void compactTriangleStorage() {
        int newSize = Helper.compactArrayStorage(
            getStoredTriangleNum(),
            i -> isTriangleValid(i),
            (valid, invalid) -> moveTriangle(valid, invalid)
        );
        
        trianglePointIndexes.removeElements(newSize * 3, trianglePointIndexes.size());
    }
    
    /**
     * Compact the point array.
     *
     * @return the number of points moved
     */
    public void compactPointStorage() {
        // remove all unused points from the grid map
        for (int i = 0; i < getStoredPointNum(); i++) {
            if (!isPointUsed(i)) {
                double x = pointCoords.getDouble(i * 2);
                double y = pointCoords.getDouble(i * 2 + 1);
                long encodedGridIndex = encodeToGrid(x, y);
                gridToPointIndex.remove(encodedGridIndex);
            }
        }
        
        int newSize = Helper.compactArrayStorage(
            getStoredPointNum(),
            i -> isPointUsed(i),
            (valid, invalid) -> movePoint(valid, invalid)
        );
        
        pointCoords.removeElements(newSize * 2, pointCoords.size());
        pointToTriangles.removeElements(newSize, pointToTriangles.size());
        Validate.isTrue(pointToTriangles.size() * 2 == pointCoords.size());
    }
    
    public void compact() {
        compactTriangleStorage();
        compactPointStorage();
    }
    
    public void moveTriangle(int triangleIndex, int destTriangleIndex) {
        Validate.isTrue(!isTriangleValid(destTriangleIndex));
        
        notifyTriangleRemovedForQuadTree(triangleIndex);
        
        int p0Index = trianglePointIndexes.getInt(triangleIndex * 3);
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
        
        pointToTriangles.get(p0Index).rem(triangleIndex);
        pointToTriangles.get(p1Index).rem(triangleIndex);
        pointToTriangles.get(p2Index).rem(triangleIndex);
        
        pointToTriangles.get(p0Index).add(destTriangleIndex);
        pointToTriangles.get(p1Index).add(destTriangleIndex);
        pointToTriangles.get(p2Index).add(destTriangleIndex);
        
        trianglePointIndexes.set(destTriangleIndex * 3, p0Index);
        trianglePointIndexes.set(destTriangleIndex * 3 + 1, p1Index);
        trianglePointIndexes.set(destTriangleIndex * 3 + 2, p2Index);
        
        trianglePointIndexes.set(triangleIndex * 3, -1);
        trianglePointIndexes.set(triangleIndex * 3 + 1, -1);
        trianglePointIndexes.set(triangleIndex * 3 + 2, -1);
        
        notifyTriangleAddedForQuadTree(destTriangleIndex);
    }
    
    public void movePoint(int pointIndex, int destPointIndex) {
        Validate.isTrue(!isPointUsed(destPointIndex));
        
        double x = pointCoords.getDouble(pointIndex * 2);
        double y = pointCoords.getDouble(pointIndex * 2 + 1);
        long gridCoord = encodeToGrid(x, y);
        int removedPointIndex = gridToPointIndex.remove(gridCoord);
        Validate.isTrue(removedPointIndex == pointIndex);
        gridToPointIndex.put(gridCoord, destPointIndex);
        
        pointCoords.set(destPointIndex * 2, x);
        pointCoords.set(destPointIndex * 2 + 1, y);
        
        IntArrayList triangles = pointToTriangles.get(pointIndex);
        pointToTriangles.set(destPointIndex, triangles);
        pointToTriangles.set(pointIndex, null);
        
        IntIterator iter = triangles.intIterator();
        while (iter.hasNext()) {
            int triangleIndex = iter.nextInt();
            int p0IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3);
            int p1IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
            int p2IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
            
            if (p0IndexInTriangle == pointIndex) {
                trianglePointIndexes.set(triangleIndex * 3, destPointIndex);
                p0IndexInTriangle = destPointIndex;
            }
            
            if (p1IndexInTriangle == pointIndex) {
                trianglePointIndexes.set(triangleIndex * 3 + 1, destPointIndex);
                p1IndexInTriangle = destPointIndex;
            }
            
            if (p2IndexInTriangle == pointIndex) {
                trianglePointIndexes.set(triangleIndex * 3 + 2, destPointIndex);
                p2IndexInTriangle = destPointIndex;
            }
        }
    }
    
    public void enableTriangleLookup() {
        if (triangleLookup != null) {
            return;
        }
        
        triangleLookup = new QuadTree<>(() -> new IntArrayList(1));
        
        for (int ti = 0; ti < getStoredTriangleNum(); ++ti) {
            if (!isTriangleValid(ti)) {
                continue;
            }
            
            getTriangleIdListInTriangleLookup(ti).add(ti);
        }
    }
    
    private IntArrayList getTriangleIdListInTriangleLookup(int ti) {
        Validate.notNull(triangleLookup);
        
        int p0Index = trianglePointIndexes.getInt(ti * 3);
        int p1Index = trianglePointIndexes.getInt(ti * 3 + 1);
        int p2Index = trianglePointIndexes.getInt(ti * 3 + 2);
        
        double p0x = pointCoords.getDouble(p0Index * 2);
        double p0y = pointCoords.getDouble(p0Index * 2 + 1);
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        
        double minX = Math.min(p0x, Math.min(p1x, p2x));
        double minY = Math.min(p0y, Math.min(p1y, p2y));
        double maxX = Math.max(p0x, Math.max(p1x, p2x));
        double maxY = Math.max(p0y, Math.max(p1y, p2y));
        
        return triangleLookup.acquireElementForBoundingBox(
            minX, minY, maxX, maxY
        );
    }
    
    private <U> @Nullable U traverseNearbyTriangles(
        int triangleIndex,
        Int2ObjectFunction<U> func
    ) {
        Validate.notNull(triangleLookup);
        
        int p0Index = trianglePointIndexes.getInt(triangleIndex * 3);
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
        
        double p0x = pointCoords.getDouble(p0Index * 2);
        double p0y = pointCoords.getDouble(p0Index * 2 + 1);
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        
        double minX = Math.min(p0x, Math.min(p1x, p2x));
        double minY = Math.min(p0y, Math.min(p1y, p2y));
        double maxX = Math.max(p0x, Math.max(p1x, p2x));
        double maxY = Math.max(p0y, Math.max(p1y, p2y));
        
        return traverseTrianglesByBB(
            minX, minY, maxX, maxY, ti -> {
                if (ti != triangleIndex) {
                    return func.apply(ti);
                }
                return null;
            }
        );
    }
    
    public boolean boundingBoxIntersects(
        int triangleIndex,
        double minX, double minY, double maxX, double maxY
    ) {
        int p0Index = trianglePointIndexes.getInt(triangleIndex * 3);
        int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
        int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
        
        double p0x = pointCoords.getDouble(p0Index * 2);
        double p0y = pointCoords.getDouble(p0Index * 2 + 1);
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        
        double triMinX = Math.min(p0x, Math.min(p1x, p2x));
        double triMinY = Math.min(p0y, Math.min(p1y, p2y));
        double triMaxX = Math.max(p0x, Math.max(p1x, p2x));
        double triMaxY = Math.max(p0y, Math.max(p1y, p2y));
        
        return Range.rangeIntersects(minX, maxX, triMinX, triMaxX) &&
            Range.rangeIntersects(minY, maxY, triMinY, triMaxY);
    }
    
    private <U> @Nullable U traverseTrianglesByBB(
        double minX, double minY, double maxX, double maxY,
        Int2ObjectFunction<U> func
    ) {
        Validate.notNull(triangleLookup);
        return triangleLookup.traverse(
            minX, minY, maxX, maxY,
            (IntArrayList triangleIndexList) -> {
                IntIterator iterator = triangleIndexList.intIterator();
                while (iterator.hasNext()) {
                    int ti = iterator.nextInt();
                    if (boundingBoxIntersects(ti, minX, minY, maxX, maxY)) {
                        U result = func.apply(ti);
                        if (result != null) {
                            return result;
                        }
                    }
                }
                return null;
            }
        );
    }
    
    public void notifyTriangleAddedForQuadTree(int triangleIndex) {
        if (triangleLookup == null) {
            return;
        }
        
        getTriangleIdListInTriangleLookup(triangleIndex).add(triangleIndex);
    }
    
    public void notifyTriangleRemovedForQuadTree(int triangleIndex) {
        if (triangleLookup == null) {
            return;
        }
        
        boolean removed = getTriangleIdListInTriangleLookup(triangleIndex).rem(triangleIndex);
        Validate.isTrue(removed, "triangle %d not found in quad tree", triangleIndex);
    }
    
    // for unit testing
    public void checkStorageIntegrity() {
        Validate.isTrue(pointCoords.size() % 2 == 0);
        Validate.isTrue(pointCoords.size() / 2 == pointToTriangles.size());
        
        Validate.isTrue(trianglePointIndexes.size() % 3 == 0);
        
        for (int triangleIndex = 0; triangleIndex < getStoredTriangleNum(); triangleIndex++) {
            if (!isTriangleValid(triangleIndex)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(triangleIndex * 3);
            int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
            
            Validate.isTrue(pointToTriangles.get(p0Index).contains(triangleIndex));
            Validate.isTrue(pointToTriangles.get(p1Index).contains(triangleIndex));
            Validate.isTrue(pointToTriangles.get(p2Index).contains(triangleIndex));
            
            if (triangleLookup != null) {
                Validate.isTrue(getTriangleIdListInTriangleLookup(triangleIndex).contains(triangleIndex));
            }
        }
        
        for (IntArrayList triangleIds : pointToTriangles) {
            triangleIds.intStream().forEach(triangleIndex -> {
                Validate.isTrue(isTriangleValid(triangleIndex));
            });
        }
        
        if (triangleLookup != null) {
            triangleLookup.traverse(
                -1, -1, 1, 1,
                triangleList -> {
                    triangleList.intStream().forEach(triangleIndex -> {
                        Validate.isTrue(isTriangleValid(triangleIndex));
                    });
                    return null;
                }
            );
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        for (int triangleIndex = 0; triangleIndex < getStoredTriangleNum(); triangleIndex++) {
            if (!isTriangleValid(triangleIndex)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(triangleIndex * 3);
            int p1Index = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
            
            double p0x = pointCoords.getDouble(p0Index * 2);
            double p0y = pointCoords.getDouble(p0Index * 2 + 1);
            double p1x = pointCoords.getDouble(p1Index * 2);
            double p1y = pointCoords.getDouble(p1Index * 2 + 1);
            double p2x = pointCoords.getDouble(p2Index * 2);
            double p2y = pointCoords.getDouble(p2Index * 2 + 1);
            
            sb.append(String.format(
                "%d: (%.3f, %.3f) (%.3f, %.3f) (%.3f, %.3f)\n",
                triangleIndex, p0x, p0y, p1x, p1y, p2x, p2y
            ));
        }
        
        return sb.toString();
    }
    
    @FunctionalInterface
    public static interface BiDoublePredicate {
        boolean test(double x, double y);
    }
    
    public boolean triangleIntersects(int ti1, int ti2) {
        int t1p0Index = trianglePointIndexes.getInt(ti1 * 3);
        int t1p1Index = trianglePointIndexes.getInt(ti1 * 3 + 1);
        int t1p2Index = trianglePointIndexes.getInt(ti1 * 3 + 2);
        
        int t2p0Index = trianglePointIndexes.getInt(ti2 * 3);
        int t2p1Index = trianglePointIndexes.getInt(ti2 * 3 + 1);
        int t2p2Index = trianglePointIndexes.getInt(ti2 * 3 + 2);
        
        double t1p0x = pointCoords.getDouble(t1p0Index * 2);
        double t1p0y = pointCoords.getDouble(t1p0Index * 2 + 1);
        double t1p1x = pointCoords.getDouble(t1p1Index * 2);
        double t1p1y = pointCoords.getDouble(t1p1Index * 2 + 1);
        double t1p2x = pointCoords.getDouble(t1p2Index * 2);
        double t1p2y = pointCoords.getDouble(t1p2Index * 2 + 1);
        
        double t2p0x = pointCoords.getDouble(t2p0Index * 2);
        double t2p0y = pointCoords.getDouble(t2p0Index * 2 + 1);
        double t2p1x = pointCoords.getDouble(t2p1Index * 2);
        double t2p1y = pointCoords.getDouble(t2p1Index * 2 + 1);
        double t2p2x = pointCoords.getDouble(t2p2Index * 2);
        double t2p2y = pointCoords.getDouble(t2p2Index * 2 + 1);
        
        return triangleIntersects(
            t1p0x, t1p0y, t1p1x, t1p1y, t1p2x, t1p2y,
            t2p0x, t2p0y, t2p1x, t2p1y, t2p2x, t2p2y
        );
    }
    
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
        
        return t0max - t1min < 0.00001 || t1max - t0min < 0.00001;
    }
    
    public void fixIntersectedTriangle(int maxOperationNum) {
        int operationNum = 0;
        int startTriangleIndex = 0;
        while (operationNum < maxOperationNum && startTriangleIndex < getStoredTriangleNum()) {
            startTriangleIndex = tryFixIntersectedTriangle(startTriangleIndex);
            operationNum++;
        }
    }
    
    private int tryFixIntersectedTriangle(int startTriangleIndex) {
        enableTriangleLookup();
        assert triangleLookup != null;
        int storedTriangleNum = getStoredTriangleNum();
        for (int ti = startTriangleIndex; ti < storedTriangleNum; ti++) {
            if (!isTriangleValid(ti)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(ti * 3);
            int p1Index = trianglePointIndexes.getInt(ti * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(ti * 3 + 2);
            double p0x = pointCoords.getDouble(p0Index * 2);
            double p0y = pointCoords.getDouble(p0Index * 2 + 1);
            double p1x = pointCoords.getDouble(p1Index * 2);
            double p1y = pointCoords.getDouble(p1Index * 2 + 1);
            double p2x = pointCoords.getDouble(p2Index * 2);
            double p2y = pointCoords.getDouble(p2Index * 2 + 1);
            
            int tiFinal = ti;
            @Nullable Unit unit = traverseNearbyTriangles(
                ti,
                anotherTi -> {
                    if (tiFinal < anotherTi) {
                        if (triangleIntersects(tiFinal, anotherTi)) {
                            subtractTriangleForOneTriangle(
                                tiFinal, p0x, p0y, p1x, p1y, p2x, p2y
                            );
                            return Unit.INSTANCE;
                        }
                    }
                    
                    return null;
                }
            );
            
            if (unit != null) {
                return ti;
            }
        }
        
        return storedTriangleNum;
    }
    
    public void fixDisconnectedEdges() {
        throw new NotImplementedException();
    }
}
