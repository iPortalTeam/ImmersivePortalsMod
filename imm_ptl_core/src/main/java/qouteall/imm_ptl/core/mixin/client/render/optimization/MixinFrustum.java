package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.culling.Frustum;
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
            minX - camX, minY - camY, minZ - camZ,
            maxX - camX, maxY - camY, maxZ - camZ
        );
    }
}
