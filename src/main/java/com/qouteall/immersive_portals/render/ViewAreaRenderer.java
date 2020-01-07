package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.SpecialPortalShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.GL_CLIP_PLANE0;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ViewAreaRenderer {
    private static void buildPortalViewAreaTrianglesBuffer(
        Vec3d fogColor, Portal portal, BufferBuilder bufferbuilder,
        Vec3d cameraPos, float partialTicks, float layerWidth
    ) {
        //if layerWidth is small, the teleportation will not be seamless
        
        //counter-clockwise triangles are front-faced in default
        
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
    
        Vec3d posInPlayerCoordinate = portal.getPos().subtract(cameraPos);
    
        if (portal instanceof Mirror) {
            posInPlayerCoordinate = posInPlayerCoordinate.add(portal.getNormal().multiply(-0.001));
        }
    
        Consumer<Vec3d> vertexOutput = p -> putIntoVertex(
            bufferbuilder, p, fogColor
        );
    
    
        if (portal.specialShape == null) {
            generateTriangleRectangular(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate
            );
        }
        else {
            generateTriangleSpecial(
                vertexOutput,
                portal,
                layerWidth,
                posInPlayerCoordinate
            );
        }
    
        if (shouldRenderAdditionalBox(portal, cameraPos)) {
            renderAdditionalBox(portal, cameraPos, vertexOutput);
        }
    }
    
    private static void generateTriangleSpecial(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        float layerWidth,
        Vec3d posInPlayerCoordinate
    ) {
        generateTriangleSpecialWithOffset(
            vertexOutput, portal, posInPlayerCoordinate,
            Vec3d.ZERO
        );

//        generateTriangleSpecialWithOffset(
//            vertexOutput, portal, posInPlayerCoordinate,
//            portal.getNormal().multiply(-layerWidth)
//        );
    }
    
    private static void generateTriangleSpecialWithOffset(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        Vec3d posInPlayerCoordinate,
        Vec3d offset
    ) {
        SpecialPortalShape specialShape = portal.specialShape;
        
        for (SpecialPortalShape.TriangleInPlane triangle : specialShape.triangles) {
            putIntoLocalVertex(
                vertexOutput, portal, offset, posInPlayerCoordinate,
                triangle.x1, triangle.y1
            );
            putIntoLocalVertex(
                vertexOutput, portal, offset, posInPlayerCoordinate,
                triangle.x3, triangle.y3
            );
            putIntoLocalVertex(
                vertexOutput, portal, offset, posInPlayerCoordinate,
                triangle.x2, triangle.y2
            );
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
    
    private static void generateTriangleRectangular(
        Consumer<Vec3d> vertexOutput,
        Portal portal,
        float layerWidth,
        Vec3d posInPlayerCoordinate
    ) {
        Vec3d layerOffsest = portal.getNormal().multiply(-layerWidth);
        
        Vec3d[] frontFace = Arrays.stream(portal.getFourVerticesRelativeToCenter(0))
            .map(pos -> pos.add(posInPlayerCoordinate))
            .toArray(Vec3d[]::new);
        
        Vec3d[] backFace = Arrays.stream(portal.getFourVerticesRelativeToCenter(0))
            .map(pos -> pos.add(posInPlayerCoordinate).add(layerOffsest))
            .toArray(Vec3d[]::new);

//        putIntoQuad(
//            vertexOutput,
//            backFace[0],
//            backFace[2],
//            backFace[3],
//            backFace[1]
//        );
        
        putIntoQuad(
            vertexOutput,
            frontFace[0],
            frontFace[2],
            frontFace[3],
            frontFace[1]
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
    
    public static void drawPortalViewTriangle(Portal portal) {
        MinecraftClient.getInstance().getProfiler().push("render_view_triangle");
        
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
        
        Vec3d fogColor = helper.getFogColor();
    
        //important
        GlStateManager.enableCull();
    
        //In OpenGL, if you forget to set one rendering state and the result will be abnormal
        //this design is bug-prone (DirectX is better in this aspect)
        GuiLighting.disable();
        GlStateManager.color4f(1, 1, 1, 1);
        GlStateManager.disableFog();
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        
        GL11.glDisable(GL_CLIP_PLANE0);
    
        if (OFInterface.isShaders.getAsBoolean()) {
            fogColor = Vec3d.ZERO;
        }
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.mc.gameRenderer.getCamera().getPos(),
            MyRenderHelper.partialTicks,
            portal instanceof Mirror ? 0 : 0.45F
        );
        
        tessellator.draw();
        
        GlStateManager.enableCull();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
    
        MinecraftClient.getInstance().getProfiler().pop();
    }
    
    
    private static boolean shouldRenderAdditionalBox(
        Portal portal,
        Vec3d cameraPos
    ) {
        return (portal.getDistanceToPlane(cameraPos) < 0.1) &&
            portal.isPointInPortalProjection(cameraPos);
    }
    
    private static void renderAdditionalBox(
        Portal portal,
        Vec3d cameraPos,
        Consumer<Vec3d> vertexOutput
    ) {
        Vec3d projected = portal.getPointInPortalProjection(cameraPos).subtract(cameraPos);
        Vec3d normal = portal.getNormal();
        
        final double boxRadius = 1;
        final double correctionFactor = 0;
        Vec3d correction = normal.multiply(correctionFactor);
        
        Vec3d dx = portal.axisW.multiply(boxRadius);
        Vec3d dy = portal.axisH.multiply(boxRadius);
        
        Vec3d a = projected.add(dx).add(dy).add(correction);
        Vec3d b = projected.subtract(dx).add(dy).add(correction);
        Vec3d c = projected.subtract(dx).subtract(dy).add(correction);
        Vec3d d = projected.add(dx).subtract(dy).add(correction);
    
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
}
