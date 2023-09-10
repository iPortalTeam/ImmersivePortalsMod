package qouteall.imm_ptl.core.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.BoxPredicate;

import java.util.Comparator;

@Environment(EnvType.CLIENT)
@OnlyIn(Dist.CLIENT)
public class FrustumCuller {
    // TODO replace with joml's frustum
    
    private BoxPredicate canDetermineInvisibleFunc;
    private double camX;
    private double camY;
    private double camZ;
    
    public FrustumCuller() {
    }
    
    public void update(double cameraX, double cameraY, double cameraZ) {
        canDetermineInvisibleFunc = getCanDetermineInvisibleFunc(cameraX, cameraY, cameraZ);
        camX = cameraX;
        camY = cameraY;
        camZ = cameraZ;
    }
    
    public boolean canDetermineInvisibleWithCameraCoord(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        return canDetermineInvisibleFunc.test(
            minX, minY, minZ, maxX, maxY, maxZ
        );
    }
    
    public boolean canDetermineInvisibleWithWorldCoord(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        return canDetermineInvisibleWithCameraCoord(
            minX - camX, minY - camY, minZ - camZ,
            maxX - camX, maxY - camY, maxZ - camZ
        );
    }
    
    private BoxPredicate getCanDetermineInvisibleFunc(
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (!IPCGlobal.doUseAdvancedFrustumCulling) {
            return BoxPredicate.nonePredicate;
        }
        if (IrisInterface.invoker.isRenderingShadowMap()) {
            return BoxPredicate.nonePredicate;
        }
        
        if (PortalRendering.isRendering()) {
            return PortalRendering.getRenderingPortal().getInnerFrustumCullingFunc(cameraX, cameraY, cameraZ);
        }
        else {
            if (!IPCGlobal.useSuperAdvancedFrustumCulling) {
                return BoxPredicate.nonePredicate;
            }
            
            Portal portal = getCurrentNearestVisibleCullablePortal();
            if (portal != null) {
                
                Vec3 portalOrigin = portal.getOriginPos();
                Vec3 portalOriginInLocalCoordinate = portalOrigin.add(
                    -cameraX, -cameraY, -cameraZ
                );
                final Vec3[] outerFrustumCullingVertices = portal.getOuterFrustumCullingVertices();
                if (outerFrustumCullingVertices == null) {
                    return BoxPredicate.nonePredicate;
                }
                Vec3[] downLeftUpRightPlaneNormals = getDownLeftUpRightPlaneNormals(
                    portalOriginInLocalCoordinate,
                    outerFrustumCullingVertices
                );
                
                Vec3 downPlane = downLeftUpRightPlaneNormals[0];
                Vec3 leftPlane = downLeftUpRightPlaneNormals[1];
                Vec3 upPlane = downLeftUpRightPlaneNormals[2];
                Vec3 rightPlane = downLeftUpRightPlaneNormals[3];
                
                Vec3 nearPlanePosInLocalCoordinate = portalOriginInLocalCoordinate;
                Vec3 nearPlaneNormal = portal.getNormal().scale(-1);
                
                return
                    (double minX, double minY, double minZ, double maxX, double maxY, double maxZ) -> {
                        boolean isBehindNearPlane = testBoxTwoVertices(
                            minX, minY, minZ, maxX, maxY, maxZ,
                            nearPlaneNormal.x, nearPlaneNormal.y, nearPlaneNormal.z,
                            nearPlanePosInLocalCoordinate.x,
                            nearPlanePosInLocalCoordinate.y,
                            nearPlanePosInLocalCoordinate.z
                        ) == BatchTestResult.all_true;
                        
                        if (!isBehindNearPlane) {
                            return false;
                        }
                        
                        boolean fullyInFrustum = isFullyInFrustum(
                            minX, minY, minZ, maxX, maxY, maxZ,
                            leftPlane, rightPlane, upPlane, downPlane
                        );
                        return fullyInFrustum;
                    };
            }
            else {
                return BoxPredicate.nonePredicate;
            }
        }
    }
    
    public static Vec3[] getDownLeftUpRightPlaneNormals(
        Vec3 portalOriginInLocalCoordinate,
        Vec3[] fourVertices
    ) {
        Vec3[] relativeVertices = {
            fourVertices[0].add(portalOriginInLocalCoordinate),
            fourVertices[1].add(portalOriginInLocalCoordinate),
            fourVertices[2].add(portalOriginInLocalCoordinate),
            fourVertices[3].add(portalOriginInLocalCoordinate)
        };
        
        //3  2
        //1  0
        return new Vec3[]{
            relativeVertices[0].cross(relativeVertices[1]),
            relativeVertices[1].cross(relativeVertices[3]),
            relativeVertices[3].cross(relativeVertices[2]),
            relativeVertices[2].cross(relativeVertices[0])
        };
    }
    
