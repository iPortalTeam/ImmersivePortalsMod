package qouteall.imm_ptl.core.portal;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.GeometryUtil;
import qouteall.q_misc_util.my_util.Mesh2D;

import java.util.ArrayList;
import java.util.List;

// TODO refactor in 1.20.2
public class GeometryPortalShape {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final int MAX_TRIANGLE_NUM = 10000;
    
    public static class TriangleInPlane {
        public double x1;
        public double y1;
        public double x2;
        public double y2;
        public double x3;
        public double y3;
        
        //counter clock wise
        public TriangleInPlane(double x1, double y1, double x2, double y2, double x3, double y3) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
            
            if (!isCounterClockWise()) {
                this.x2 = x3;
                this.y2 = y3;
                this.x3 = x2;
                this.y3 = y2;
                assert isCounterClockWise();
            }
        }
        
        public boolean isCounterClockWise() {
            return isOnLeftSideOfTheLine(
                x3, y3,
                x1, y1,
                x2, y2, 0
            );
        }
        
        public boolean isPointInTriangle(
            double x, double y
        ) {
            return lenientIsPointInTriangle(x, y, 0);
        }
        
        public boolean lenientIsPointInTriangle(double x, double y, double leniency) {
            assert isCounterClockWise();
            
            return isOnLeftSideOfTheLine(x, y, x1, y1, x2, y2, leniency) &&
                isOnLeftSideOfTheLine(x, y, x2, y2, x3, y3, leniency) &&
                isOnLeftSideOfTheLine(x, y, x3, y3, x1, y1, leniency);
        }
        
