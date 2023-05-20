package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.animation.RenderedRect;

import java.util.Random;

public class WireRenderingHelper {
    
    public static void renderSmallCubeFrame(
        VertexConsumer vertexConsumer, Vec3 cameraPos, Vec3 boxCenter,
        int color,
        PoseStack matrixStack
    ) {
        Random random = new Random(color);
        
        double boxSize = Math.pow(boxCenter.distanceTo(cameraPos), 0.3) * 0.09;
        
        matrixStack.pushPose();
        matrixStack.translate(
            boxCenter.x - cameraPos.x,
            boxCenter.y - cameraPos.y,
            boxCenter.z - cameraPos.z
        );
        
        DQuaternion rotation = getRandomSmoothRotation(random);
        
        double periodLen = 100;
        
        matrixStack.mulPose(rotation.toMcQuaternion());
        Matrix4f matrix = matrixStack.last().pose();
        
        float alpha = ((color >> 24) & 0xff) / 255f;
        float red = ((color >> 16) & 0xff) / 255f;
        float green = ((color >> 8) & 0xff) / 255f;
        float blue = (color & 0xff) / 255f;
        
        LevelRenderer.renderLineBox(
            matrixStack,
            vertexConsumer,
            -boxSize / 2,
            -boxSize / 2,
            -boxSize / 2,
            boxSize / 2,
            boxSize / 2,
            boxSize / 2,
            red, green, blue, alpha
        );
        matrixStack.popPose();
    }
    
    public static DQuaternion getRandomSmoothRotation(Random random) {
        double time = StableClientTimer.getStableTickTime() + (double) StableClientTimer.getStablePartialTicks();
        
        DQuaternion rotation = DQuaternion.identity;
        
        for (int i = 0; i < 6; i++) {
            rotation = rotation.hamiltonProduct(
                DQuaternion.rotationByDegrees(
                    randomVec(random), CHelper.getSmoothCycles(random.nextInt(30, 60)) * 360
                )
            );
        }
        
        return rotation;
    }
    
    public static double getRandomSmoothCycle(Random random) {
        double totalFactor = 0.1;
        double total = 0;
        for (int i = 0; i < 5; i++) {
            double smoothCycle = CHelper.getSmoothCycles(random.nextInt(30, 300));
            double sin = Math.sin(2 * Math.PI * smoothCycle);
            double factor = random.nextDouble(0.1, 1);
            totalFactor += factor;
            total += sin * factor;
        }
        
        return total / totalFactor;
    }
    
    @NotNull
    public static Vec3 randomVec(Random random) {
        return new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
    }
    
