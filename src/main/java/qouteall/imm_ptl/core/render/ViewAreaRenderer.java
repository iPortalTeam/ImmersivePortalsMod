package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.opengl.GlStateManager;
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
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.my_util.TriangleConsumer;

import java.util.Objects;

public class ViewAreaRenderer {
    
    public static void renderPortalArea(
        Portal portal, Vec3 fogColor,
        Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
        boolean doFaceCulling, boolean doModifyColor,
        boolean doModifyDepth, boolean doClip
    ) {
        
        if (doFaceCulling) {
            GlStateManager._enableCull();
        }
        else {
            GlStateManager._disableCull();
        }
        
        if (portal.isFuseView() && IPGlobal.maxPortalLayer != 0) {
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
            if (portal.isFuseView()) {
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
            portal,
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
        Vec3 fogColor, Portal portal,
        Vec3 cameraPos, float partialTick
    ) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator
            .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        Vec3 originRelativeToCamera = portal.getOriginPos().subtract(cameraPos);
        
        TriangleConsumer vertexOutput = (p0x, p0y, p0z, p1x, p1y, p1z, p2x, p2y, p2z) -> {
            bufferBuilder
                .addVertex((float) p0x, (float) p0y, (float) p0z)
                .setColor((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f);
            bufferBuilder
                .addVertex((float) p1x, (float) p1y, (float) p1z)
                .setColor((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f);
            bufferBuilder
                .addVertex((float) p2x, (float) p2y, (float) p2z)
                .setColor((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f);
        };
        
        portal.renderViewAreaMesh(originRelativeToCamera, vertexOutput);
        
        BufferUploader.draw(Objects.requireNonNull(bufferBuilder.build()));
    }
    
    public static void outputTriangle(
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
    
    @Deprecated
    private static void generateTriangleForNormalShape(
        TriangleConsumer vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        //avoid floating point error for converted global portal
        final double w = Math.min(portal.getWidth(), 23333);
        final double h = Math.min(portal.getHeight(), 23333);
        
        Vec3 localXAxis = portal.getAxisW().scale(w / 2);
        Vec3 localYAxis = portal.getAxisH().scale(h / 2);
        
        outputFullQuad(vertexOutput, posInPlayerCoordinate, localXAxis, localYAxis);
        
    }
    
    @Deprecated
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
        
        Vec3 localXAxis = portal.getAxisW().scale(r);
        Vec3 localYAxis = portal.getAxisH().scale(r);
        
        outputFullQuad(vertexOutput, localCenter, localXAxis, localYAxis);
    }
    
    public static void outputFullQuad(
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
