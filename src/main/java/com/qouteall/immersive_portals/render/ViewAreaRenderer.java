package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ViewAreaRenderer {
    private static void buildPortalViewAreaTrianglesBuffer(
        Vec3d fogColor, Portal portal, BufferBuilder bufferbuilder,
        Vec3d cameraPos, float partialTicks, float layerWidth
    ) {
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        Vec3d posInPlayerCoordinate = portal.getPos().subtract(cameraPos);
        
        if (portal instanceof Mirror) {
            posInPlayerCoordinate = posInPlayerCoordinate.add(portal.getNormal().multiply(-0.001));
        }
        
        Consumer<Vec3d> vertexOutput = p -> putIntoVertex(
            bufferbuilder, p, fogColor
        );
        
        boolean isClose = isCloseToPortal(portal, cameraPos);
        
        if (portal.specialShape == null) {
            if (portal instanceof GlobalTrackedPortal) {
                generateTriangleForGlobalPortal(
                    vertexOutput,
                    portal,
                    layerWidth,
                    posInPlayerCoordinate
                );
            }
            else {
                generateTriangleForNormalShape(
                    vertexOutput,
                    portal,
                    layerWidth,
                    posInPlayerCoordinate
                );
            }
        }
        else {
            generateTriangleForSpecialShape(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate
            );
        }
        
        if (isClose) {
            renderAdditionalBox(portal, cameraPos, vertexOutput);
        }
    }
    
    private static void generateTriangleForSpecialShape(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        float layerWidth,
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
        float layerWidth,
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
        float layerWidth,
        Vec3d posInPlayerCoordinate
    ) {
        Vec3d cameraPosLocal = posInPlayerCoordinate.multiply(-1);
        
        double cameraLocalX = cameraPosLocal.dotProduct(portal.axisW);
        double cameraLocalY = cameraPosLocal.dotProduct(portal.axisH);
        
        double r = MinecraftClient.getInstance().options.viewDistance * 16-16;
    
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
        
        GlStateManager.disableTexture();
        PixelCuller.endCulling();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.client.gameRenderer.getCamera().getPos(),
            MyRenderHelper.tickDelta,
            portal instanceof Mirror ? 0 : 0.45F
        );
        
        boolean shouldReverseCull = MyRenderHelper.isRenderingOddNumberOfMirrors();
        if (shouldReverseCull) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
        if (doFrontCulling) {
            if (CGlobal.renderer.isRendering()) {
                PixelCuller.updateCullingPlaneInner(
                    matrixStack, CGlobal.renderer.getRenderingPortal(), false
                );
                PixelCuller.loadCullingPlaneClassical(matrixStack);
                PixelCuller.startClassicalCulling();
            }
        }
        
        MinecraftClient.getInstance().getProfiler().push("draw");
        McHelper.runWithTransformation(
            matrixStack,
            () -> tessellator.draw()
        );
        MinecraftClient.getInstance().getProfiler().pop();
        
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (doFrontCulling) {
            if (CGlobal.renderer.isRendering()) {
                PixelCuller.endCulling();
            }
        }
        
        GlStateManager.enableTexture();
        
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private static Vec3d getCurrentFogColor(Portal portal) {
    
        if (Global.edgelessSky) {
            return getFogColorOf(MyRenderHelper.originalPlayerDimension);
        }
        
        return getFogColorOf(portal.dimensionTo);
    }
    
    private static Vec3d getFogColorOf(DimensionType dimension) {
        Helper.SimpleBox<Vec3d> boxOfFogColor = new Helper.SimpleBox<>(null);
        
        FogRendererContext.swappingManager.swapAndInvoke(
            RenderDimensionRedirect.getRedirectedDimension(dimension),
            () -> {
                boxOfFogColor.obj = FogRendererContext.getCurrentFogColor.get();
            }
        );
        
        Vec3d fogColor = boxOfFogColor.obj;
        
        if (OFInterface.isShaders.getAsBoolean()) {
            fogColor = Vec3d.ZERO;
        }
        return fogColor;
    }
    
    private static boolean isCloseToPortal(
        Portal portal,
        Vec3d cameraPos
    ) {
        return (portal.getDistanceToPlane(cameraPos) < 0.2) &&
            portal.isPointInPortalProjection(cameraPos);
    }
    
    private static void renderAdditionalBox(
        Portal portal,
        Vec3d cameraPos,
        Consumer<Vec3d> vertexOutput
    ) {
        Vec3d projected = portal.getPointInPortalProjection(cameraPos).subtract(cameraPos);
        Vec3d normal = portal.getNormal();
        
        renderHood(portal, vertexOutput, projected, normal, 0.4);
    }
    
    private static void renderHood(
        Portal portal,
        Consumer<Vec3d> vertexOutput,
        Vec3d projected,
        Vec3d normal,
        double boxRadius
    ) {
        Vec3d dx = portal.axisW.multiply(boxRadius);
        Vec3d dy = portal.axisH.multiply(boxRadius);
        
        Vec3d a = projected.add(dx).add(dy);
        Vec3d b = projected.subtract(dx).add(dy);
        Vec3d c = projected.subtract(dx).subtract(dy);
        Vec3d d = projected.add(dx).subtract(dy);
        
        Vec3d mid = projected.add(normal.multiply(-0.5));
        
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
