package qouteall.q_misc_util.my_util;

import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

@Testable
public class Mesh2DTest {
    @Test
    public void test() {
        Mesh2D mesh2D = new Mesh2D();
        
        mesh2D.addTriangle(
            0, 0, 1, 0, 0, 1
        );
        
        mesh2D.addTriangle(
            0, 0, 1, 0, 0, -1
        );
        
        mesh2D.simplify(Integer.MAX_VALUE);
        
        mesh2D.compact();
        
        Validate.isTrue(mesh2D.getStoredTriangleNum() == 1);
        
        mesh2D.subtractTriangle(
            0.5, 0, 0.5, 0.5, 0, 0
        );
        
        mesh2D.simplify(Integer.MAX_VALUE);
        mesh2D.compact();
        
        Validate.isTrue(mesh2D.getStoredTriangleNum() == 4);
    }
}
