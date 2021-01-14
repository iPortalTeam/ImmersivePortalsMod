package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class GeometryPortalShape {
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
                x2, y2
            );
        }
        
        public boolean isPointInTriangle(
            double x, double y
        ) {
            assert isCounterClockWise();
            
            return isOnLeftSideOfTheLine(x, y, x1, y1, x2, y2) &&
                isOnLeftSideOfTheLine(x, y, x2, y2, x3, y3) &&
                isOnLeftSideOfTheLine(x, y, x3, y3, x1, y1);
        }
        
        public double getArea() {
            return crossProduct2D(
                x2 - x1, y2 - y1,
                x3 - x1, y3 - y1
            ) / 2;
        }
    }
    
    private static boolean isOnLeftSideOfTheLine(
        double x, double y,
        double x1, double y1,
        double x2, double y2
    ) {
        return crossProduct2D(
            x - x1, x2 - x1,
            y - y1, y2 - y1
        ) >= 0;
    }
    
    //positive if it's rotating counter clock wise
    private static double crossProduct2D(
        double x1, double y1,
        double x2, double y2
    ) {
        return x1 * y2 - x2 * y1;
    }
    
    public List<TriangleInPlane> triangles;
    
    public GeometryPortalShape() {
        triangles = new ArrayList<>();
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
            tag.add(DoubleTag.of(triangle.x1));
            tag.add(DoubleTag.of(triangle.y1));
            tag.add(DoubleTag.of(triangle.x2));
            tag.add(DoubleTag.of(triangle.y2));
            tag.add(DoubleTag.of(triangle.x3));
            tag.add(DoubleTag.of(triangle.y3));
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
    
    public boolean isValid() {
        if (triangles.isEmpty()) {
            return false;
        }
        
        return triangles.stream().allMatch(
            triangleInPlane -> triangleInPlane.getArea() > 0.001
        );
    }
}
