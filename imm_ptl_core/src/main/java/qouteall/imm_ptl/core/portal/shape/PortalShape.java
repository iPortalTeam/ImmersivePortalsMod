package qouteall.imm_ptl.core.portal.shape;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectiond;
import org.joml.Vector2d;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Mesh2D;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.TriangleConsumer;

public interface PortalShape {
    public boolean isPlanar();
    
    public AABB getBoundingBox(
        UnilateralPortalState portalState
    );
    
    public double distanceToPortalShape(
        UnilateralPortalState portalState, Vec3 pos
    );
    
    public double signedDistanceForTeleportation(
        UnilateralPortalState portalState, Vec3 pos
    );
    
    public @Nullable Vec3 raytracePortalShapeByLocalPos(
        UnilateralPortalState portalState,
        Vec3 localFrom, Vec3 localTo, double leniency
    );
    
    public default @Nullable Vec3 raytracePortalShape(
        UnilateralPortalState portalState,
        Vec3 from, Vec3 to,
        double leniency
    ) {
        Vec3 localFrom = portalState.transformGlobalToLocal(from);
        Vec3 localTo = portalState.transformGlobalToLocal(to);
        
        Vec3 hit = raytracePortalShapeByLocalPos(
            portalState, localFrom, localTo, leniency
        );
        
        if (hit != null) {
            return portalState.transformLocalToGlobal(hit);
        }
        
        return null;
    }
    
    public @Nullable Plane getClipping(
        UnilateralPortalState portalState
    );
    
    public PortalShape getFlipped();
    
    public PortalShape getReverse();
    
    public boolean roughTestVisibility(
        UnilateralPortalState portalState,
        Vec3 cameraPos
    );
    
    public void renderViewAreaMesh(
        Vec3 portalOriginRelativeToCamera,
        UnilateralPortalState portalState,
        TriangleConsumer vertexOutput,
        boolean isGlobalPortal
    );
    
    public static final class RectangularShape implements PortalShape {
        public static final RectangularShape INSTANCE = new RectangularShape();
        
        @Override
        public boolean isPlanar() {
            return true;
        }
        
        @Override
        public AABB getBoundingBox(UnilateralPortalState portalState) {
            double thickness = 0.02;
            double halfW = portalState.width() / 2;
            double halfH = portalState.height() / 2;
            return Helper.boundingBoxOfPoints(
                new Vec3[]{
                    portalState.transformLocalToGlobal(halfW, halfH, thickness),
                    portalState.transformLocalToGlobal(halfW, -halfH, thickness),
                    portalState.transformLocalToGlobal(-halfW, halfH, thickness),
                    portalState.transformLocalToGlobal(-halfW, -halfH, thickness),
                    portalState.transformLocalToGlobal(halfW, halfH, -thickness),
                    portalState.transformLocalToGlobal(halfW, -halfH, -thickness),
                    portalState.transformLocalToGlobal(-halfW, halfH, -thickness),
                    portalState.transformLocalToGlobal(-halfW, -halfH, -thickness)
                }
            );
        }
        
        @Override
        public double distanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
            Vec3 localPos = portalState.transformGlobalToLocal(pos);
            
            double distToRec = Helper.getDistanceToRectangle(
                localPos.x(), localPos.y(),
                -portalState.width() / 2, -portalState.height() / 2,
                portalState.width() / 2, portalState.height() / 2
            );
            
            return Math.sqrt(distToRec * distToRec + localPos.z() * localPos.z());
        }
        
        @Override
        public double signedDistanceForTeleportation(UnilateralPortalState portalState, Vec3 pos) {
            Vec3 localPos = portalState.transformGlobalToLocal(pos);
            
            return localPos.z();
        }
        
        public @Nullable Vec3 raytracePortalShapeByLocalPos(
            UnilateralPortalState portalState,
            Vec3 localFrom, Vec3 localTo, double leniency
        ) {
            double width = portalState.width();
            double height = portalState.height();
            
            if (localFrom.y() > 0 && localTo.y() < 0) {
                double deltaY = localTo.y() - localFrom.y();
                
                // localFrom + (localTo - localFrom) * t = 0
                // localFrom.y() + deltaY * t = 0
                // t = -localFrom.y() / deltaY
                
                double t = -localFrom.y() / deltaY;
                Vec3 hit = localFrom.add(localTo.subtract(localFrom).scale(t));
                if (Math.abs(hit.x()) < width / 2 + leniency &&
                    Math.abs(hit.z()) < height / 2 + leniency
                ) {
                    return hit;
                }
            }
            
            return null;
        }
        
