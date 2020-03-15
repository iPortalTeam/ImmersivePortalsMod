package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.Arrays;
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
            generateTriangleForNormalShape(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate,
                false
            );
        }
        else {
            generateTriangleForSpecialShape(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate,
                false
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
        Vec3d posInPlayerCoordinate,
        boolean doGenerateWall
    ) {
        generateTriangleSpecial(
            vertexOutput, portal, posInPlayerCoordinate,
            portal.getNormal().multiply(-0.5), doGenerateWall
        );
    }
    
    private static void generateTriangleSpecial(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate,
        Vec3d innerOffset,
        boolean doGenerateWall
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
            
            if (doGenerateWall) {
                Vec3d center = a.add(b).add(c).multiply(1.0 / 3);
                
                Vec3d as = a.multiply(0.99).add(center.multiply(0.01)).add(innerOffset);
                Vec3d bs = b.multiply(0.99).add(center.multiply(0.01)).add(innerOffset);
                Vec3d cs = c.multiply(0.99).add(center.multiply(0.01)).add(innerOffset);
                
                putIntoQuad(vertexOutput, a, b, bs, as);
                putIntoQuad(vertexOutput, b, c, cs, bs);
                putIntoQuad(vertexOutput, c, a, as, cs);
            }
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
        Vec3d posInPlayerCoordinate,
        boolean isClose
    ) {
        Vec3d layerOffsest = portal.getNormal().multiply(-layerWidth);
        
        Vec3d[] frontFace = Arrays.stream(portal.getFourVerticesLocal(0))
            .map(pos -> pos.add(posInPlayerCoordinate))
            .toArray(Vec3d[]::new);
        
        putIntoQuad(
            vertexOutput,
            frontFace[0],
            frontFace[2],
            frontFace[3],
            frontFace[1]
        );
        
        if (isClose) {
            Vec3d[] backFace = Arrays.stream(portal.getFourVerticesLocal(0.01))
                .map(pos -> pos.add(posInPlayerCoordinate).add(layerOffsest))
                .toArray(Vec3d[]::new);
            
            putIntoQuad(
                vertexOutput,
                frontFace[0], frontFace[2],
                backFace[2], backFace[0]
            );
            
            putIntoQuad(
                vertexOutput,
                frontFace[2], frontFace[3],
                backFace[3], backFace[2]
            );
            
            putIntoQuad(
                vertexOutput,
                frontFace[3], frontFace[1],
                backFace[1], backFace[3]
            );
            
            putIntoQuad(
                vertexOutput,
                frontFace[1], frontFace[0],
                backFace[0], backFace[1]
            );
        }
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
        CGlobal.myGameRenderer.endCulling();
    
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.mc.gameRenderer.getCamera().getPos(),
            MyRenderHelper.partialTicks,
            portal instanceof Mirror ? 0 : 0.45F
        );
    
        boolean shouldReverseCull = MyRenderHelper.isRenderingOddNumberOfMirrors();
        if (shouldReverseCull) {
            MyRenderHelper.applyMirrorFaceCulling();
        }
        if (doFrontCulling) {
            if (CGlobal.renderer.isRendering()) {
                CGlobal.myGameRenderer.updateCullingPlane(matrixStack);
                CGlobal.myGameRenderer.startCulling();
            }
        }
    
        McHelper.runWithTransformation(
            matrixStack,
            () -> tessellator.draw()
        );
    
        if (shouldReverseCull) {
            MyRenderHelper.recoverFaceCulling();
        }
        if (doFrontCulling) {
            if (CGlobal.renderer.isRendering()) {
                CGlobal.myGameRenderer.endCulling();
            }
        }
    
        GlStateManager.enableTexture();
    
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    private static Vec3d getCurrentFogColor(Portal portal) {
        DimensionType dimension = portal.dimensionTo;
        
        //for Altius
        if (portal instanceof VerticalConnectingPortal) {
            if (dimension == DimensionType.THE_NETHER) {
                return getFogColorOf(DimensionType.OVERWORLD);
            }
        }
        
        return getFogColorOf(dimension);
    }
    
    private static Vec3d getFogColorOf(DimensionType dimension) {
        Helper.SimpleBox<Vec3d> boxOfFogColor = new Helper.SimpleBox<>(null);
        
        FogRendererContext.swappingManager.swapAndInvoke(
            dimension,
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
    
        renderAdditionalBox(portal, vertexOutput, projected, normal);
    }
    
    private static void renderAdditionalBox(
        Portal portal,
        Consumer<Vec3d> vertexOutput,
        Vec3d projected,
        Vec3d normal
    ) {
//        renderHood(portal, vertexOutput, projected, normal, 1.5);
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
        
        Consumer<Vec3d> compactVertexOutput = vertexOutput;
        
        compactVertexOutput.accept(b);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(a);
        
        compactVertexOutput.accept(c);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(b);
        
        compactVertexOutput.accept(d);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(c);
        
        compactVertexOutput.accept(a);
        compactVertexOutput.accept(mid);
        compactVertexOutput.accept(d);
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
