package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ViewAreaRenderer {
    private static void buildPortalViewAreaTrianglesBuffer(
        Vec3d fogColor, PortalLike portal, BufferBuilder bufferbuilder,
        Vec3d cameraPos, float tickDelta, float layerWidth
    ) {
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        Vec3d posInPlayerCoordinate = portal.getOriginPos().subtract(cameraPos);
        
        Consumer<Vec3d> vertexOutput = p -> putIntoVertex(
            bufferbuilder, p, fogColor
        );
        
        portal.renderViewAreaMesh(posInPlayerCoordinate, vertexOutput);
        
    }
    
    public static void generateViewAreaTriangles(Portal portal, Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput) {
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
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate
        );
    }
    
    public static void generateTriangleSpecial(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate
    ) {
        GeometryPortalShape specialShape = portal.specialShape;
        
        for (GeometryPortalShape.TriangleInPlane triangle : specialShape.triangles) {
            Vec3d a = posInPlayerCoordinate
                .add(portal.axisW.multiply(triangle.x1))
                .add(portal.axisH.multiply(triangle.y1));
            
            Vec3d b = posInPlayerCoordinate
                .add(portal.axisW.multiply(triangle.x3))
                .add(portal.axisH.multiply(triangle.y3));
            
            Vec3d c = posInPlayerCoordinate
                .add(portal.axisW.multiply(triangle.x2))
                .add(portal.axisH.multiply(triangle.y2));
            
            vertexOutput.accept(a);
            vertexOutput.accept(b);
            vertexOutput.accept(c);
        }
    }
    
    //according to https://stackoverflow.com/questions/43002528/when-can-hotspot-allocate-objects-on-the-stack
    //this will not generate gc pressure
    private static void putIntoLocalVertex(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d offset,
        Vec3d posInPlayerCoordinate,
        double localX, double localY
    ) {
        vertexOutput.accept(
            posInPlayerCoordinate
                .add(portal.axisW.multiply(localX))
                .add(portal.axisH.multiply(localY))
                .add(offset)
        );
    }
    
    private static void generateTriangleForNormalShape(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate
    ) {
        //avoid floating point error for converted global portal
        final double w = Math.min(portal.width, 23333);
        final double h = Math.min(portal.height, 23333);
        Vec3d v0 = portal.getPointInPlaneLocal(
            w / 2 - (double) 0,
            -h / 2 + (double) 0
        );
        Vec3d v1 = portal.getPointInPlaneLocal(
            -w / 2 + (double) 0,
            -h / 2 + (double) 0
        );
        Vec3d v2 = portal.getPointInPlaneLocal(
            w / 2 - (double) 0,
            h / 2 - (double) 0
        );
        Vec3d v3 = portal.getPointInPlaneLocal(
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
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate
    ) {
        Vec3d cameraPosLocal = posInPlayerCoordinate.multiply(-1);
        
        double cameraLocalX = cameraPosLocal.dotProduct(portal.axisW);
        double cameraLocalY = cameraPosLocal.dotProduct(portal.axisH);
        
        double r = MinecraftClient.getInstance().options.viewDistance * 16 - 16;
        if (TransformationManager.isIsometricView) {
            r *= 2;
        }
        
        double distance = Math.abs(cameraPosLocal.dotProduct(portal.getNormal()));
        if (distance > 200) {
            r = r * 200 / distance;
        }
        
        Vec3d v0 = portal.getPointInPlaneLocalClamped(
            r + cameraLocalX,
            -r + cameraLocalY
        );
        Vec3d v1 = portal.getPointInPlaneLocalClamped(
            -r + cameraLocalX,
            -r + cameraLocalY
        );
        Vec3d v2 = portal.getPointInPlaneLocalClamped(
            r + cameraLocalX,
            r + cameraLocalY
        );
        Vec3d v3 = portal.getPointInPlaneLocalClamped(
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
    
    static private void putIntoVertex(BufferBuilder bufferBuilder, Vec3d pos, Vec3d fogColor) {
        bufferBuilder
            .vertex(pos.x, pos.y, pos.z)
            .color((float) fogColor.x, (float) fogColor.y, (float) fogColor.z, 1.0f)
            .next();
    }
    
    //a d
    //b c
    private static void putIntoQuad(
        Consumer<Vec3d> vertexOutput,
        Vec3d a,
        Vec3d b,
        Vec3d c,
        Vec3d d
    ) {
        //counter-clockwise triangles are front-faced in default
        
        vertexOutput.accept(b);
        vertexOutput.accept(c);
        vertexOutput.accept(d);
        
        vertexOutput.accept(d);
        vertexOutput.accept(a);
        vertexOutput.accept(b);
        
    }
    
    public static void drawPortalViewTriangle(
        PortalLike portal,
        MatrixStack matrixStack,
        boolean doFrontCulling,
        boolean doFaceCulling
    ) {
        
        MinecraftClient.getInstance().getProfiler().push("render_view_triangle");
        
        Vec3d fogColor = FogRendererContext.getCurrentFogColor.get();
        
        if (doFaceCulling) {
            GlStateManager.enableCull();
        }
        else {
            GlStateManager.disableCull();
        }
        
        //should not affect shader pipeline
        FrontClipping.disableClipping();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.client.gameRenderer.getCamera().getPos(),
            RenderStates.tickDelta,
            portal instanceof Mirror ? 0 : 0.45F
        );
        
        boolean shouldReverseCull = PortalRendering.isRenderingOddNumberOfMirrors();
        if (shouldReverseCull) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
        if (doFrontCulling) {
            if (PortalRendering.isRendering()) {
                FrontClipping.setupInnerClipping(
                    matrixStack, PortalRendering.getRenderingPortal(), false
                );
            }
        }
        
        MinecraftClient.getInstance().getProfiler().push("draw");
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        CHelper.checkGlError();
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        GL11.glDisable(GL32.GL_DEPTH_CLAMP);
        MinecraftClient.getInstance().getProfiler().pop();
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (doFrontCulling) {
            if (PortalRendering.isRendering()) {
                FrontClipping.disableClipping();
            }
        }
        
        //this is important
        GlStateManager.enableCull();
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
}