        // the plane's normal points to the "remaining" side
        @Override
        public Plane getClipping(UnilateralPortalState portalState) {
            return new Plane(
                portalState.position(),
                portalState.getNormal()
            );
        }
        
        @Override
        public PortalShape getFlipped() {
            return this;
        }
        
        @Override
        public PortalShape getReverse() {
            return this;
        }
        
        @Override
        public void renderViewAreaMesh(
            Vec3 portalOriginRelativeToCamera,
            UnilateralPortalState portalState,
            TriangleConsumer vertexOutput,
            boolean isGlobalPortal
        ) {
            final double w = Math.min(portalState.width(), 23333);
            final double h = Math.min(portalState.height(), 23333);
            
            Vec3 localXAxis = portalState.getAxisW().scale(w / 2);
            Vec3 localYAxis = portalState.getAxisH().scale(h / 2);
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput, portalOriginRelativeToCamera, localXAxis, localYAxis
            );
        }
        
        @Override
        public boolean roughTestVisibility(
            UnilateralPortalState portalState,
            Vec3 cameraPos
        ) {
            Vec3 localPos = portalState.transformGlobalToLocal(cameraPos);
            
            return localPos.z() > 0;
        }
    }
    
    public static final class SpecialFlatShape implements PortalShape {
        public final Mesh2D mesh;
        
        public SpecialFlatShape(Mesh2D mesh) {this.mesh = mesh;}
        
        @Override
        public boolean isPlanar() {
            return true;
        }
        
        @Override
        public AABB getBoundingBox(UnilateralPortalState portalState) {
            return RectangularShape.INSTANCE.getBoundingBox(portalState);
        }
        
        // it does not calculate "real" distance here
        @Override
        public double distanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
            return RectangularShape.INSTANCE.distanceToPortalShape(portalState, pos);
        }
        
        @Override
        public double signedDistanceForTeleportation(UnilateralPortalState portalState, Vec3 pos) {
            return RectangularShape.INSTANCE.signedDistanceForTeleportation(portalState, pos);
        }
        
        @Override
        public @Nullable Vec3 raytracePortalShapeByLocalPos(
            UnilateralPortalState portalState,
            Vec3 localFrom, Vec3 localTo, double leniency
        ) {
            Vec3 roughRayTrace = RectangularShape.INSTANCE.raytracePortalShapeByLocalPos(
                portalState, localFrom, localTo, leniency
            );
            
            if (roughRayTrace == null) {
                return null;
            }
            
            double localX = roughRayTrace.x();
            double localY = roughRayTrace.y();
            double halfWidth = portalState.width() / 2;
            double halfHeight = portalState.height() / 2;
            
            double boxR = Math.max(leniency, 0.00001);
            double nx = localX / halfWidth;
            double ny = localY / halfHeight;
            boolean intersectWithMesh = mesh.boxIntersects(
                nx - boxR, ny - boxR,
                nx + boxR, ny + boxR
            );
            
            if (intersectWithMesh) {
                return roughRayTrace;
            }
            else {
                return null;
            }
        }
        
        @Override
        public @Nullable Plane getClipping(UnilateralPortalState portalState) {
            return RectangularShape.INSTANCE.getClipping(portalState);
        }
        
        @Override
        public PortalShape getFlipped() {
            Mesh2D newMesh = new Mesh2D();
            
            for (int ti = 0; ti < mesh.getStoredTriangleNum(); ti++) {
                if (mesh.isTriangleValid(ti)) {
                    int p0Index = mesh.trianglePointIndexes.getInt(ti * 3);
                    int p1Index = mesh.trianglePointIndexes.getInt(ti * 3 + 1);
                    int p2Index = mesh.trianglePointIndexes.getInt(ti * 3 + 2);
                    
                    newMesh.addTriangle(
                        -mesh.pointCoords.getDouble(p0Index * 2),
                        mesh.pointCoords.getDouble(p0Index * 2 + 1),
                        -mesh.pointCoords.getDouble(p1Index * 2),
                        mesh.pointCoords.getDouble(p1Index * 2 + 1),
                        -mesh.pointCoords.getDouble(p2Index * 2),
                        mesh.pointCoords.getDouble(p2Index * 2 + 1)
                    );
                }
            }
            
            return new SpecialFlatShape(newMesh);
        }
        
        @Override
        public PortalShape getReverse() {
            return getFlipped();
        }
        
        @Override
        public void renderViewAreaMesh(
            Vec3 portalOriginRelativeToCamera,
            UnilateralPortalState portalState,
            TriangleConsumer vertexOutput,
            boolean isGlobalPortal
        ) {
            double halfWidth = portalState.width() / 2;
            double halfHeight = portalState.height() / 2;
            
            Vec3 localXAxis = portalState.getAxisW().scale(halfWidth);
            Vec3 localYAxis = portalState.getAxisH().scale(halfHeight);
            
            for (int ti = 0; ti < mesh.getStoredTriangleNum(); ti++) {
                if (mesh.isTriangleValid(ti)) {
                    int p0Index = mesh.getTrianglePointIndex(ti, 0);
                    int p1Index = mesh.getTrianglePointIndex(ti, 1);
                    int p2Index = mesh.getTrianglePointIndex(ti, 2);
                    
                    double p0x = mesh.getPointX(p0Index);
                    double p0y = mesh.getPointY(p0Index);
                    double p1x = mesh.getPointX(p1Index);
                    double p1y = mesh.getPointY(p1Index);
                    double p2x = mesh.getPointX(p2Index);
                    double p2y = mesh.getPointY(p2Index);
                    
                    ViewAreaRenderer.outputTriangle(
                        vertexOutput, portalOriginRelativeToCamera,
                        localXAxis, localYAxis, p0x, p0y, p1x, p1y, p2x, p2y
                    );
                }
            }
        }
        
        @Override
        public boolean roughTestVisibility(
            UnilateralPortalState portalState,
            Vec3 cameraPos
        ) {
            return RectangularShape.INSTANCE.roughTestVisibility(portalState, cameraPos);
        }
    }
    
    public static final class BoxShape implements PortalShape {
        
        public static final BoxShape FACING_OUTWARDS = new BoxShape(true);
        public static final BoxShape FACING_INWARDS = new BoxShape(false);
        
        public final boolean facingOutwards;
        
        public BoxShape(boolean facingOutwards) {this.facingOutwards = facingOutwards;}
        
        @Override
        public boolean isPlanar() {
            return false;
        }
        
        @Override
        public AABB getBoundingBox(UnilateralPortalState portalState) {
            double halfW = portalState.width() / 2;
            double halfH = portalState.height() / 2;
            double halfT = portalState.thickness() / 2;
            return Helper.boundingBoxOfPoints(
                new Vec3[]{
                    portalState.transformLocalToGlobal(-halfW, -halfH, -halfT),
                    portalState.transformLocalToGlobal(-halfW, -halfH, halfT),
                    portalState.transformLocalToGlobal(-halfW, halfH, -halfT),
                    portalState.transformLocalToGlobal(-halfW, halfH, halfT),
                    portalState.transformLocalToGlobal(halfW, -halfH, -halfT),
                    portalState.transformLocalToGlobal(halfW, -halfH, halfT),
                    portalState.transformLocalToGlobal(halfW, halfH, -halfT),
                    portalState.transformLocalToGlobal(halfW, halfH, halfT)
                }
            );
        }
        
        @Override
        public double distanceToPortalShape(UnilateralPortalState portalState, Vec3 pos) {
            Vec3 localPos = portalState.transformGlobalToLocal(pos);
            
            double dx = Helper.getDistanceToRange(
                -portalState.width() / 2, portalState.width() / 2, localPos.x()
            );
            double dy = Helper.getDistanceToRange(
                -portalState.height() / 2, portalState.height() / 2, localPos.y()
            );
            double dz = Helper.getDistanceToRange(
                -portalState.thickness() / 2, portalState.thickness() / 2, localPos.z()
            );
            
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        
        @Override
        public double signedDistanceForTeleportation(UnilateralPortalState portalState, Vec3 pos) {
            Vec3 localPos = portalState.transformGlobalToLocal(pos);
            
            double sx = Helper.getSignedDistanceToRange(
                -portalState.width() / 2, portalState.width() / 2, localPos.x()
            );
            double sy = Helper.getSignedDistanceToRange(
                -portalState.height() / 2, portalState.height() / 2, localPos.y()
            );
            double sz = Helper.getSignedDistanceToRange(
                -portalState.thickness() / 2, portalState.thickness() / 2, localPos.z()
            );
            
            double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sx < 0 && sy < 0 && sz < 0) {
                return -l;
            }
            else {
                return l;
            }
        }
        
        @Override
        public @Nullable Vec3 raytracePortalShapeByLocalPos(
            UnilateralPortalState portalState, Vec3 localFrom, Vec3 localTo, double leniency
        ) {
            Vector2d resultHolder = new Vector2d();
            int r = Intersectiond.intersectLineSegmentAab(
                localFrom.x(), localFrom.y(), localFrom.z(),
                localTo.x(), localTo.y(), localTo.z(),
                -portalState.width() / 2, -portalState.height() / 2, -portalState.thickness() / 2,
                portalState.width() / 2, portalState.height() / 2, portalState.thickness() / 2,
                resultHolder
            );
            
            if (r == Intersectiond.OUTSIDE) {
                return null;
            }
            
            double t = resultHolder.x;
            
            return localFrom.add(localTo.subtract(localFrom).scale(t));
        }
        
        @Override
        public @Nullable Plane getClipping(UnilateralPortalState portalState) {
            return null;
        }
        
        @Override
        public PortalShape getFlipped() {
            if (facingOutwards) {
                return FACING_INWARDS;
            }
            else {
                return FACING_OUTWARDS;
            }
        }
        
        @Override
        public PortalShape getReverse() {
            return getFlipped();
        }
        
        @Override
        public boolean roughTestVisibility(
            UnilateralPortalState portalState,
            Vec3 cameraPos
        ) {
            Vec3 localPos = portalState.transformGlobalToLocal(cameraPos);
            
            boolean in = localPos.x() > -portalState.width() / 2 &&
                localPos.x() < portalState.width() / 2 &&
                localPos.y() > -portalState.height() / 2 &&
                localPos.y() < portalState.height() / 2 &&
                localPos.z() > -portalState.thickness() / 2 &&
                localPos.z() < portalState.thickness() / 2;
            
            if (facingOutwards) {
                return in;
            }
            else {
                return !in;
            }
        }
        
        @Override
        public void renderViewAreaMesh(
            Vec3 portalOriginRelativeToCamera,
            UnilateralPortalState portalState,
            TriangleConsumer vertexOutput,
            boolean isGlobalPortal
        ) {
            Vec3 localHX = portalState.getAxisW().scale(portalState.width() / 2);
            Vec3 localHY = portalState.getAxisH().scale(portalState.height() / 2);
            Vec3 localHZ = portalState.getNormal().scale(portalState.thickness() / 2);
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput,
                portalOriginRelativeToCamera.add(localHX),
                facingOutwards ? localHY : localHZ,
                facingOutwards ? localHZ : localHY
            );
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput,
                portalOriginRelativeToCamera.subtract(localHX),
                facingOutwards ? localHZ : localHY,
                facingOutwards ? localHY : localHZ
            );
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput,
                portalOriginRelativeToCamera.add(localHY),
                facingOutwards ? localHZ : localHX,
                facingOutwards ? localHX : localHZ
            );
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput,
                portalOriginRelativeToCamera.subtract(localHY),
                facingOutwards ? localHX : localHZ,
                facingOutwards ? localHZ : localHX
            );
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput,
                portalOriginRelativeToCamera.add(localHZ),
                facingOutwards ? localHX : localHY,
                facingOutwards ? localHY : localHX
            );
            
            ViewAreaRenderer.outputFullQuad(
                vertexOutput,
                portalOriginRelativeToCamera.subtract(localHZ),
                facingOutwards ? localHY : localHX,
                facingOutwards ? localHX : localHY
            );
        }
    }
}
