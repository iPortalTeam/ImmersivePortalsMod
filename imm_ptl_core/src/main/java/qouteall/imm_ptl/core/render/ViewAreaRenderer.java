package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.GeometryPortalShape;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

import static org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK;
import static org.lwjgl.opengl.GL11.glGetBoolean;

public class ViewAreaRenderer {
    
    public static void renderPortalArea(
        PortalLike portal, Vec3 fogColor,
        Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
        boolean doFaceCulling, boolean doModifyColor,
        boolean doModifyDepth
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
            RenderStates.tickDelta
        );
        
        shader.clear();
        
        GlStateManager._enableCull();
        CHelper.disableDepthClamp();
        
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._depthMask(true);
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        
        CHelper.checkGlError();
    }
    
    public static void buildPortalViewAreaTrianglesBuffer(
        Vec3 fogColor, PortalLike portal,
        Vec3 cameraPos, float tickDelta
    ) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        Vec3 posInPlayerCoordinate = portal.getOriginPos().subtract(cameraPos);
        
        Consumer<Vec3> vertexOutput = p -> putIntoVertex(
            bufferBuilder, p, fogColor
        );
        
        portal.renderViewAreaMesh(posInPlayerCoordinate, vertexOutput);
        
        BufferUploader.draw(bufferBuilder.end());
    }
    
    public static void generateViewAreaTriangles(Portal portal, Vec3 posInPlayerCoordinate, Consumer<Vec3> vertexOutput) {
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
        Consumer<Vec3> vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate
        );
    }
    
    public static void generateTriangleSpecial(
        Consumer<Vec3> vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        GeometryPortalShape specialShape = portal.specialShape;
        
        for (GeometryPortalShape.TriangleInPlane triangle : specialShape.triangles) {
            Vec3 a = posInPlayerCoordinate
                .add(portal.axisW.scale(triangle.x1))
                .add(portal.axisH.scale(triangle.y1));
            
            Vec3 b = posInPlayerCoordinate
                .add(portal.axisW.scale(triangle.x3))
                .add(portal.axisH.scale(triangle.y3));
            
            Vec3 c = posInPlayerCoordinate
                .add(portal.axisW.scale(triangle.x2))
                .add(portal.axisH.scale(triangle.y2));
            
            vertexOutput.accept(a);
            vertexOutput.accept(b);
            vertexOutput.accept(c);
        }
    }
    
    private static void putIntoLocalVertex(
        Consumer<Vec3> vertexOutput,
        Portal portal,
        Vec3 offset,
        Vec3 posInPlayerCoordinate,
        double localX, double localY
    ) {
        //according to https://stackoverflow.com/questions/43002528/when-can-hotspot-allocate-objects-on-the-stack
        //this will possibly not generate gc pressure?
        
        vertexOutput.accept(
            posInPlayerCoordinate
                .add(portal.axisW.scale(localX))
                .add(portal.axisH.scale(localY))
                .add(offset)
        );
    }
    
    private static void generateTriangleForNormalShape(
        Consumer<Vec3> vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        //avoid floating point error for converted global portal
        final double w = Math.min(portal.width, 23333);
        final double h = Math.min(portal.height, 23333);
        Vec3 v0 = portal.getPointInPlaneLocal(
            w / 2 - (double) 0,
            -h / 2 + (double) 0
        );
        Vec3 v1 = portal.getPointInPlaneLocal(
            -w / 2 + (double) 0,
            -h / 2 + (double) 0
        );
        Vec3 v2 = portal.getPointInPlaneLocal(
            w / 2 - (double) 0,
            h / 2 - (double) 0
        );
        Vec3 v3 = portal.getPointInPlaneLocal(
            -w / 2 + (double) 0,
            h / 2 - (double) 0
        );
        
        putIntoQuad(
            vertexOutput,
            v0.add(posInPlayerCoordinate),
            v2.add(posInPlayerCoordinate),
            v3.add(posInPlayerCoordinate),
            v1.add(posInPlayerCoordinate)
        );
        
    }
    
    private static void generateTriangleForGlobalPortal(
        Consumer<Vec3> vertexOutput,
        Portal portal,
        Vec3 posInPlayerCoordinate
    ) {
        Vec3 cameraPosLocal = posInPlayerCoordinate.scale(-1);
        
        double cameraLocalX = cameraPosLocal.dot(portal.axisW);
        double cameraLocalY = cameraPosLocal.dot(portal.axisH);
        
        double r = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16 - 16;
        if (TransformationManager.isIsometricView) {
            r *= 2;
        }
        
        double distance = Math.abs(cameraPosLocal.dot(portal.getNormal()));
        if (distance > 200) {
            r = r * 200 / distance;
        }
        
        Vec3 v0 = portal.getPointInPlaneLocalClamped(
            r + cameraLocalX,
            -r + cameraLocalY
        );
        Vec3 v1 = portal.getPointInPlaneLocalClamped(
            -r + cameraLocalX,
            -r + cameraLocalY
        );
        Vec3 v2 = portal.getPointInPlaneLocalClamped(
            r + cameraLocalX,
            r + cameraLocalY
        );
        Vec3 v3 = portal.getPointInPlaneLocalClamped(
            -r + cameraLocalX,
            r + cameraLocalY
        );
        
        putIntoQuad(
            vertexOutput,
            v0.add(posInPlayerCoordinate),
            v2.add(posInPlayerCoordinate),
            v3.add(posInPlayerCoordinate),
            v1.add(posInPlayerCoordinate)
        );
    }
    
    static private void putIntoVertex(BufferBuilder bufferBuilder, Vec3 pos, Vec3 fogColor) {
        bufferBuilder
            .vertex(pos.x, pos.y, pos.z)
            .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
            .endVertex();
    }
    
    //a d
    //b c
    private static void putIntoQuad(
        Consumer<Vec3> vertexOutput,
        Vec3 a,
        Vec3 b,
        Vec3 c,
        Vec3 d
    ) {
        //counter-clockwise triangles are front-faced in default
        
        vertexOutput.accept(b);
        vertexOutput.accept(c);
        vertexOutput.accept(d);
        
        vertexOutput.accept(d);
        vertexOutput.accept(a);
        vertexOutput.accept(b);
        
    }