    public static enum BatchTestResult {
        all_true,
        all_false,
        both
    }
    
    public static BatchTestResult testBoxTwoVertices(
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        double planeNormalX, double planeNormalY, double planeNormalZ,
        double planePosX, double planePosY, double planePosZ
    ) {
        double p1x;
        double p1y;
        double p1z;
        double p2x;
        double p2y;
        double p2z;
        
        if (planeNormalX > 0) {
            p1x = minX;
            p2x = maxX;
        }
        else {
            p1x = maxX;
            p2x = minX;
        }
        
        if (planeNormalY > 0) {
            p1y = minY;
            p2y = maxY;
        }
        else {
            p1y = maxY;
            p2y = minY;
        }
        
        if (planeNormalZ > 0) {
            p1z = minZ;
            p2z = maxZ;
        }
        else {
            p1z = maxZ;
            p2z = minZ;
        }
        
        boolean r1 = isInFrontOf(
            p1x - planePosX, p1y - planePosY, p1z - planePosZ,
            planeNormalX, planeNormalY, planeNormalZ
        );
        
        boolean r2 = isInFrontOf(
            p2x - planePosX, p2y - planePosY, p2z - planePosZ,
            planeNormalX, planeNormalY, planeNormalZ
        );
        
        if (r1 && r2) {
            return BatchTestResult.all_true;
        }
        
        if ((!r1) && (!r2)) {
            return BatchTestResult.all_false;
        }
        
        return BatchTestResult.both;
    }
    
    public static BatchTestResult testBoxTwoVertices(
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        Vec3 planeNormal
    ) {
        return testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ,
            planeNormal.x, planeNormal.y, planeNormal.z,
            0, 0, 0
        );
    }
    
    private static boolean isInFrontOf(double x, double y, double z, Vec3 planeNormal) {
        return x * planeNormal.x + y * planeNormal.y + z * planeNormal.z >= 0;
    }
    
    private static boolean isInFrontOf(
        double x, double y, double z,
        double planeNormalX, double planeNormalY, double planeNormalZ
    ) {
        return x * planeNormalX + y * planeNormalY + z * planeNormalZ >= 0;
    }
    
    public static boolean isFullyOutsideFrustum(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        Vec3 leftPlane,
        Vec3 rightPlane,
        Vec3 upPlane,
        Vec3 downPlane
    ) {
        BatchTestResult left = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, leftPlane
        );
        BatchTestResult right = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, rightPlane
        );
        if (left == BatchTestResult.all_false && right == BatchTestResult.all_true) {
            return true;
        }
        if (left == BatchTestResult.all_true && right == BatchTestResult.all_false) {
            return true;
        }
        
        BatchTestResult up = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, upPlane
        );
        BatchTestResult down = testBoxTwoVertices(
            minX, minY, minZ, maxX, maxY, maxZ, downPlane
        );
        if (up == BatchTestResult.all_false && down == BatchTestResult.all_true) {
            return true;
        }
        if (up == BatchTestResult.all_true && down == BatchTestResult.all_false) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isFullyInFrustum(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
        Vec3 leftPlane,
        Vec3 rightPlane,
        Vec3 upPlane,
        Vec3 downPlane
    ) {
        return testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, leftPlane)
            == BatchTestResult.all_true
            && testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, rightPlane)
            == BatchTestResult.all_true
            && testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, upPlane)
            == BatchTestResult.all_true
            && testBoxTwoVertices(minX, minY, minZ, maxX, maxY, maxZ, downPlane)
            == BatchTestResult.all_true;
    }
    
    @Nullable
    private static Portal getCurrentNearestVisibleCullablePortal() {
        if (TransformationManager.isIsometricView) {
            return null;
        }
        
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return CHelper.getClientNearbyPortals(16).filter(
            portal -> portal.isInFrontOfPortal(cameraPos)
        ).filter(
            Portal::canDoOuterFrustumCulling
        ).min(
            Comparator.comparingDouble(portal -> portal.getDistanceToNearestPointInPortal(cameraPos))
        ).orElse(null);
    }
    
    public static boolean isTouchingInsideContentArea(Portal renderingPortal, AABB boundingBox) {
        Vec3 planeNormal = renderingPortal.getContentDirection();
        Vec3 planePos = renderingPortal.getDestPos();
        BatchTestResult batchTestResult = testBoxTwoVertices(
            boundingBox.minX, boundingBox.minY, boundingBox.minZ,
            boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ,
            planeNormal.x, planeNormal.y, planeNormal.z,
            planePos.x, planePos.y, planePos.z
        );
        return batchTestResult != BatchTestResult.all_false;
    }
}
