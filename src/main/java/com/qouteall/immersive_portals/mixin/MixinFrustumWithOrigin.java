package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.Globals;
import com.qouteall.immersive_portals.MyCommand;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.PlaneTestResult;
import net.minecraft.client.render.FrustumWithOrigin;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(FrustumWithOrigin.class)
public class MixinFrustumWithOrigin {
    @Shadow
    private double originX;
    @Shadow
    private double originY;
    @Shadow
    private double originZ;
    
    private Portal portal;
    
    //respectively down left up right
    private Vec3d[] normalOf4Planes;
    private Vec3d portalDestInLocalCoordinate;
    
    @Inject(
        method = "Lnet/minecraft/client/render/FrustumWithOrigin;setOrigin(DDD)V",
        at = @At("TAIL")
    )
    private void onSetOrigin(double double_1, double double_2, double double_3, CallbackInfo ci) {
        if (Globals.portalRenderManager.isRendering()) {
            portal = Globals.portalRenderManager.getRenderingPortal();
            
            portalDestInLocalCoordinate = portal.destination.add(-originX, -originY, -originZ);
            Vec3d[] fourVertices = portal.getFourVerticesRelativeToCenter(0);
            Vec3d portalCenter = portal.getPos();
            Vec3d[] relativeVertices = {
                fourVertices[0].add(portalDestInLocalCoordinate),
                fourVertices[1].add(portalDestInLocalCoordinate),
                fourVertices[2].add(portalDestInLocalCoordinate),
                fourVertices[3].add(portalDestInLocalCoordinate)
            };
            
            //3  2
            //1  0
            normalOf4Planes = new Vec3d[]{
                relativeVertices[0].crossProduct(relativeVertices[1]),
                relativeVertices[1].crossProduct(relativeVertices[3]),
                relativeVertices[3].crossProduct(relativeVertices[2]),
                relativeVertices[2].crossProduct(relativeVertices[0])
            };
        }
    }
    
    private Vec3d getDownPlane() {
        return normalOf4Planes[0];
    }
    
    private Vec3d getLeftPlane() {
        return normalOf4Planes[1];
    }
    
    private Vec3d getUpPlane() {
        return normalOf4Planes[2];
    }
    
    private Vec3d getRightPlane() {
        return normalOf4Planes[3];
    }
    
    private PlaneTestResult testFor8Points(Vec3d planeNormal, Vec3d[] points) {
        assert points.length == 8;
        boolean firstResult = isInFrontOf(points[0], planeNormal);
        for (int i = 1; i < 8; i++) {
            boolean thisResult = isInFrontOf(points[i], planeNormal);
            if (thisResult != firstResult) {
                return PlaneTestResult.front_and_back;
            }
        }
        return firstResult ? PlaneTestResult.all_front : PlaneTestResult.all_back;
    }
    
    private boolean isInFrontOf(Vec3d pos, Vec3d planeNormal) {
        return pos.dotProduct(planeNormal) > 0;
    }
    
    private boolean isPosInPortalViewFrustum(Vec3d pos) {
        if (pos.subtract(portalDestInLocalCoordinate).dotProduct(portal.getNormal()) > 0) {
            return false;
        }
        return Arrays.stream(normalOf4Planes).allMatch(
            normal -> normal.dotProduct(pos) > 0
        );
    }
    
    //this frustum culling algorithm will not fail when frustum is very small
    //I invented this algorithm(maybe re-invent)
    private boolean isOutsidePortalFrustum(Box box) {
        Vec3d[] eightVertices = Helper.eightVerticesOf(box);
        
        PlaneTestResult left = testFor8Points(getLeftPlane(), eightVertices);
        PlaneTestResult right = testFor8Points(getRightPlane(), eightVertices);
        if (left == PlaneTestResult.all_back && right == PlaneTestResult.all_front) {
            return true;
        }
        if (left == PlaneTestResult.all_front && right == PlaneTestResult.all_back) {
            return true;
        }
        
        PlaneTestResult up = testFor8Points(getUpPlane(), eightVertices);
        PlaneTestResult down = testFor8Points(getDownPlane(), eightVertices);
        if (up == PlaneTestResult.all_back && down == PlaneTestResult.all_front) {
            return true;
        }
        if (up == PlaneTestResult.all_front && down == PlaneTestResult.all_back) {
            return true;
        }
        
        return false;
    }
    
    @Inject(
        method = "Lnet/minecraft/client/render/FrustumWithOrigin;intersects(DDDDDD)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onIntersectionTest(
        double double_1,
        double double_2,
        double double_3,
        double double_4,
        double double_5,
        double double_6,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (MyCommand.doUseAdvancedFrustumCulling) {
            if (Globals.portalRenderManager.isRendering()) {
                Box boxInLocalCoordinate = new Box(
                    double_1,
                    double_2,
                    double_3,
                    double_4,
                    double_5,
                    double_6
                ).offset(
                    -originX,
                    -originY,
                    -originZ
                );
    
                if (isOutsidePortalFrustum(boxInLocalCoordinate)) {
                    cir.setReturnValue(false);
                    cir.cancel();
                }
                
                //then do vanilla frustum culling
            }
        }
        
        
    }
}
