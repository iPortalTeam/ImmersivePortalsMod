package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.math.Vector4f;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
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
    
    @Shadow
    protected abstract boolean cubeCompletelyInFrustum(float f, float g, float h, float i, float j, float k);
    
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
        
        while (!this.cubeCompletelyInFrustum((float) (minX - this.camX), (float) (minY - this.camY), (float) (minZ - this.camZ), (float) (maxX - this.camX), (float) (maxY - this.camY), (float) (maxZ - this.camZ))) {
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
