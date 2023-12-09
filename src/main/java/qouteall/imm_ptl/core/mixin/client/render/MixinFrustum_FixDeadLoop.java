package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.LimitedLogger;

@Mixin(Frustum.class)
public abstract class MixinFrustum_FixDeadLoop {
    @Shadow
    private double camX;
    
    @Shadow
    private double camY;
    
    @Shadow
    private double camZ;
    
    @Shadow
    private Vector4f viewVector;
    
    @Shadow @Final private FrustumIntersection intersection;
    private static LimitedLogger limitedLogger = new LimitedLogger(10);
    
    /**
     * Make it to not deadloop when using isometric view.
     * Also make it to not deadloop even if the projection matrix is broken. (In normal cases the projection should not be broken.)
     *
     * @author qouteall
     * @reason Hard to do by injection or redirection
     */
    @Overwrite
    @IPVanillaCopy
    public Frustum offsetToFullyIncludeCameraCube(int gridSize) {
        if (TransformationManager.isIsometricView) {
            return (Frustum) (Object) this;
        }
        
        double minX = Math.floor(this.camX / (double) gridSize) * (double) gridSize;
        double minY = Math.floor(this.camY / (double) gridSize) * (double) gridSize;
        double minZ = Math.floor(this.camZ / (double) gridSize) * (double) gridSize;
        double maxX = Math.ceil(this.camX / (double) gridSize) * (double) gridSize;
        double maxY = Math.ceil(this.camY / (double) gridSize) * (double) gridSize;
        double maxZ = Math.ceil(this.camZ / (double) gridSize) * (double) gridSize;
        
        int countLimit = 10; // limit the loop count
        
        while (this.intersection.intersectAab((float) (minX - this.camX), (float) (minY - this.camY), (float) (minZ - this.camZ), (float) (maxX - this.camX), (float) (maxY - this.camY), (float) (maxZ - this.camZ))!= -2) {
            this.camX -= (double) (this.viewVector.x() * 4.0F);
            this.camY -= (double) (this.viewVector.y() * 4.0F);
            this.camZ -= (double) (this.viewVector.z() * 4.0F);
            countLimit--;
            if (countLimit <= 0) {
                limitedLogger.invoke(() -> {
                    Helper.err("the projection matrix and the frustum are abnormal");
                    new Throwable().printStackTrace();
                });
                break;
            }
        }
        
        return (Frustum) (Object) this;
    }
}
