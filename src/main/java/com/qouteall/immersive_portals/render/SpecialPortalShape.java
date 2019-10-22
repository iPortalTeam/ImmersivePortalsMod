package com.qouteall.immersive_portals.render;

import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class SpecialPortalShape {
    public static class TriangleInPlane {
        public int x1;
        public int y1;
        public int x2;
        public int y2;
        public int x3;
        public int y3;
        
        public TriangleInPlane(int x1, int y1, int x2, int y2, int x3, int y3) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
        }
    }
    
    public List<TriangleInPlane> triangles;
    
    public SpecialPortalShape() {
        triangles = new ArrayList<>();
    }
    
    public SpecialPortalShape(ListTag tag) {
    
    }
}
