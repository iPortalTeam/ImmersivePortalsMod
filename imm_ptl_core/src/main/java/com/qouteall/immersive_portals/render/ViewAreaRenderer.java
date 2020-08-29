package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL32;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public class ViewAreaRenderer {
    private static void buildPortalViewAreaTrianglesBuffer(
        Vec3d fogColor, Portal portal, BufferBuilder bufferbuilder,
        Vec3d cameraPos, float tickDelta, float layerWidth
    ) {
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        Vec3d posInPlayerCoordinate = portal.getPos().subtract(cameraPos);
        
        if (portal instanceof Mirror) {
            //rendering portal behind translucent objects with shader is broken
            double mirrorOffset =
                (OFInterface.isShaders.getAsBoolean() || Global.pureMirror) ? 0.01 : -0.01;
            posInPlayerCoordinate = posInPlayerCoordinate.add(
                portal.getNormal().multiply(mirrorOffset));
        }
        
        Consumer<Vec3d> vertexOutput = p -> putIntoVertex(
            bufferbuilder, p, fogColor
        );
        
        generateViewAreaTriangles(portal, posInPlayerCoordinate, vertexOutput);
        
//        double distanceToPlane = portal.getDistanceToPlane(cameraPos);
//        boolean shouldRenderHood = shouldRenderAdditionalHood(portal, cameraPos, distanceToPlane);
        
//        if (shouldRenderHood) {
//            renderAdditionalBox(portal, cameraPos, vertexOutput);
//        }
//
//        if (distanceToPlane < 0.05) {
//            generateViewAreaTriangles(
//                portal,
//                posInPlayerCoordinate.add(portal.getNormal().multiply(0.05)),
//                vertexOutput
//            );
//        }
        
    }
    
    private static void generateViewAreaTriangles(Portal portal, Vec3d posInPlayerCoordinate, Consumer<Vec3d> vertexOutput) {
        if (portal.specialShape == null) {
            if (portal instanceof GlobalTrackedPortal) {
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
    
    private static void generateTriangleForSpecialShape(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate,
            portal.getNormal().multiply(-0.5)
        );
    }
    
    private static void generateTriangleSpecial(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate,
        Vec3d innerOffset
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
        Vec3d v0 = portal.getPointInPlaneLocal(
            portal.width / 2 - (double) 0,
            -portal.height / 2 + (double) 0
        );
        Vec3d v1 = portal.getPointInPlaneLocal(
            -portal.width / 2 + (double) 0,
            -portal.height / 2 + (double) 0
        );
        Vec3d v2 = portal.getPointInPlaneLocal(
            portal.width / 2 - (double) 0,
            portal.height / 2 - (double) 0
        );
        Vec3d v3 = portal.getPointInPlaneLocal(
            -portal.width / 2 + (double) 0,
            portal.height / 2 - (double) 0
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
        Portal portal,
        MatrixStack matrixStack,
        boolean doFrontCulling,
        boolean doFaceCulling
    ) {
        
        MinecraftClient.getInstance().getProfiler().push("render_view_triangle");
        
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
        
        Vec3d fogColor = getCurrentFogColor(portal);
        
        if (doFaceCulling) {
            GlStateManager.enableCull();
        }
        else {
            GlStateManager.disableCull();
        }
        
        //should not affect shader pipeline
//        GlStateManager.disableTexture();
        PixelCuller.endCulling();
        
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
                PixelCuller.updateCullingPlaneInner(
                    matrixStack, PortalRendering.getRenderingPortal(), false
                );
                PixelCuller.loadCullingPlaneClassical(matrixStack);
                PixelCuller.startClassicalCulling();
            }
        }
        
        MinecraftClient.getInstance().getProfiler().push("draw");
//        CGlobal.shaderManager.unloadShader();
        glEnable(GL32.GL_DEPTH_CLAMP);
        CHelper.checkGlError();
        McHelper.runWithTransformation(
            matrixStack,
            tessellator::draw
        );
        glDisable(GL32.GL_DEPTH_CLAMP);
        MinecraftClient.getInstance().getProfiler().pop();
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (doFrontCulling) {
            if (PortalRendering.isRendering()) {
                PixelCuller.endCulling();
            }
        }
        
//        GlStateManager.enableTexture();
        
        //this is important
        GlStateManager.enableCull();
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private static Vec3d getCurrentFogColor(Portal portal) {
        
        if (OFInterface.isShaders.getAsBoolean()) {
            return Vec3d.ZERO;
        }
        
        //TODO handle edgelessSky option
        
        return FogRendererContext.getFogColorOf(
            ((ClientWorld) portal.getDestinationWorld()),
            portal.transformPoint(McHelper.getCurrentCameraPos())
        );
    }
    
    
    private static boolean shouldRenderAdditionalHood(
        Portal portal,
        Vec3d cameraPos,
        double distanceToPlane
    ) {
//        double localX = cameraPos.subtract(portal.getPos()).dotProduct(portal.axisW);
//        double localY = cameraPos.subtract(portal.getPos()).dotProduct(portal.axisH);
//        if (Math.abs(localX) > 0.9 * portal.width / 2) {
//            return false;
//        }
//        if (Math.abs(localY) > 0.9 * portal.height / 2) {
//            return false;
//        }
        
        Vec3d cameraRotation = MinecraftClient.getInstance().cameraEntity.getRotationVector();
        double cos = cameraRotation.dotProduct(portal.getNormal());
        double sin = Math.sqrt(1.0 - cos * cos);
        
        double threshold = sin * 0.2 + 0.05;
        
        
        return (distanceToPlane < threshold) &&
            portal.isPointInPortalProjection(cameraPos);
    }
    
    private static void renderAdditionalBox(
        Portal portal,
        Vec3d cameraPos,
        Consumer<Vec3d> vertexOutput
    ) {
        Vec3d projected = portal.getPointInPortalProjection(cameraPos).subtract(cameraPos);
        Vec3d normal = portal.getNormal();
        
        renderHood(portal, vertexOutput, projected, normal);
    }
    
    private static void renderHood(
        Portal portal,
        Consumer<Vec3d> vertexOutput,
        Vec3d projected,
        Vec3d normal
    ) {
        double boxRadius = 0.1 * Math.sqrt(3) * 1.3;//0.4
        double boxDepth = 0.1 * 1.3;//0.5
        
        Vec3d dx = portal.axisW.multiply(boxRadius);
        Vec3d dy = portal.axisH.multiply(boxRadius);
        
        Vec3d a = projected.add(dx).add(dy);
        Vec3d b = projected.subtract(dx).add(dy);
        Vec3d c = projected.subtract(dx).subtract(dy);
        Vec3d d = projected.add(dx).subtract(dy);
        
        Vec3d mid = projected.add(normal.multiply(-boxDepth));
        
        vertexOutput.accept(b);
        vertexOutput.accept(mid);
        vertexOutput.accept(a);
        
        vertexOutput.accept(c);
        vertexOutput.accept(mid);
        vertexOutput.accept(b);
        
        vertexOutput.accept(d);
        vertexOutput.accept(mid);
        vertexOutput.accept(c);
        
        vertexOutput.accept(a);
        vertexOutput.accept(mid);
        vertexOutput.accept(d);
    }
    
    
    @Deprecated
    private static void renderAdditionalBoxExperimental(
        Portal portal,
        Consumer<Vec3d> vertexOutput,
        Vec3d projected,
        Vec3d normal
    ) {
        final double boxRadius = 1.5;
        final double boxDepth = 0.5;
        
        //b  a
        //c  d
        
        Vec3d dx = portal.axisW.multiply(boxRadius);
        Vec3d dy = portal.axisH.multiply(boxRadius);
        
        Vec3d a = projected.add(dx).add(dy);
        Vec3d b = projected.subtract(dx).add(dy);
        Vec3d c = projected.subtract(dx).subtract(dy);
        Vec3d d = projected.add(dx).subtract(dy);
        
        Vec3d dz = normal.multiply(-boxDepth);
        
        Vec3d as = a.add(dz);
        Vec3d bs = b.add(dz);
        Vec3d cs = c.add(dz);
        Vec3d ds = d.add(dz);
        
        putIntoQuad(vertexOutput, a, b, bs, as);
        putIntoQuad(vertexOutput, b, c, cs, bs);
        putIntoQuad(vertexOutput, c, d, ds, cs);
        putIntoQuad(vertexOutput, d, a, as, ds);
        
        putIntoQuad(vertexOutput, a, b, c, d);
    }
    
}
