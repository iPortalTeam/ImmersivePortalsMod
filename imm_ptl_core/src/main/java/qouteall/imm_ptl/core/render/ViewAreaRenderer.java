package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.GeometryPortalShape;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.my_util.Mesh2D;
import qouteall.q_misc_util.my_util.TriangleConsumer;

public class ViewAreaRenderer {
    
    public static void renderPortalArea(
        PortalRenderable portalRenderable, Vec3 fogColor,
        Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
        boolean doFaceCulling, boolean doModifyColor,
        boolean doModifyDepth, boolean doClip
    ) {
        PortalLike portalLike = portalRenderable.getPortalLike();
        
        if (doFaceCulling) {
            GlStateManager._enableCull();
        }
        else {
            GlStateManager._disableCull();
        }
        
        if (portalLike.isFuseView() && IPGlobal.maxPortalLayer != 0) {
            GlStateManager._colorMask(false, false, false, false);
        }
        else {
            if (!doModifyColor) {
                GlStateManager._colorMask(false, false, false, false);
            }
            else {
                GlStateManager._colorMask(true, true, true, true);
            }
        }
        
        if (doModifyDepth) {
            if (portalLike.isFuseView()) {
                GlStateManager._depthMask(false);
            }
            else {
                GlStateManager._depthMask(true);
            }
        }
        else {
            GlStateManager._depthMask(false);
        }
        
        boolean shouldReverseCull = PortalRendering.isRenderingOddNumberOfMirrors();
        if (shouldReverseCull) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
        
        if (doClip) {
            if (PortalRendering.isRendering()) {
                FrontClipping.setupInnerClipping(
                    PortalRendering.getActiveClippingPlane(),
                    modelViewMatrix, 0  // don't do adjustment
                );
            }
        }
        else {
            FrontClipping.disableClipping();
        }
        
        GlStateManager._enableDepthTest();
        
        CHelper.enableDepthClamp();
        
        ShaderInstance shader = MyRenderHelper.portalAreaShader;
        RenderSystem.setShader(() -> shader);
        
        shader.MODEL_VIEW_MATRIX.set(modelViewMatrix);
        shader.PROJECTION_MATRIX.set(projectionMatrix);
        
        FrontClipping.updateClippingEquationUniformForCurrentShader(false);
        
        shader.apply();
        
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portalRenderable,
            CHelper.getCurrentCameraPos(),
            RenderStates.getPartialTick()
        );
        
        shader.clear();
        
        GlStateManager._enableCull();
        CHelper.disableDepthClamp();
        
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._depthMask(true);
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        
        if (PortalRendering.isRendering()) {
            FrontClipping.disableClipping();
        }
        
