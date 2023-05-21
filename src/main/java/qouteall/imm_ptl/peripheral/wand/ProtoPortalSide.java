package qouteall.imm_ptl.peripheral.wand;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.Circle;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;

/**
 * The proto-portal's one-side when using portal wand to create new portal.
 * Will be serialized to JSON (require type adapter for dimension id).
 */
public class ProtoPortalSide {
    @NotNull
    public ResourceKey<Level> dimension;
    
    @NotNull
    public Vec3 leftBottom;
    
    @Nullable
    public Vec3 rightBottom;
    
    @Nullable
    public Vec3 leftTop;
    
    public ProtoPortalSide(@NotNull ResourceKey<Level> dimension, @NotNull Vec3 leftBottom) {
        this.leftBottom = leftBottom;
        this.dimension = dimension;
    }
    
    public ProtoPortalSide copy() {
        ProtoPortalSide newPortal = new ProtoPortalSide(dimension, leftBottom);
        newPortal.rightBottom = rightBottom;
        newPortal.leftTop = leftTop;
        return newPortal;
    }
    
    public Vec3 getHorizontalAxis() {
        assert rightBottom != null;
        return rightBottom.subtract(leftBottom);
    }
    
    public Vec3 getVerticalAxis() {
        assert leftTop != null;
        return leftTop.subtract(leftBottom);
    }
    
    public Vec3 getNormal() {
        return getHorizontalAxis().cross(getVerticalAxis()).normalize();
    }
    
    public double getWidth() {
        return getHorizontalAxis().length();
    }
    
    public double getHeight() {
        return getVerticalAxis().length();
    }
    
    public double getHeightDivWidth() {
        double width = getWidth();
        double height = getHeight();
        
        if (width == 0) {
            return 0;
        }
        
        return height / width;
    }
    
    @Nullable
    public ProtoPortalSide undo() {
        if (leftTop != null) {
            leftTop = null;
            return this;
        }
        if (rightBottom != null) {
            rightBottom = null;
            return this;
        }
        return null;
    }
    
    public boolean isComplete() {
        return leftTop != null;
    }
    
    @Nullable
    public WithDim<Plane> getCursorConstraintPlane() {
        if (isComplete()) {
            return null;
        }
        if (rightBottom == null) {
            return null;
        }
        return new WithDim<>(dimension, new Plane(leftBottom, getHorizontalAxis()));
    }
    
    @Nullable
    public WithDim<Circle> getCursorConstraintCircle(double heightDivWidth) {
        if (isComplete()) {
            return null;
        }
        
        if (rightBottom == null) {
            return null;
        }
        
        WithDim<Plane> plane = getCursorConstraintPlane();
        Validate.notNull(plane);
        
        Vec3 horizontalAxis = getHorizontalAxis();
        double width = horizontalAxis.length();
        
        double constraintHeight = width * heightDivWidth;
        
        return new WithDim<>(dimension, new Circle(plane.value(), leftBottom, constraintHeight));
    }
    
    public boolean isValidPlacement(@Nullable Double heightDivWidth) {
        if (rightBottom != null) {
            double width = getHorizontalAxis().length();
    
            if (width < 0.001 || width > 64.001) {
                return false;
            }
            
            if (leftTop != null) {
                double height = getVerticalAxis().length();
                
                if (height < 0.001 || height > 64.001) {
                    return false;
                }
            }
            else if (heightDivWidth != null) {
                double height = width * heightDivWidth;
                
                if (height < 0.001 || height > 64.001) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public void placeCursor(Vec3 pos) {
        if (rightBottom == null) {
            rightBottom = pos;
        }
        else if (leftTop == null) {
            leftTop = pos;
        }
        else {
            throw new IllegalStateException();
        }
    }
}
