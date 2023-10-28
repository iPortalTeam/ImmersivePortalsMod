package qouteall.imm_ptl.core.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPCGlobal;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.q_misc_util.my_util.BoxPredicateF;
import qouteall.q_misc_util.my_util.Plane;

import java.util.Comparator;

@Environment(EnvType.CLIENT)
public class FrustumCuller {
    private @Nullable BoxPredicateF canDetermineInvisibleFunc;
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
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
    ) {
        if (canDetermineInvisibleFunc == null) {
            return false;
        }
        
        return canDetermineInvisibleFunc.test(
            minX, minY, minZ,
            maxX, maxY, maxZ
        );
    }
    
    public boolean canDetermineInvisibleWithCameraCoord(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        if (canDetermineInvisibleFunc == null) {
            return false;
        }
        
        return canDetermineInvisibleFunc.test(
            (float) minX, (float) minY, (float) minZ,
            (float) maxX, (float) maxY, (float) maxZ
        );
    }
    
    public boolean canDetermineInvisibleWithWorldCoord(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        if (canDetermineInvisibleFunc == null) {
            return false;
        }
        
        return canDetermineInvisibleWithCameraCoord(
            minX - camX, minY - camY, minZ - camZ,
            maxX - camX, maxY - camY, maxZ - camZ
        );
    }
    
    private @Nullable BoxPredicateF getCanDetermineInvisibleFunc(
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (!IPCGlobal.doUseAdvancedFrustumCulling) {
            return null;
        }
        if (IrisInterface.invoker.isRenderingShadowMap()) {
            return null;
        }
        
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            
            // do inner frustum culling
            
            if (renderingPortal instanceof Portal portal) {
                return portal.getPortalShape().getInnerFrustumCullingFunc(
                    portal, new Vec3(cameraX, cameraY, cameraZ)
                );
            }
        }
        else {
            if (!IPCGlobal.useSuperAdvancedFrustumCulling) {
                return null;
            }
            
            // do outer frustum culling
            
            Portal portal = getCurrentNearestVisibleCullablePortal();
            if (portal != null) {
                return portal.getPortalShape().getOuterFrustumCullingFunc(
                    portal, new Vec3(cameraX, cameraY, cameraZ)
                );
            }
        }
        
        return null;
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
    
    // 2  1
    // 3  0
    public static Vec3[] getRectPortalFourVerticesCounterClockwise(
        UnilateralPortalState thisSideState
    ) {
        double halfWidth = thisSideState.width() / 2;
        double halfHeight = thisSideState.height() / 2;
        return new Vec3[]{
            thisSideState.transformLocalToGlobal(halfWidth, -halfHeight, 0),
            thisSideState.transformLocalToGlobal(halfWidth, halfHeight, 0),
            thisSideState.transformLocalToGlobal(-halfWidth, halfHeight, 0),
            thisSideState.transformLocalToGlobal(-halfWidth, -halfHeight, 0)
        };
    }
    
    public static Frustum4Planes getFrustumPlanesFromFourVerticesCounterClockwise(
        Vec3[] vertices
    ) {
        // 2  1
        // 3  0
        Vec3 normal0 = vertices[1].cross(vertices[0]).normalize();
        Vec3 normal1 = vertices[2].cross(vertices[1]).normalize();
        Vec3 normal2 = vertices[3].cross(vertices[2]).normalize();
        Vec3 normal3 = vertices[0].cross(vertices[3]).normalize();
        
        // assume that the plane origin is in the coordinate origin, so W is 0
        return new Frustum4Planes(
            (float) normal0.x, (float) normal0.y, (float) normal0.z, 0,
            (float) normal1.x, (float) normal1.y, (float) normal1.z, 0,
            (float) normal2.x, (float) normal2.y, (float) normal2.z, 0,
            (float) normal3.x, (float) normal3.y, (float) normal3.z, 0
        );
    }
    
    // the frustum culling func returning true means culled
    public static BoxPredicateF getFlatPortalInnerFrustumCullingFunc(
        Portal portal, Vec3 cameraPos
    ) {
        Vec3[] v = getRectPortalFourVerticesCounterClockwise(portal.getThisSideState());
        
        // 2  1
        // 3  0
        Vec3[] vTransformed = new Vec3[]{
            portal.transformPoint(v[0]).subtract(cameraPos),
            portal.transformPoint(v[1]).subtract(cameraPos),
            portal.transformPoint(v[2]).subtract(cameraPos),
            portal.transformPoint(v[3]).subtract(cameraPos)
        };
        
        Frustum4Planes fourPlanes =
            getFrustumPlanesFromFourVerticesCounterClockwise(vTransformed);
        
        // to cull out, it must be fully outside portal frustum
        return fourPlanes::isFullyOutside;
    }
    
    // the frustum culling func returning true means culled
    public static BoxPredicateF getFlatPortalOuterFrustumCullingFunc(
        Portal portal, Vec3 cameraPos
    ) {
        Vec3[] v = getRectPortalFourVerticesCounterClockwise(portal.getThisSideState());
        
        Vec3[] vTransformed = new Vec3[]{
            v[0].subtract(cameraPos),
            v[1].subtract(cameraPos),
            v[2].subtract(cameraPos),
            v[3].subtract(cameraPos)
        };
        
        Frustum4Planes fourPlanes =
            getFrustumPlanesFromFourVerticesCounterClockwise(vTransformed);
        
        Plane portalPlane = new Plane(portal.getOriginPos(), portal.getNormal());
        
        float portalPlaneX = (float) portalPlane.getEquationX();
        float portalPlaneY = (float) portalPlane.getEquationY();
        float portalPlaneZ = (float) portalPlane.getEquationZ();
        float portalPlaneW = (float) portalPlane.getEquationW();
        
        // to cull out, it must be fully behind portal plane
        // and inside portal frustum
        return (float minX, float minY, float minZ, float maxX, float maxY, float maxZ) -> {
            return isFullyBehindPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                portalPlaneX, portalPlaneY, portalPlaneZ, portalPlaneW
            ) && fourPlanes.isFullyOutside(
                minX, minY, minZ, maxX, maxY, maxZ
            );
        };
    }
    
    // (p - origin) * normal > 0
    // p * normal - origin * normal > 0
    // planeX * x + planeY * y + planeZ * z + planeW > 0
    // planeX = normal.x  planeY = normal.y  planeZ = normal.z  planeW = -origin * normal
    public static boolean isFullyInFrontOfPlane(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        float planeX, float planeY, float planeZ, float planeW
    ) {
        float testingBoxX = planeX > 0 ? minX : maxX;
        float testingBoxY = planeY > 0 ? minY : maxY;
        float testingBoxZ = planeZ > 0 ? minZ : maxZ;
        
        return testingBoxX * planeX + testingBoxY * planeY + testingBoxZ * planeZ + planeW > 0;
    }
    
    public static boolean isFullyBehindPlane(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        float planeX, float planeY, float planeZ, float planeW
    ) {
        float testingBoxX = planeX > 0 ? maxX : minX;
        float testingBoxY = planeY > 0 ? maxY : minY;
        float testingBoxZ = planeZ > 0 ? maxZ : minZ;
        
        return testingBoxX * planeX + testingBoxY * planeY + testingBoxZ * planeZ + planeW < 0;
    }
    
    public record Frustum4Planes(
        float p0X, float p0Y, float p0Z, float p0W,
        float p1X, float p1Y, float p1Z, float p1W,
        float p2X, float p2Y, float p2Z, float p2W,
        float p3X, float p3Y, float p3Z, float p3W
    ) {
        public boolean isFullyOutside(
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ
        ) {
            return isFullyBehindPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p0X, p0Y, p0Z, p0W
            ) || isFullyBehindPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p1X, p1Y, p1Z, p1W
            ) || isFullyBehindPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p2X, p2Y, p2Z, p2W
            ) || isFullyBehindPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p3X, p3Y, p3Z, p3W
            );
        }
        
        public boolean isFullyInside(
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ
        ) {
            return isFullyInFrontOfPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p0X, p0Y, p0Z, p0W
            ) && isFullyInFrontOfPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p1X, p1Y, p1Z, p1W
            ) && isFullyInFrontOfPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p2X, p2Y, p2Z, p2W
            ) && isFullyInFrontOfPlane(
                minX, minY, minZ, maxX, maxY, maxZ,
                p3X, p3Y, p3Z, p3W
            );
        }
    }
    
    
    @Deprecated
    public static enum BatchTestResult {
        all_true,
        all_false,
        both
    }
    
    @Deprecated
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
    
    @Deprecated
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
    
    @Deprecated
    private static boolean isInFrontOf(
        double x, double y, double z,
        double planeNormalX, double planeNormalY, double planeNormalZ
    ) {
        return x * planeNormalX + y * planeNormalY + z * planeNormalZ >= 0;
    }
}