        public double getArea() {
            return Helper.crossProduct2D(
                x2 - x1, y2 - y1,
                x3 - x1, y3 - y1
            ) / -2.0;
        }
    }
    
    private static boolean isOnLeftSideOfTheLine(
        double x, double y,
        double x1, double y1,
        double x2, double y2,
        double leniency
    ) {
        double cross = Helper.crossProduct2D(
            x - x1, x2 - x1,
            y - y1, y2 - y1
        );
        if (leniency == 0) {
            return cross >= 0;
        }
        else {
            return cross >= -leniency * Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
        }
    }
    
    public List<TriangleInPlane> triangles;
    
    public boolean normalized = false;
    
    public GeometryPortalShape() {
        triangles = new ArrayList<>();
    }
    
    public GeometryPortalShape(List<TriangleInPlane> triangles) {
        this.triangles = triangles;
    }
    
    public GeometryPortalShape(ListTag tag) {
        triangles = new ArrayList<>();
        
        int size = tag.size();
        if (size % 6 != 0) {
            Helper.err("Bad Portal Shape Data " + tag);
            return;
        }
        
        int triangleNum = size / 6;
        
        for (int i = 0; i < triangleNum; i++) {
            triangles.add(new TriangleInPlane(
                tag.getDouble(i * 6 + 0),
                tag.getDouble(i * 6 + 1),
                tag.getDouble(i * 6 + 2),
                tag.getDouble(i * 6 + 3),
                tag.getDouble(i * 6 + 4),
                tag.getDouble(i * 6 + 5)
            ));
        }
    }
    
    public ListTag writeToTag() {
        ListTag tag = new ListTag();
        
        for (TriangleInPlane triangle : triangles) {
            tag.add(DoubleTag.valueOf(triangle.x1));
            tag.add(DoubleTag.valueOf(triangle.y1));
            tag.add(DoubleTag.valueOf(triangle.x2));
            tag.add(DoubleTag.valueOf(triangle.y2));
            tag.add(DoubleTag.valueOf(triangle.x3));
            tag.add(DoubleTag.valueOf(triangle.y3));
        }
        
        return tag;
    }
    
    public void addTriangleForRectangle(double x1, double y1, double x2, double y2) {
        triangles.add(new TriangleInPlane(
            x1, y1,
            x2, y1,
            x2, y2
        ));
        triangles.add(new TriangleInPlane(
            x2, y2,
            x1, y2,
            x1, y1
        ));
    }
    
    public void normalize(double width, double height) {
        if (width == 0 || height == 0) {
            LOGGER.error("Trying to normalize with width {} and height {}", width, height);
            return;
        }
        
        if (!normalized) {
            double halfWidth = width / 2;
            double halfHeight = height / 2;
            for (TriangleInPlane triangle : triangles) {
                triangle.x1 /= halfWidth;
                triangle.y1 /= halfHeight;
                triangle.x2 /= halfWidth;
                triangle.y2 /= halfHeight;
                triangle.x3 /= halfWidth;
                triangle.y3 /= halfHeight;
            }
            
            this.normalized = true;
        }
    }
    
    public boolean isValid() {
        if (triangles.isEmpty()) {
            return false;
        }
        
        return true;

//        return triangles.stream().allMatch(
//            triangleInPlane -> triangleInPlane.getArea() > 0.00001
//        );
    }
    
    public GeometryPortalShape getFlippedWithScaling(double scale) {
        List<TriangleInPlane> newTriangleList = triangles.stream()
            .map(triangle -> new TriangleInPlane(
                -triangle.x1 * scale,
                triangle.y1 * scale,
                -triangle.x2 * scale,
                triangle.y2 * scale,
                -triangle.x3 * scale,
                triangle.y3 * scale
            )).toList();
        GeometryPortalShape result = new GeometryPortalShape(newTriangleList);
        result.normalized = normalized;
        return result;
    }
    
    public Mesh2D toMesh() {
        Validate.isTrue(normalized);
        
        Mesh2D result = new Mesh2D();
        for (TriangleInPlane triangle : triangles) {
            result.addTriangle(
                triangle.x1, triangle.y1,
                triangle.x2, triangle.y2,
                triangle.x3, triangle.y3
            );
        }
        
        return result;
    }
    
    public static GeometryPortalShape fromMesh(Mesh2D mesh2D) {
        GeometryPortalShape result = new GeometryPortalShape();
        result.normalized = true;
        for (int i = 0; i < mesh2D.getStoredTriangleNum(); i++) {
            if (mesh2D.isTriangleValid(i)) {
                int pointIndex1 = mesh2D.trianglePointIndexes.getInt(i * 3);
                int pointIndex2 = mesh2D.trianglePointIndexes.getInt(i * 3 + 1);
                int pointIndex3 = mesh2D.trianglePointIndexes.getInt(i * 3 + 2);
                
                double x1 = mesh2D.pointCoords.getDouble(pointIndex1 * 2);
                double y1 = mesh2D.pointCoords.getDouble(pointIndex1 * 2 + 1);
                double x2 = mesh2D.pointCoords.getDouble(pointIndex2 * 2);
                double y2 = mesh2D.pointCoords.getDouble(pointIndex2 * 2 + 1);
                double x3 = mesh2D.pointCoords.getDouble(pointIndex3 * 2);
                double y3 = mesh2D.pointCoords.getDouble(pointIndex3 * 2 + 1);
                
                result.triangles.add(new TriangleInPlane(
                    x1, y1, x2, y2, x3, y3
                ));
                
                if (result.triangles.size() >= MAX_TRIANGLE_NUM) {
                    break;
                }
            }
        }
        
        return result;
    }
    
    public GeometryPortalShape simplified() {
        Mesh2D mesh2D = toMesh();
        mesh2D.simplify();
        return fromMesh(mesh2D);
    }
    
    public static GeometryPortalShape createDefault() {
        GeometryPortalShape result = new GeometryPortalShape();
        result.addTriangleForRectangle(-1, -1, 1, 1);
        result.normalized = true;
        return result;
    }
    
    public boolean boxIntersects(double minX, double minY, double maxX, double maxY) {
        return triangles.stream().anyMatch(t -> GeometryUtil.triangleIntersectsWithAABB(
            t.x1, t.y1, t.x2, t.y2, t.x3, t.y3,
            minX, minY, maxX, maxY
        ));
    }
}