//    public static void drawPortalViewTriangle(
//        PortalLike portal,
//        MatrixStack matrixStack,
//        boolean doFrontCulling,
//        boolean doFaceCulling
//    ) {
//
//        MinecraftClient.getInstance().getProfiler().push("render_view_triangle");
//
//        Vec3d fogColor = FogRendererContext.getCurrentFogColor.get();
//
//        if (doFaceCulling) {
//            GlStateManager.enableCull();
//        }
//        else {
//            GlStateManager.disableCull();
//        }
//
//        //should not affect shader pipeline
//        FrontClipping.disableClipping();
//
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferbuilder = tessellator.getBuffer();
//        buildPortalViewAreaTrianglesBuffer(
//            fogColor,
//            portal,
//            bufferbuilder,
//            PortalRenderer.client.gameRenderer.getCamera().getPos(),
//            RenderStates.tickDelta,
//            portal instanceof Mirror ? 0 : 0.45F
//        );
//
//        boolean shouldReverseCull = PortalRendering.isRenderingOddNumberOfMirrors();
//        if (shouldReverseCull) {
//            MyRenderHelper.applyMirrorFaceCulling();
//        }
//        if (doFrontCulling) {
//            if (PortalRendering.isRendering()) {
//                FrontClipping.setupInnerClipping(
//                    matrixStack, PortalRendering.getRenderingPortal(), false
//                );
//            }
//        }
//
//        MinecraftClient.getInstance().getProfiler().push("draw");
//        GL11.glEnable(GL32.GL_DEPTH_CLAMP);
//        CHelper.checkGlError();
//        McHelper.runWithTransformation(
//            matrixStack,
//            tessellator::draw
//        );
//        GL11.glDisable(GL32.GL_DEPTH_CLAMP);
//        MinecraftClient.getInstance().getProfiler().pop();
//
//        if (shouldReverseCull) {
//            MyRenderHelper.recoverFaceCulling();
//        }
//        if (doFrontCulling) {
//            if (PortalRendering.isRendering()) {
//                FrontClipping.disableClipping();
//            }
//        }
//
//        //this is important
//        GlStateManager.enableCull();
//
//        MinecraftClient.getInstance().getProfiler().pop();
//    }
    
}
