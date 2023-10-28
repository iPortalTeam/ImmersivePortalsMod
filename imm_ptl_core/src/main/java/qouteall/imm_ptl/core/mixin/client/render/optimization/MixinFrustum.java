package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.ducks.IEFrustum;
import qouteall.imm_ptl.core.render.FrustumCuller;

@Mixin(Frustum.class)
public class MixinFrustum implements IEFrustum {
    @Shadow
    private double camX;
    @Shadow
    private double camY;
    @Shadow
    private double camZ;
    
    @Shadow
    private Vector4f viewVector;
    
    /**
     * In {@link Frustum#offsetToFullyIncludeCameraCube(int)}
     * the camX, camY, camZ may get changed.
     * So we need to store the original value.
     * normal frustums can be moved back without wrongly culling anything
     * but the portal frustum may be tilted and moving it back may be wrong
     */
    private double portal_camX;
    private double portal_camY;
    private double portal_camZ;
    
    private FrustumCuller portal_frustumCuller;
    
    // copy the extra fields when copying frustum
    @Inject(
        method = "<init>(Lnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At("RETURN")
    )
    private void onFrustumCopy(Frustum other, CallbackInfo ci) {
        if (other instanceof IEFrustum) {
            MixinFrustum otherFrustum = (MixinFrustum) (Object) other;
            portal_camX = otherFrustum.portal_camX;
            portal_camY = otherFrustum.portal_camY;
            portal_camZ = otherFrustum.portal_camZ;
            portal_frustumCuller = otherFrustum.portal_frustumCuller;
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/culling/Frustum;prepare(DDD)V",
        at = @At("TAIL")
    )
    private void onSetOrigin(double double_1, double double_2, double double_3, CallbackInfo ci) {
        if (IrisInterface.invoker.isRenderingShadowMap()) {
            return;
        }
        
        if (portal_frustumCuller == null) {
            portal_frustumCuller = new FrustumCuller();
        }
        portal_frustumCuller.update(camX, camY, camZ);
        
        portal_camX = camX;
        portal_camY = camY;
        portal_camZ = camZ;
    }
    
    @Inject(
        method = "cubeInFrustum",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onCubeInFrustum(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (ip_canDetermineInvisibleWithCamCoord(
            (float) (minX - portal_camX),
            (float) (minY - portal_camY),
            (float) (minZ - portal_camZ),
            (float) (maxX - portal_camX),
            (float) (maxY - portal_camY),
            (float) (maxZ - portal_camZ)
        )) {
            cir.setReturnValue(false);
        }
    }
    
    // with scaling transformation, the view vector may be not unit-len
    @Inject(
        method = "calculateFrustum",
        at = @At("RETURN")
    )
    private void onCalculateFrustumReturn(
        Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci
    ) {
        viewVector.normalize();
    }
    
    @Override
    public boolean ip_canDetermineInvisibleWithCamCoord(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
    ) {
        return portal_frustumCuller.canDetermineInvisibleWithCameraCoord(
            minX, minY, minZ, maxX, maxY, maxZ
        );
    }
    
    @Override
    public Vec3 ip_getViewVec3() {
        return new Vec3(
            viewVector.x(),
            viewVector.y(),
            viewVector.z()
        );
    }
}
