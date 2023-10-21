package qouteall.imm_ptl.core.portal;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.q_misc_util.my_util.Mesh2D;

// TODO remove in 1.20.3
public class GeometryPortalShape {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final int MAX_TRIANGLE_NUM = 10000;
    
    public final @NotNull Mesh2D mesh;
    
    public GeometryPortalShape(@NotNull Mesh2D mesh) {
        this.mesh = mesh;
        mesh.enableTriangleLookup();
    }
    
    public static @Nullable GeometryPortalShape fromTag(ListTag tag) {
        int size = tag.size();
        if (size % 6 != 0) {
            LOGGER.error("Invalid Portal Shape Data {}", tag);
            return null;
        }
        
        Mesh2D mesh = new Mesh2D();
        
        int triangleNum = size / 6;
        
        triangleNum = Math.min(triangleNum, MAX_TRIANGLE_NUM);
        
        for (int i = 0; i < triangleNum; i++) {
            mesh.addTriangle(
                tag.getDouble(i * 6 + 0),
                tag.getDouble(i * 6 + 1),
                tag.getDouble(i * 6 + 2),
                tag.getDouble(i * 6 + 3),
                tag.getDouble(i * 6 + 4),
                tag.getDouble(i * 6 + 5)
            );
        }
        
        if (mesh.getStoredTriangleNum() == 0) {
            return null;
        }
        
        return new GeometryPortalShape(mesh);
    }
    
    // in the old portal data, the coordinates are not normalized
    public static @Nullable GeometryPortalShape fromTagOldFormat(
        ListTag tag, double halfWidth, double halfHeight
    ) {
        int size = tag.size();
        if (size % 6 != 0) {
            LOGGER.error("Invalid Portal Shape Data {}", tag);
            return null;
        }
        
        Mesh2D mesh = new Mesh2D();
        
        int triangleNum = size / 6;
        
        triangleNum = Math.min(triangleNum, MAX_TRIANGLE_NUM);
        
        for (int i = 0; i < triangleNum; i++) {
            mesh.addTriangle(
                tag.getDouble(i * 6 + 0) / halfWidth,
                tag.getDouble(i * 6 + 1) / halfHeight,
                tag.getDouble(i * 6 + 2) / halfWidth,
                tag.getDouble(i * 6 + 3) / halfHeight,
                tag.getDouble(i * 6 + 4) / halfWidth,
                tag.getDouble(i * 6 + 5) / halfHeight
            );
        }
        
        if (mesh.getStoredTriangleNum() == 0) {
            return null;
        }
        
        return new GeometryPortalShape(mesh);
    }
    
    public ListTag writeToTag() {
        ListTag tag = new ListTag();
        
        for (int ti = 0; ti < mesh.getStoredTriangleNum(); ti++) {
            if (mesh.isTriangleValid(ti)) {
                int p0Index = mesh.trianglePointIndexes.getInt(ti * 3);
                int p1Index = mesh.trianglePointIndexes.getInt(ti * 3 + 1);
                int p2Index = mesh.trianglePointIndexes.getInt(ti * 3 + 2);
                
                tag.add(DoubleTag.valueOf(mesh.pointCoords.getDouble(p0Index * 2)));
                tag.add(DoubleTag.valueOf(mesh.pointCoords.getDouble(p0Index * 2 + 1)));
                tag.add(DoubleTag.valueOf(mesh.pointCoords.getDouble(p1Index * 2)));
                tag.add(DoubleTag.valueOf(mesh.pointCoords.getDouble(p1Index * 2 + 1)));
                tag.add(DoubleTag.valueOf(mesh.pointCoords.getDouble(p2Index * 2)));
                tag.add(DoubleTag.valueOf(mesh.pointCoords.getDouble(p2Index * 2 + 1)));
            }
        }
        
        return tag;
    }
    
    public void addTriangleForRectangle(double x1, double y1, double x2, double y2) {
        mesh.addTriangle(x1, y1, x2, y1, x2, y2);
        mesh.addTriangle(x2, y2, x1, y2, x1, y1);
    }
    
    /**
     * Flips the X coord
     */
    public GeometryPortalShape getFlipped() {
        Mesh2D newMesh = new Mesh2D();
        
        for (int ti = 0; ti < mesh.getStoredTriangleNum(); ti++) {
            if (mesh.isTriangleValid(ti)) {
                int p0Index = mesh.trianglePointIndexes.getInt(ti * 3);
                int p1Index = mesh.trianglePointIndexes.getInt(ti * 3 + 1);
                int p2Index = mesh.trianglePointIndexes.getInt(ti * 3 + 2);
                
                newMesh.addTriangle(
                    -mesh.pointCoords.getDouble(p0Index * 2),
                    mesh.pointCoords.getDouble(p0Index * 2 + 1),
                    -mesh.pointCoords.getDouble(p1Index * 2),
                    mesh.pointCoords.getDouble(p1Index * 2 + 1),
                    -mesh.pointCoords.getDouble(p2Index * 2),
                    mesh.pointCoords.getDouble(p2Index * 2 + 1)
                );
            }
        }
        
        return new GeometryPortalShape(newMesh);
    }
    
    public static GeometryPortalShape createDefault() {
        GeometryPortalShape result = new GeometryPortalShape(new Mesh2D());
        result.addTriangleForRectangle(-1, -1, 1, 1);
        return result;
    }
    
    /**
     * Note: give normalized coord
     */
    public boolean boxIntersects(double minX, double minY, double maxX, double maxY) {
        return mesh.boxIntersects(minX, minY, maxX, maxY);
    }
    
    public GeometryPortalShape copy() {
        return new GeometryPortalShape(mesh.copy());
    }
}
