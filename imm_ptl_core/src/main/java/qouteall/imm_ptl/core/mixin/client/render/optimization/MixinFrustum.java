package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPCGlobal;
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
    
    @Shadow private Vector4f viewVector;
    private double portal_camX;
    private double portal_camY;
    private double portal_camZ;
    
    private FrustumCuller portal_frustumCuller;
    
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
        
        // the camX, camY, camZ may get changed in offsetToFullyIncludeCameraCube()
        // normal frustums can be moved back without wrongly culling anything
        // but the portal frustum may be tilted and moving it back may be wrong
        portal_camX = camX;
        portal_camY = camY;
        portal_camZ = camZ;
    }
    
    
    @Inject(
        method = "Lnet/minecraft/client/renderer/culling/Frustum;cubeInFrustum(DDDDDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIntersectionTest(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (IPCGlobal.doUseAdvancedFrustumCulling) {
            boolean canDetermineInvisible = canDetermineInvisible(minX, minY, minZ, maxX, maxY, maxZ);
            if (canDetermineInvisible) {
                cir.setReturnValue(false);
            }
        }
    }
    
    @Override
    public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (portal_frustumCuller == null) {
            return false;
        }
        return portal_frustumCuller.canDetermineInvisibleWithCameraCoord(
            minX - portal_camX, minY - portal_camY, minZ - portal_camZ,
            maxX - portal_camX, maxY - portal_camY, maxZ - portal_camZ
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
