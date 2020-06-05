package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.render.FrustumCuller;
import net.minecraft.client.render.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class MixinFrustum {
    @Shadow
    private double x;
    @Shadow
    private double y;
    @Shadow
    private double z;
    
    private FrustumCuller portal_frustumCuller;
    
    @Inject(
        method = "setPosition",
        at = @At("TAIL")
    )
    private void onSetOrigin(double double_1, double double_2, double double_3, CallbackInfo ci) {
        if (portal_frustumCuller == null) {
            portal_frustumCuller = new FrustumCuller();
        }
        portal_frustumCuller.update(x, y, z);
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/Frustum;isVisible(DDDDDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIntersectionTest(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (CGlobal.doUseAdvancedFrustumCulling) {
            boolean canDetermineInvisible = portal_frustumCuller.canDetermineInvisible(
                minX - x, minY - y, minZ - z,
                maxX - x, maxY - y, maxZ - z
            );
            if (canDetermineInvisible) {
                cir.setReturnValue(false);
            }
        }
    }
    
}
