package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
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
        if (CGlobal.renderer.isRendering()) {
            portal = CGlobal.renderer.getRenderingPortal();
            
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
    
    private boolean isOutsidePortalFrustum(Box box) {
        if (!SGlobal.doUseAdvancedFrustumCulling) {
            return false;
        }
        
        Vec3d[] eightVertices = Helper.eightVerticesOf(box);
    
        Helper.BatchTestResult left = Helper.batchTest(
            eightVertices,
            point -> isInFrontOf(point, getLeftPlane())
        );
        Helper.BatchTestResult right = Helper.batchTest(
            eightVertices,
            point -> isInFrontOf(point, getRightPlane())
        );
        if (left == Helper.BatchTestResult.all_false && right == Helper.BatchTestResult.all_true) {
            return true;
        }
        if (left == Helper.BatchTestResult.all_true && right == Helper.BatchTestResult.all_false) {
            return true;
        }
    
        Helper.BatchTestResult up = Helper.batchTest(
            eightVertices,
            point -> isInFrontOf(point, getUpPlane())
        );
        Helper.BatchTestResult down = Helper.batchTest(
            eightVertices,
            point -> isInFrontOf(point, getDownPlane())
        );
        if (up == Helper.BatchTestResult.all_false && down == Helper.BatchTestResult.all_true) {
            return true;
        }
        if (up == Helper.BatchTestResult.all_true && down == Helper.BatchTestResult.all_false) {
            return true;
        }
    
        //this is problematic
//        BatchTestResult portalPlane = Helper.batchTest(
//            eightVertices,
//            point -> point
//                .subtract(portalDestInLocalCoordinate)
//                .dotProduct(portal.getNormal()) < 0 //true for inside portal area
//        );
//        if (portalPlane == BatchTestResult.all_false) {
//            return true;
//        }
    
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
        if (SGlobal.doUseAdvancedFrustumCulling) {
            if (CGlobal.renderer.isRendering()) {
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
