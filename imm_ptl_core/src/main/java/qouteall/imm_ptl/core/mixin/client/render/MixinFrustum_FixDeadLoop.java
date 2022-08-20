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
    
    /**
     * The offsetToFullyIncludeCameraCube method moves back the frustum to make the nearest 8x8x8 cube
     * included in the frustum, to fix rare bugs of the visible section iteration.
     * It does culling in the visible section iteration but can only cull caves because the culling is very rough.
     *
     * With isometric view it will be stuck into deadloop.
     * However, without isometric, it still randomly stuck into deadloop when Pehkui is installed.
     * I still don't know the exact condition to reproduce the deadloop.
     * So simply limit the loop count for now.
     *
     * For a normal frustum matrix, m32 should not be 0
     *
     * @author qouteall
     * @reason Hard to do by injection or redirection
     */
    @Overwrite
    @IPVanillaCopy
    public Frustum offsetToFullyIncludeCameraCube(int gridSize) {
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
                break;
            }
        }

        return (Frustum) (Object) this;
    }
}
