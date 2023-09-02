package qouteall.q_misc_util.my_util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;

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
    
    /**
     * @return the index of the triangle. -1 if the triangle is invalid.
     */
    public int addTriangle(int pointIndex0, int pointIndex1, int pointIndex2) {
        if (pointIndex0 == pointIndex1 || pointIndex1 == pointIndex2 || pointIndex2 == pointIndex0) {
            return -1;
        }
        
        double p0x = pointCoords.getDouble(pointIndex0 * 2);
        double p0y = pointCoords.getDouble(pointIndex0 * 2 + 1);
        double p1x = pointCoords.getDouble(pointIndex1 * 2);
        double p1y = pointCoords.getDouble(pointIndex1 * 2 + 1);
        double p2x = pointCoords.getDouble(pointIndex2 * 2);
        double p2y = pointCoords.getDouble(pointIndex2 * 2 + 1);
        
        double cross = Helper.crossProduct2D(
            p1x - p0x, p1y - p0y, p2x - p0x, p2y - p0y
        );
        
        if (Math.abs(cross) < 0.000000001) {
            return -1;
        }
        
        int triangleIndex = getStoredTriangleNum();
        trianglePointIndexes.add(pointIndex0);
        if (cross > 0) {
            trianglePointIndexes.add(pointIndex1);
            trianglePointIndexes.add(pointIndex2);
        }
        else {
            trianglePointIndexes.add(pointIndex2);
            trianglePointIndexes.add(pointIndex1);
        }
        
        pointToTriangles.get(pointIndex0).add(triangleIndex);
        pointToTriangles.get(pointIndex1).add(triangleIndex);
        pointToTriangles.get(pointIndex2).add(triangleIndex);
        
        Validate.isTrue(isTriangleValid(triangleIndex));
        
        notifyTriangleAddedForQuadTree(triangleIndex);
        
        return triangleIndex;
    }
    
    public int indexPoint(double x, double y) {
        x = Mth.clamp(x, -1.0, 1.0);
        y = Mth.clamp(y, -1.0, 1.0);
        
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
        
        return GeometryUtil.getAngle(dx1, dy1, dx2, dy2);
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
    
    public int getTrianglePointIndex(int triangleIndex, int indexInTriangle) {
        return trianglePointIndexes.getInt(triangleIndex * 3 + indexInTriangle);
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
        turnTriangleToCounterClockwise(triangleIndex);
        notifyTriangleAddedForQuadTree(triangleIndex);
    }
    
    /**
     * @return whether the triangle changes
     */
    public boolean turnTriangleToCounterClockwise(int triangleIndex) {
        Validate.isTrue(isTriangleValid(triangleIndex));
        
        int p0Index = getTrianglePointIndex(triangleIndex, 0);
        int p1Index = getTrianglePointIndex(triangleIndex, 1);
        int p2Index = getTrianglePointIndex(triangleIndex, 2);
        
        double p0x = pointCoords.getDouble(p0Index * 2);
        double p0y = pointCoords.getDouble(p0Index * 2 + 1);
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        
        double cross = Helper.crossProduct2D(
            p1x - p0x, p1y - p0y, p2x - p0x, p2y - p0y
        );
        
        if (cross < 0) {
            // not counter-clockwise
            // swap the p1 and p2
            trianglePointIndexes.set(triangleIndex * 3 + 1, p2Index);
            trianglePointIndexes.set(triangleIndex * 3 + 2, p1Index);
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * If any triangle flips after collapsing, it cannot collapse
     */
    public boolean canCollapseEdge(int destPointIndex, int movingPointIndex) {
        IntArrayList trianglesMoving = pointToTriangles.get(movingPointIndex);
        
        IntIterator iter = trianglesMoving.intIterator();
        while (iter.hasNext()) {
            int triangleIndex = iter.nextInt();
            int p0IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3);
            int p1IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3 + 1);
            int p2IndexInTriangle = trianglePointIndexes.getInt(triangleIndex * 3 + 2);
            
            if (p0IndexInTriangle == movingPointIndex) {
                p0IndexInTriangle = destPointIndex;
            }
            if (p1IndexInTriangle == movingPointIndex) {
                p1IndexInTriangle = destPointIndex;
            }
            if (p2IndexInTriangle == movingPointIndex) {
                p2IndexInTriangle = destPointIndex;
            }
            
            if (p0IndexInTriangle == p1IndexInTriangle ||
                p0IndexInTriangle == p2IndexInTriangle ||
                p1IndexInTriangle == p2IndexInTriangle
            ) {
                continue;
            }
            
            double np0x = pointCoords.getDouble(p0IndexInTriangle * 2);
            double np0y = pointCoords.getDouble(p0IndexInTriangle * 2 + 1);
            double np1x = pointCoords.getDouble(p1IndexInTriangle * 2);
            double np1y = pointCoords.getDouble(p1IndexInTriangle * 2 + 1);
            double np2x = pointCoords.getDouble(p2IndexInTriangle * 2);
            double np2y = pointCoords.getDouble(p2IndexInTriangle * 2 + 1);
            
            double cross = Helper.crossProduct2D(
                np1x - np0x, np1y - np0y, np2x - np0x, np2y - np0y
            );
            if (cross < 0) {
                return false;
            }
        }
        
        return true;
    }
    
    public void collapseEdge(int destPointIndex, int movingPointIndex) {
        // copy to avoid modifying while traversal
        IntArrayList relevantTriangles = new IntArrayList(pointToTriangles.get(movingPointIndex));
        
        IntIterator iter = relevantTriangles.intIterator();
        
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
                    
                    if (p1Index == i) {
                        if (canCollapseEdge(p0Index, p1Index)) {
                            collapseEdge(p0Index, p1Index);
                            return i;
                        }
                    }
                    if (p2Index == i) {
                        if (canCollapseEdge(p1Index, p2Index)) {
                            collapseEdge(p1Index, p2Index);
                            return i;
                        }
                    }
                    if (p0Index == i) {
                        if (canCollapseEdge(p2Index, p0Index)) {
                            collapseEdge(p2Index, p0Index);
                            return i;
                        }
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
    private int tryToCollapseOuterEdgeAndTooShortEdges(int startPointIndex) {
        for (int i = 0; i < pointCoords.size() / 2; i++) {
            if (!isPointUsed(i)) {
                continue;
            }
            
            double x = pointCoords.getDouble(i * 2);
            double y = pointCoords.getDouble(i * 2 + 1);
            
            IntOpenHashSet adjacentVertices = adjacentVerticesFrom(i);
            
            // try to collapse outer edge
            double exposingAngle = getExposingAngle(i);
            boolean isOnOuterEdgeStraightLine = Math.abs(exposingAngle - Math.PI) < 0.0001;
            
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
                        
                        boolean collinear = GeometryUtil.isOppositeVec(
                            p1x - x, p1y - y, p2x - x, p2y - y
                        );
                        
                        double lenSq = (p1x - p2x) * (p1x - p2x) + (p1y - p2y) * (p1y - p2y);
                        
                        if ((isOnOuterEdgeStraightLine && collinear) || lenSq < 1.0e-8) {
                            if (canCollapseEdge(p1, i)) {
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
    
    public void simplify() {
        simplifySteps(getStoredTriangleNum());
    }
    
    /**
     * @param countLimit The maximum number of points to collapse.
     * @return The number of edges collapsed.
     */
    public int simplifySteps(int countLimit) {
        fixEdgeCrossingPoint();
        
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
                outerEdgeCollapsePointIndex = tryToCollapseOuterEdgeAndTooShortEdges(outerEdgeCollapsePointIndex);
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
    
    public static double select(int index, double n1, double n2, double n3) {
        return switch (index) {
            case 0 -> n1;
            case 1 -> n2;
            case 2 -> n3;
            default -> throw new IllegalArgumentException();
        };
    }
    
    // Note: must supply a counter-clockwise triangle
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
        if (!triangleIntersects(targetTriangleIndex, tp0x, tp0y, tp1x, tp1y, tp2x, tp2y)) {
            return null;
        }
        
        IntArrayList relevantTriangles = new IntArrayList();
        relevantTriangles.add(targetTriangleIndex);
        
        IntArrayList tempTriangles = new IntArrayList();
        
        GeometryUtil.Line2D line01 = GeometryUtil.Line2D.fromTwoPoints(tp0x, tp0y, tp1x, tp1y);
        GeometryUtil.Line2D line12 = GeometryUtil.Line2D.fromTwoPoints(tp1x, tp1y, tp2x, tp2y);
        GeometryUtil.Line2D line20 = GeometryUtil.Line2D.fromTwoPoints(tp2x, tp2y, tp0x, tp0y);
        GeometryUtil.Line2D[] lines = new GeometryUtil.Line2D[]{line01, line12, line20};
        
        for (GeometryUtil.Line2D line : lines) {
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
            Validate.isTrue(triangleIndex != -1);
            
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
        GeometryUtil.Line2D line
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
        GeometryUtil.Line2D line,
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
        
        GeometryUtil.Line2D line01 = GeometryUtil.Line2D.fromTwoPoints(p0x, p0y, p1x, p1y);
        GeometryUtil.Line2D line02 = GeometryUtil.Line2D.fromTwoPoints(p0x, p0y, p2x, p2y);
        
        double t01 = line01.getIntersectionWithLine(line);
        double t02 = line02.getIntersectionWithLine(line);
        
        int intersectionPoint01 = -1;
        if (!Double.isNaN(t01) && t01 >= 0 && t01 <= 1) {
            // p0-p1 intersects with the line
            double ix = line01.linePX() + t01 * line01.dirX();
            double iy = line01.linePY() + t01 * line01.dirY();
            intersectionPoint01 = indexPoint(ix, iy);
            
            if (intersectionPoint01 == p0Index || intersectionPoint01 == p1Index) {
                intersectionPoint01 = -1;
            }
        }
        
        int intersectionPoint02 = -1;
        if (!Double.isNaN(t02) && t02 >= 0 && t02 <= 1) {
            // p0-p2 intersects with the line
            double ix = line02.linePX() + t02 * line02.dirX();
            double iy = line02.linePY() + t02 * line02.dirY();
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
            
            IntArrayList result = new IntArrayList();
            if (t1 != -1) {
                result.add(t1);
            }
            if (t2 != -1) {
                result.add(t2);
            }
            if (t3 != -1) {
                result.add(t3);
            }
            return result;
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
            
            IntArrayList result = new IntArrayList();
            if (t1 != -1) {
                result.add(t1);
            }
            if (t2 != -1) {
                result.add(t2);
            }
            return result;
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
            
            IntArrayList result = new IntArrayList();
            if (t1 != -1) {
                result.add(t1);
            }
            if (t2 != -1) {
                result.add(t2);
            }
            return result;
        }
        
        throw new IllegalStateException();
    }
    
    public IntOpenHashSet adjacentVerticesFrom(int pointIndex) {
        IntOpenHashSet result = new IntOpenHashSet();
        
        IntArrayList triangleIds = pointToTriangles.get(pointIndex);
        
        IntIterator iter = triangleIds.intIterator();
        
        while (iter.hasNext()) {
            int ti = iter.nextInt();
            
            int p0Index = trianglePointIndexes.getInt(ti * 3);
            int p1Index = trianglePointIndexes.getInt(ti * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(ti * 3 + 2);
            
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
    
    // for debugging and testing
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
            
            double p0x = pointCoords.getDouble(p0Index * 2);
            double p0y = pointCoords.getDouble(p0Index * 2 + 1);
            double p1x = pointCoords.getDouble(p1Index * 2);
            double p1y = pointCoords.getDouble(p1Index * 2 + 1);
            double p2x = pointCoords.getDouble(p2Index * 2);
            double p2y = pointCoords.getDouble(p2Index * 2 + 1);
            
            // ensure that it's counter-clockwise
            double w = Helper.crossProduct2D(
                p1x - p0x, p1y - p0y,
                p2x - p0x, p2y - p0y
            );
            Validate.isTrue(w > 0, "%f", w);
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
    
    public boolean triangleIntersects(int ti1, int ti2) {
        int t1p0Index = trianglePointIndexes.getInt(ti1 * 3);
        int t1p1Index = trianglePointIndexes.getInt(ti1 * 3 + 1);
        int t1p2Index = trianglePointIndexes.getInt(ti1 * 3 + 2);
        
        double t1p0x = pointCoords.getDouble(t1p0Index * 2);
        double t1p0y = pointCoords.getDouble(t1p0Index * 2 + 1);
        double t1p1x = pointCoords.getDouble(t1p1Index * 2);
        double t1p1y = pointCoords.getDouble(t1p1Index * 2 + 1);
        double t1p2x = pointCoords.getDouble(t1p2Index * 2);
        double t1p2y = pointCoords.getDouble(t1p2Index * 2 + 1);
        
        return triangleIntersects(ti2, t1p0x, t1p0y, t1p1x, t1p1y, t1p2x, t1p2y);
    }
    
    public boolean triangleIntersects(
        int ti, double t1p0x, double t1p0y, double t1p1x, double t1p1y, double t1p2x, double t1p2y
    ) {
        int t2p0Index = trianglePointIndexes.getInt(ti * 3);
        int t2p1Index = trianglePointIndexes.getInt(ti * 3 + 1);
        int t2p2Index = trianglePointIndexes.getInt(ti * 3 + 2);
        
        double t2p0x = pointCoords.getDouble(t2p0Index * 2);
        double t2p0y = pointCoords.getDouble(t2p0Index * 2 + 1);
        double t2p1x = pointCoords.getDouble(t2p1Index * 2);
        double t2p1y = pointCoords.getDouble(t2p1Index * 2 + 1);
        double t2p2x = pointCoords.getDouble(t2p2Index * 2);
        double t2p2y = pointCoords.getDouble(t2p2Index * 2 + 1);
        
        return GeometryUtil.triangleIntersects(
            t1p0x, t1p0y, t1p1x, t1p1y, t1p2x, t1p2y,
            t2p0x, t2p0y, t2p1x, t2p1y, t2p2x, t2p2y
        );
    }
    
    public void fixIntersectedTriangle() {
        int maxOperationNum = getStoredTriangleNum();
        
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
            
            int ti_ = ti;
            @Nullable Unit unit = traverseNearbyTriangles(
                ti,
                anotherTi -> {
                    if (ti_ < anotherTi) {
                        if (triangleIntersects(ti_, anotherTi)) {
                            subtractTriangleForOneTriangle(
                                anotherTi, p0x, p0y, p1x, p1y, p2x, p2y
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
    
    /**
     * For the cases that an edge goes through a point, but the point is not the endpoint of the edge,
     * it will cause simplification to be wrong.
     * This should be fixed by detecting point on edge and split relevant triangles.
     *
     * @return count of operations
     */
    public int fixEdgeCrossingPoint() {
        enableTriangleLookup();
        
        int operationCount = 0;
        
        int i = 0;
        while (i < getStoredPointNum()) {
            if (!isPointUsed(i)) {
                i++;
                continue;
            }
            
            double x = pointCoords.getDouble(i * 2);
            double y = pointCoords.getDouble(i * 2 + 1);
            
            int i_ = i;
            for (; ; ) {
                double tiny = 0.00001;
                Unit result = traverseTrianglesByBB(
                    x - tiny, y - tiny, x + tiny, y + tiny,
                    triangleIndex -> {
                        for (int v = 0; v < 3; v++) {
                            if (getTrianglePointIndex(triangleIndex, v) == i_) {
                                return null;
                            }
                        }
                        
                        for (int edgeIndex = 0; edgeIndex < 3; edgeIndex++) {
                            int p0Index = getTrianglePointIndex(triangleIndex, edgeIndex);
                            int p1Index = getTrianglePointIndex(triangleIndex, (edgeIndex + 1) % 3);
                            int p2Index = getTrianglePointIndex(triangleIndex, (edgeIndex + 2) % 3);
                            
                            if (isOnLineSegment(p0Index, i_, p1Index)) {
                                removeTriangle(triangleIndex);
                                addTriangle(p0Index, i_, p2Index);
                                addTriangle(i_, p1Index, p2Index);
                                return Unit.INSTANCE;
                            }
                        }
                        
                        return null;
                    }
                );
                
                if (result == null) {
                    break;
                }
                else {
                    operationCount++;
                }
            }
            i++;
        }
        
        return operationCount;
    }
    
    public boolean isOnLineSegment(int p1Index, int p2Index, int p3Index) {
        double p1x = pointCoords.getDouble(p1Index * 2);
        double p1y = pointCoords.getDouble(p1Index * 2 + 1);
        double p2x = pointCoords.getDouble(p2Index * 2);
        double p2y = pointCoords.getDouble(p2Index * 2 + 1);
        double p3x = pointCoords.getDouble(p3Index * 2);
        double p3y = pointCoords.getDouble(p3Index * 2 + 1);
        
        return GeometryUtil.isOppositeVec(p1x - p2x, p1y - p2y, p3x - p2x, p3y - p2y);
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
    
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonArray points = new JsonArray();
        JsonArray triangles = new JsonArray();
        
        for (int i = 0; i < getStoredPointNum(); i++) {
            if (!isPointUsed(i)) {
                points.add((JsonElement) null);
                continue;
            }
            
            double x = pointCoords.getDouble(i * 2);
            double y = pointCoords.getDouble(i * 2 + 1);
            
            JsonArray point = new JsonArray();
            point.add(x);
            point.add(y);
            points.add(point);
        }
        
        for (int i = 0; i < getStoredTriangleNum(); i++) {
            if (!isTriangleValid(i)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(i * 3);
            int p1Index = trianglePointIndexes.getInt(i * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(i * 3 + 2);
            
            JsonArray triangle = new JsonArray();
            triangle.add(p0Index);
            triangle.add(p1Index);
            triangle.add(p2Index);
            triangles.add(triangle);
        }
        
        json.add("points", points);
        json.add("triangles", triangles);
        
        return json;
    }
    
    public Vec2d getBarycenter() {
        double sumX = 0;
        double sumY = 0;
        double sumWeight = 0;
        for (int i = 0; i < getStoredTriangleNum(); i++) {
            if (!isTriangleValid(i)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(i * 3);
            int p1Index = trianglePointIndexes.getInt(i * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(i * 3 + 2);
            
            double p0x = pointCoords.getDouble(p0Index * 2);
            double p0y = pointCoords.getDouble(p0Index * 2 + 1);
            double p1x = pointCoords.getDouble(p1Index * 2);
            double p1y = pointCoords.getDouble(p1Index * 2 + 1);
            double p2x = pointCoords.getDouble(p2Index * 2);
            double p2y = pointCoords.getDouble(p2Index * 2 + 1);
            
            double weight = Helper.crossProduct2D(
                p1x - p0x, p1y - p0y,
                p2x - p0x, p2y - p0y
            );
            
            sumX += (p0x + p1x + p2x) * weight;
            sumY += (p0y + p1y + p2y) * weight;
            sumWeight += weight;
        }
        
        if (sumWeight == 0) {
            return new Vec2d(0, 0);
        }
        
        return new Vec2d((sumX / sumWeight) / 3, (sumY / sumWeight) / 3);
    }
    
    public static record Rect(double minX, double minY, double maxX, double maxY) {
    }
    
    public Rect getBoundingBox() {
        double minX = 0;
        double minY = 0;
        double maxX = 0;
        double maxY = 0;
        
        boolean firstPoint = true;
        for (int i = 0; i < getStoredPointNum(); i++) {
            if (!isPointUsed(i)) {
                continue;
            }
            double x = pointCoords.getDouble(i * 2);
            double y = pointCoords.getDouble(i * 2 + 1);
            
            if (firstPoint) {
                minX = x;
                minY = y;
                maxX = x;
                maxY = y;
                firstPoint = false;
            }
            else {
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        
        return new Rect(minX, minY, maxX, maxY);
    }
    
    public double getArea() {
        double sumAreaDoubled = 0;
        for (int i = 0; i < getStoredTriangleNum(); i++) {
            if (!isTriangleValid(i)) {
                continue;
            }
            
            int p0Index = trianglePointIndexes.getInt(i * 3);
            int p1Index = trianglePointIndexes.getInt(i * 3 + 1);
            int p2Index = trianglePointIndexes.getInt(i * 3 + 2);
            
            double p0x = pointCoords.getDouble(p0Index * 2);
            double p0y = pointCoords.getDouble(p0Index * 2 + 1);
            double p1x = pointCoords.getDouble(p1Index * 2);
            double p1y = pointCoords.getDouble(p1Index * 2 + 1);
            double p2x = pointCoords.getDouble(p2Index * 2);
            double p2y = pointCoords.getDouble(p2Index * 2 + 1);
            
            double triangleAreaDoubled = Helper.crossProduct2D(
                p1x - p0x, p1y - p0y,
                p2x - p0x, p2y - p0y
            );
            sumAreaDoubled += triangleAreaDoubled;
        }
        
        return sumAreaDoubled / 2;
    }
    
    public void subtractPolygon(ObjectArrayList<Vec2d> polygonVertexes) {
        // dissect the polygon into triangles using fan triangulation
        for (int i = 1; i < polygonVertexes.size() - 1; i++) {
            int p0Index = 0;
            int p1Index = i;
            int p2Index = i + 1;
            
            subtractTriangleFromMesh(
                polygonVertexes.get(p0Index).x(), polygonVertexes.get(p0Index).y(),
                polygonVertexes.get(p1Index).x(), polygonVertexes.get(p1Index).y(),
                polygonVertexes.get(p2Index).x(), polygonVertexes.get(p2Index).y()
            );
        }
    }
    
    public void transformPoints(Function<Vec2d, Vec2d> transform) {
        gridToPointIndex.clear();
        
        for (int i = 0; i < getStoredPointNum(); i++) {
            // Note: unused points are also transformed
            double x = pointCoords.getDouble(i * 2);
            double y = pointCoords.getDouble(i * 2 + 1);
            
            Vec2d transformed = transform.apply(new Vec2d(x, y));
            double transformedX = transformed.x();
            double transformedY = transformed.y();
            
            pointCoords.set(i * 2, transformedX);
            pointCoords.set(i * 2 + 1, transformedY);
            
            long newGridIndex = encodeToGrid(transformedX, transformedY);
            gridToPointIndex.put(newGridIndex, i);
        }
    }
    
    public void debugVisualize() {
        JsonObject jsonObject = toJson();
        
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        
        File file = new File("./misc/mesh_to_visualize.json");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.append(gson.toJson(jsonObject));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        try {
            // runs a python script that uses matplotlib
            // must install python and matplotlib
            Process process = Runtime.getRuntime().exec("python ./misc/visualize_mesh.py");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