        CHelper.checkGlError();
    }
    
    public static void buildPortalViewAreaTrianglesBuffer(
        Vec3 fogColor, PortalRenderable portalRenderable,
        Vec3 cameraPos, float tickDelta
    ) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        Vec3 originRelativeToCamera = portalRenderable.getPortalLike().getOriginPos().subtract(cameraPos);
        
        TriangleConsumer vertexOutput = (p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z) -> {
            bufferBuilder
                .vertex(p0x, p0y, p0z)
                .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
                .endVertex();
            bufferBuilder
                .vertex(p1x, p1y, p1z)
                .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
                .endVertex();
            bufferBuilder
                .vertex(p2x, p2y, p2z)
                .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
                .endVertex();
        };
        
        if (portalRenderable instanceof Portal portal) {
            portal.renderViewAreaMesh(originRelativeToCamera, vertexOutput);
        }
        else if (portalRenderable instanceof PortalRenderer.PortalGroupToRender portalGroupToRender) {
            PortalLike portalLike = portalGroupToRender.getPortalLike();
            for (Portal portal : portalGroupToRender.portals()) {
                Vec3 relativeToGroup = portal.getOriginPos().subtract(portalLike.getOriginPos());
                portal.renderViewAreaMesh(
                    originRelativeToCamera.add(relativeToGroup),
                    vertexOutput
                );
            }
        }
        
        BufferUploader.draw(bufferBuilder.end());
    }
    
    public static void generateViewAreaTriangles(
        Portal portal, Vec3 posInPlayerCoordinate,
        TriangleConsumer vertexOutput
    ) {
        if (portal.specialShape == null) {
            if (portal.getIsGlobal()) {
                generateTriangleForGlobalPortal(
                    vertexOutput,
                    portal,
                    posInPlayerCoordinate
                );
            }
            else {
                generateTriangleForNormalShape(
                    vertexOutput,
                    portal,
                    posInPlayerCoordinate
                );
            }
        }
        else {
            generateTriangleForSpecialShape(
                vertexOutput,
                portal,
                posInPlayerCoordinate
            );
        }
    }
    
    public static void generateTriangleForSpecialShape(
        TriangleConsumer vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate
        );
    }
    
    public static void generateTriangleSpecial(
        TriangleConsumer vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        GeometryPortalShape specialShape = portal.specialShape;
        assert specialShape != null;
        
        double halfWidth = portal.width / 2;
        double halfHeight = portal.height / 2;
        
        Vec3 localXAxis = portal.axisW.scale(halfWidth);
        Vec3 localYAxis = portal.axisH.scale(halfHeight);
        
        Mesh2D mesh = specialShape.mesh;
        
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
                
                outputTriangle(
                    vertexOutput, posInPlayerCoordinate,
                    localXAxis, localYAxis, p0x, p0y, p1x, p1y, p2x, p2y);
            }
        }
        
    }
    
    private static void outputTriangle(
        TriangleConsumer vertexOutput, Vec3 center,
        Vec3 localXAxis, Vec3 localYAxis,
        double p0x, double p0y, double p1x, double p1y, double p2x, double p2y
    ) {
        vertexOutput.accept(
            center.x + p0x * localXAxis.x() + p0y * localYAxis.x(),
            center.y + p0x * localXAxis.y() + p0y * localYAxis.y(),
            center.z + p0x * localXAxis.z() + p0y * localYAxis.z(),
            center.x + p1x * localXAxis.x() + p1y * localYAxis.x(),
            center.y + p1x * localXAxis.y() + p1y * localYAxis.y(),
            center.z + p1x * localXAxis.z() + p1y * localYAxis.z(),
            center.x + p2x * localXAxis.x() + p2y * localYAxis.x(),
            center.y + p2x * localXAxis.y() + p2y * localYAxis.y(),
            center.z + p2x * localXAxis.z() + p2y * localYAxis.z()
        );
    }
    
    private static void generateTriangleForNormalShape(
        TriangleConsumer vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        //avoid floating point error for converted global portal
        final double w = Math.min(portal.width, 23333);
        final double h = Math.min(portal.height, 23333);
        
        Vec3 localXAxis = portal.axisW.scale(w / 2);
        Vec3 localYAxis = portal.axisH.scale(h / 2);
        
        outputFullQuad(vertexOutput, posInPlayerCoordinate, localXAxis, localYAxis);
        
    }
    
    private static void generateTriangleForGlobalPortal(
        TriangleConsumer vertexOutput,
        Portal portal,
        Vec3 portalOriginLocal
    ) {
        Vec3 cameraPosFromPortalOrigin = portalOriginLocal.scale(-1);
        
        Vec3 cameraPosFromPortalOriginProjected =
            portal.getLocalVecProjectedToPlane(cameraPosFromPortalOrigin);
        
        Vec3 localCenter = portalOriginLocal.add(cameraPosFromPortalOriginProjected);
        
        double r = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 - 16;
        if (TransformationManager.isIsometricView) {
            r *= 2;
        }
        
        double distance = Math.abs(cameraPosFromPortalOrigin.dot(portal.getNormal()));
        if (distance > 200) {
            r = r * 200 / distance;
        }
        
        Vec3 localXAxis = portal.axisW.scale(r);
        Vec3 localYAxis = portal.axisH.scale(r);
        
        outputFullQuad(vertexOutput, localCenter, localXAxis, localYAxis);
    }
    
    private static void outputFullQuad(
        TriangleConsumer vertexOutput, Vec3 posInPlayerCoordinate,
        Vec3 localXAxis, Vec3 localYAxis
    ) {
        outputTriangle(
            vertexOutput, posInPlayerCoordinate,
            localXAxis, localYAxis, 1, 1, -1, 1, 1, -1
        );
        outputTriangle(
            vertexOutput, posInPlayerCoordinate,
            localXAxis, localYAxis, -1, 1, -1, -1, 1, -1
        );
    }
}
