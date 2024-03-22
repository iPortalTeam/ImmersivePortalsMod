package qouteall.q_misc_util.my_util;

import com.mojang.logging.LogUtils;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;
import org.slf4j.Logger;

@Testable
public class Mesh2DTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Test
    public void test() {
        long e1 = Mesh2D.encodeToGrid(1, -0.5);
        long e2 = Mesh2D.encodeToGrid(0, -0.5);
        Validate.isTrue(e1 != e2);
        
        Mesh2D mesh2D = new Mesh2D();
        mesh2D.enableTriangleLookup();
        
        mesh2D.addTriangle(
            0, 0, 1, 0, 0, 1
        );
        mesh2D.checkStorageIntegrity();
        
        mesh2D.addTriangle(
            0, 0, 1, 0, 0, -1
        );
        mesh2D.checkStorageIntegrity();
        
        mesh2D.simplify();
        mesh2D.checkStorageIntegrity();
        
        mesh2D.compact();
        mesh2D.checkStorageIntegrity();
        
        Validate.isTrue(mesh2D.getStoredTriangleNum() == 1);
        
        mesh2D.subtractTriangleFromMesh(
            0.5, 0, 0.5, 0.5, 0, 0
        );
        mesh2D.checkStorageIntegrity();
        
        mesh2D.simplify();
        mesh2D.checkStorageIntegrity();
        
        mesh2D.compact();
        mesh2D.checkStorageIntegrity();
        
        Validate.isTrue(mesh2D.getStoredTriangleNum() == 4);
        
        mesh2D = new Mesh2D();
        
        mesh2D.addTriangle(
            0, 0, 0, 1, -1, 0
        );
        mesh2D.addTriangle(
            0, 0, 0, 0.5, 1, 0
        );
        
        mesh2D.fixTJunction();
        mesh2D.compact();
        mesh2D.checkStorageIntegrity();
        
        Validate.isTrue(mesh2D.getStoredTriangleNum() == 3);
    }
    
    @Test
    public void testFixIntersection() throws InterruptedException {
        Mesh2D mesh2D = new Mesh2D();
        
        mesh2D.addTriangle(0, 0, 1, 0, 0, 1);
        mesh2D.addTriangle(0.5, 1, 0.5, -1, -0.2, 0);
        
//        mesh2D.debugVisualize();
        
        mesh2D.fixIntersectedTriangle();
        mesh2D.compact();
        mesh2D.checkStorageIntegrity();
        
        mesh2D.simplify();
        mesh2D.compact();
        mesh2D.checkStorageIntegrity();
        
//        mesh2D.debugVisualize();
    }
}