    public static void renderPlane(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Plane plane, double renderedPlaneScale,
        int color, PoseStack matrixStack
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        Vec3 planeCenter = plane.pos;
        Vec3 normal = plane.normal;
    
        Vec3 anyVecNonNormal = new Vec3(13, 29, 71).normalize();
        if (Math.abs(normal.dot(anyVecNonNormal)) > 0.99) {
            anyVecNonNormal = new Vec3(1, 0, 0);
        }
        
        Vec3 planeX = normal.cross(anyVecNonNormal).normalize();
        Vec3 planeY = normal.cross(planeX).normalize();
        
        matrixStack.pushPose();
        matrixStack.translate(
            planeCenter.x - cameraPos.x,
            planeCenter.y - cameraPos.y,
            planeCenter.z - cameraPos.z
        );
        
        matrixStack.mulPose(
            DQuaternion.rotationByDegrees(normal, CHelper.getSmoothCycles(211) * 360)
                .toMcQuaternion()
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        double cameraDistanceToCenter = player.getEyePosition(RenderStates.getPartialTick())
            .distanceTo(planeCenter);
        
        int lineNumPerSide = 10;
        double lineInterval = cameraDistanceToCenter * 0.2 * renderedPlaneScale;
        double lineLenPerSide = lineNumPerSide * lineInterval;
        
        for (int ix = -lineNumPerSide; ix <= lineNumPerSide; ix++) {
            Vec3 lineStart = planeX.scale(ix * lineInterval)
                .add(planeY.scale(-lineLenPerSide));
            Vec3 lineEnd = planeX.scale(ix * lineInterval)
                .add(planeY.scale(lineLenPerSide));
            
            putLineToLineStrip(vertexConsumer, color, planeY, matrix, lineStart, lineEnd);
        }
        
        for (int iy = -lineNumPerSide; iy <= lineNumPerSide; iy++) {
            Vec3 lineStart = planeY.scale(iy * lineInterval)
                .add(planeX.scale(-lineLenPerSide));
            Vec3 lineEnd = planeY.scale(iy * lineInterval)
                .add(planeX.scale(lineLenPerSide));
            
            putLineToLineStrip(vertexConsumer, color, planeX, matrix, lineStart, lineEnd);
        }
        
        matrixStack.popPose();
    }
    
    public static void renderCircle(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Circle circle,
        int color, PoseStack matrixStack
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        Vec3 planeCenter = circle.plane().pos;
        Vec3 normal = circle.plane().normal;
        
        Vec3 anyVecNonNormal = new Vec3(0, 1, 0);
        if (Math.abs(normal.dot(anyVecNonNormal)) > 0.9) {
            anyVecNonNormal = new Vec3(1, 0, 0);
        }
        
        Vec3 planeX = normal.cross(anyVecNonNormal).normalize();
        Vec3 planeY = normal.cross(planeX).normalize();
        
        Vec3 circleCenter = circle.circleCenter();
        double circleRadius = circle.radius();
        
        matrixStack.pushPose();
        
        matrixStack.translate(
            circleCenter.x - cameraPos.x,
            circleCenter.y - cameraPos.y,
            circleCenter.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        int vertexNum = Mth.clamp((int) Math.round(circleRadius * 40), 20, 400);
        
        for (int i = 0; i < vertexNum; i++) {
            double angle = i * 2 * Math.PI / vertexNum;
            double nextAngle = (i + 1) * 2 * Math.PI / vertexNum;
            boolean isBegin = i == 0;
            boolean isEnd = i == vertexNum - 1;
            
            Vec3 lineStart = planeX.scale(Math.cos(angle) * circleRadius)
                .add(planeY.scale(Math.sin(angle) * circleRadius));
            Vec3 lineEnd = planeX.scale(Math.cos(nextAngle) * circleRadius)
                .add(planeY.scale(Math.sin(nextAngle) * circleRadius));
            
            if (isBegin) {
                vertexConsumer
                    .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
                    .color(0)
                    .normal((float) normal.x, (float) normal.y, (float) normal.z)
                    .endVertex();
                
                vertexConsumer
                    .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
                    .color(color)
                    .normal((float) normal.x, (float) normal.y, (float) normal.z)
                    .endVertex();
            }
            
            vertexConsumer
                .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
                .color(color)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
            
            if (isEnd) {
                vertexConsumer
                    .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
                    .color(0)
                    .normal((float) normal.x, (float) normal.y, (float) normal.z)
                    .endVertex();
            }
        }
        
        matrixStack.popPose();
    }
    
    static void renderPortalAreaGrid(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        ProtoPortalSide protoPortalSide,
        int color, PoseStack matrixStack
    ) {
        int separation = 8;
        
        Vec3 leftBottom = protoPortalSide.leftBottom;
        Vec3 rightBottom = protoPortalSide.rightBottom;
        Vec3 leftTop = protoPortalSide.leftTop;
        
        Vec3 xAxis = rightBottom.subtract(leftBottom);
        Vec3 yAxis = leftTop.subtract(leftBottom);
        
        Vec3 normal = xAxis.cross(yAxis).normalize();
        
        matrixStack.pushPose();
        matrixStack.translate(
            leftBottom.x - cameraPos.x,
            leftBottom.y - cameraPos.y,
            leftBottom.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        for (int i = 0; i <= separation; i++) {
            double ratio = (double) i / separation;
            
            Vec3 lineStart = xAxis.scale(ratio);
            Vec3 lineEnd = xAxis.scale(ratio).add(yAxis);
            
            putLine(vertexConsumer, color, yAxis.normalize(), matrix, lineStart, lineEnd);
        }
        
        for (int i = 0; i <= separation; i++) {
            double ratio = (double) i / separation;
            
            Vec3 lineStart = yAxis.scale(ratio);
            Vec3 lineEnd = yAxis.scale(ratio).add(xAxis);
            
            putLine(vertexConsumer, color, xAxis.normalize(), matrix, lineStart, lineEnd);
        }
        
        matrixStack.popPose();
    }
    
    private static void drawLockShape(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Vec3 center,
        int color, PoseStack matrixStack
    ) {
        matrixStack.pushPose();
        
        matrixStack.translate(
            center.x - cameraPos.x,
            center.y - cameraPos.y,
            center.z - cameraPos.z
        );
        
        matrixStack.mulPose(
            DQuaternion.rotationByDegrees(
                new Vec3(0, 1, 0),
                CHelper.getSmoothCycles(60) * 360
            ).toMcQuaternion()
        );
        
        float scale = 1.0f / 3000;
        matrixStack.scale(scale, scale, scale);
        
        Matrix4f matrix = matrixStack.last().pose();
        
        double w = 380;
        double h = 270;
        double ringWidth = 60;
        double ringAreaWidth = 152;
        double rightAreaHeight = 136;
        
        Vec3[] lineVertices = new Vec3[]{
            // the body
            new Vec3(w / 2, h / 2, 0),
            new Vec3(-w / 2, h / 2, 0),
            new Vec3(-w / 2, h / 2, 0),
            new Vec3(-w / 2, -h / 2, 0),
            new Vec3(-w / 2, -h / 2, 0),
            new Vec3(w / 2, -h / 2, 0),
            new Vec3(w / 2, -h / 2, 0),
            new Vec3(w / 2, h / 2, 0),
            
            // the ring inner edges
            new Vec3(ringAreaWidth / 2, h / 2, 0),
            new Vec3(ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(-ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(-ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(-ringAreaWidth / 2, h / 2, 0),
            
            // the ring outer edges
            new Vec3(ringAreaWidth / 2 + ringWidth, h / 2, 0),
            new Vec3(ringAreaWidth / 2 + ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(ringAreaWidth / 2 + ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(-ringAreaWidth / 2 - ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(-ringAreaWidth / 2 - ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(-ringAreaWidth / 2 - ringWidth, h / 2, 0),
        };
        
        for (int i = 0; i < lineVertices.length / 2; i++) {
            putLine(vertexConsumer, color, matrix, lineVertices[i * 2], lineVertices[i * 2 + 1]);
        }
        
        matrixStack.popPose();
    }
    
    private static void putLine(VertexConsumer vertexConsumer, int color, Matrix4f matrix, Vec3 lineStart, Vec3 lineEnd) {
        putLine(vertexConsumer, color, lineEnd.subtract(lineStart).normalize(), matrix, lineStart, lineEnd);
    }
    
    private static void putLine(VertexConsumer vertexConsumer, int color, Vec3 normal, Matrix4f matrix, Vec3 lineStart, Vec3 lineEnd) {
        vertexConsumer
            .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
            .color(color)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
            .color(color)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
    }
    
    private static void putLineToLineStrip(
        VertexConsumer vertexConsumer, int color, Vec3 normal,
        Matrix4f matrix, Vec3 lineStart, Vec3 lineEnd
    ) {
        // use alpha 0 vertices to "jump" without leaving visible line
        vertexConsumer
            .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
            .color(0)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
            .color(color)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
            .color(color)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
            .color(0)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
    }
    
    public static void renderRectLine(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        RenderedRect rect,
        int partCount, int color, double shrinkFactor,
        PoseStack matrixStack
    ) {
        matrixStack.pushPose();
        
        matrixStack.translate(
            rect.center().x - cameraPos.x,
            rect.center().y - cameraPos.y,
            rect.center().z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        Vec3 normal = rect.orientation().rotate(new Vec3(0, 0, 1));
        Vec3 axisW = rect.orientation().rotate(new Vec3(1, 0, 0));
        Vec3 axisH = rect.orientation().rotate(new Vec3(0, 1, 0));
        
        Vec3 facingOffset = normal.scale(0.01);
        
        Random random = new Random(color);
        
        Vec3[] vertices = new Vec3[]{
            axisW.scale(shrinkFactor * rect.width() / 2)
                .add(axisH.scale(shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            axisW.scale(shrinkFactor * rect.width() / 2)
                .add(axisH.scale(-1 * shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            axisW.scale(-1 * shrinkFactor * rect.width() / 2)
                .add(axisH.scale(-1 * shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            axisW.scale(-1 * shrinkFactor * rect.width() / 2)
                .add(axisH.scale(shrinkFactor * rect.height() / 2))
                .add(facingOffset),
        };
        
        int lineNum = vertices.length;
        
        
        for (int i = 0; i < partCount; i++) {
            double offset = CHelper.getSmoothCycles(random.nextInt(30, 300));
//            double offset = getRandomSmoothCycle(random);
            
            double totalStartRatio = ((double) i * 2) / (partCount * 2) + offset;
            double totalEndRatio = ((double) i * 2 + 1) / (partCount * 2) + offset;
            
            renderSubLineInLineLoop(
                vertexConsumer, matrix,
                vertices, color, totalStartRatio, totalEndRatio
            );
        }
        
        matrixStack.popPose();
    }
    
    public static void renderSubLineInLineLoop(
        VertexConsumer vertexConsumer, Matrix4f matrix,
        Vec3[] lineVertices, int color,
        double totalStartRatio, double totalEndRatio
    ) {
        int lineNum = lineVertices.length;
        
        double startRatioByLine = totalStartRatio * lineNum;
        double endRatioByLine = totalEndRatio * lineNum;
        
        int startRatioLineIndex = (int) Math.floor(startRatioByLine);
        int endRatioLineIndex = (int) Math.floor(endRatioByLine);
        
        for (int lineIndex = startRatioLineIndex; lineIndex <= endRatioLineIndex; lineIndex++) {
            double startLimit = lineIndex;
            double endLimit = lineIndex + 1;
            
            double startRatio = Math.max(startLimit, startRatioByLine);
            double endRatio = Math.min(endLimit, endRatioByLine);
            
            putLinePart(
                vertexConsumer, color, matrix,
                lineVertices[Math.floorMod(lineIndex, lineNum)],
                lineVertices[Math.floorMod(lineIndex + 1, lineNum)],
                startRatio - lineIndex,
                endRatio - lineIndex
            );
        }
    }
    
    private static void putLinePart(
        VertexConsumer vertexConsumer, int color,
        Matrix4f matrix, Vec3 lineStart, Vec3 lineEnd,
        double startRatio, double endRatio
    ) {
        Vec3 vec = lineEnd.subtract(lineStart);
        
        Vec3 partStartPos = lineStart.add(vec.scale(startRatio));
        Vec3 partEndPos = lineStart.add(vec.scale(endRatio));
        
        putLine(
            vertexConsumer, color, matrix, partStartPos, partEndPos
        );
    }
}
