package qouteall.imm_ptl.peripheral.portal_generation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.Plane;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * WIP
 * The process and relevant marking rendering is handled purely on client side.
 * When it finishes, it performs a remote procedure call to create the portal.
 */
@Environment(EnvType.CLIENT)
public class ClientPortalWandInteraction {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Nullable
    public static Vec3 cursorPointing;
    
    @Nullable
    public static Vec3 renderedCursorPointing;
    
    @Nullable
    public static ResourceKey<Level> renderedPlaneDimension;
    @Nullable
    public static Plane renderedPlane;
    @Nullable
    public static Pair<Vec3, Double> renderedCircle;
    
    @Nullable
    public static ResourceKey<Level> firstSideDimension;
    // 3 points are enough for determining portal area
    @Nullable
    public static Vec3 firstSideLeftBottom;
    @Nullable
    public static Vec3 firstRightRightBottom;
    @Nullable
    public static Vec3 firstSideLeftUp;
    
    @Nullable
    public static Vec3 secondSideLeftBottom;
    @Nullable
    public static Vec3 secondSideRightBottom;
    @Nullable
    public static Vec3 secondSideLeftUp;
    @Nullable
    public static ResourceKey<Level> secondSideDimension;
    
    private static boolean messageInformed = false;
    
    public static void reset() {
        firstSideLeftBottom = null;
        firstRightRightBottom = null;
        firstSideLeftUp = null;
        firstSideDimension = null;
        
        secondSideLeftBottom = null;
        secondSideRightBottom = null;
        secondSideLeftUp = null;
        secondSideDimension = null;
    }
    
    public static void onRightClick() {
        if (cursorPointing == null) {
            return;
        }
        
        if (firstSideLeftBottom == null) {
            firstSideLeftBottom = cursorPointing;
            firstSideDimension = Minecraft.getInstance().level.dimension();
            return;
        }
        if (firstRightRightBottom == null) {
            firstRightRightBottom = cursorPointing;
            return;
        }
        if (firstSideLeftUp == null) {
            firstSideLeftUp = cursorPointing;
            return;
        }
        
        if (secondSideLeftBottom == null) {
            secondSideLeftBottom = cursorPointing;
            secondSideDimension = Minecraft.getInstance().level.dimension();
            return;
        }
        if (secondSideRightBottom == null) {
            secondSideRightBottom = cursorPointing;
            return;
        }
        if (secondSideLeftUp == null) {
            secondSideLeftUp = cursorPointing;
            finish();
        }
    }
    
    public static void clearCursorPointing() {
        cursorPointing = null;
        renderedCursorPointing = null;
        renderedPlane = null;
        renderedPlaneDimension = null;
        renderedCircle = null;
    }
    
    public static void updateDisplay() {
        cursorPointing = null;
        renderedPlane = null;
        renderedPlaneDimension = null;
        renderedCircle = null;
        
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        if (!messageInformed) {
            messageInformed = true;
            player.sendSystemMessage(
                IPMcHelper.getTextWithCommand(
                    Component.translatable("imm_ptl.show_portal_wand_instruction"),
                    "/imm_ptl_client_debug show_portal_wand_instruction"
                )
            );
        }
        
        boolean isPlacingFirstSide = firstSideDimension != null && (firstSideLeftBottom == null || firstSideLeftUp == null);
        if (isPlacingFirstSide) {
            if (player.level.dimension() != firstSideDimension) {
                return;
            }
        }
        
        boolean isPlacingSecondSide = secondSideDimension != null && (secondSideLeftBottom == null || secondSideLeftUp == null);
        if (isPlacingSecondSide) {
            if (player.level.dimension() != secondSideDimension) {
                return;
            }
        }
        
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        Vec3 viewVec = player.getViewVector(RenderStates.getPartialTick());
        
        Plane cursorLimitingPlane = getCursorLimitingPlane();
        
        if (cursorLimitingPlane != null) {
            cursorPointing = cursorLimitingPlane.raytrace(eyePos, viewVec);
            
            renderedPlane = cursorLimitingPlane;
            renderedPlaneDimension = player.level.dimension();
            
            if (cursorPointing != null) {
                // align it and then project onto plane
                // aligning may cause it to be out of the plane
                cursorPointing = align(player.level, cursorPointing);
                cursorPointing = cursorLimitingPlane.getProjection(cursorPointing);
                
                Pair<Vec3, Double> cursorLimitingCircle = getCursorLimitingCircle();
                
                if (cursorLimitingCircle != null) {
                    renderedCircle = cursorLimitingCircle;
                    
                    // align it into the circle
                    Vec3 center = cursorLimitingCircle.getFirst();
                    double radius = cursorLimitingCircle.getSecond();
                    
                    Vec3 offset = cursorPointing.subtract(center);
                    
                    if (offset.lengthSqr() > 0.001) {
                        cursorPointing = offset.normalize().scale(radius).add(center);
                    }
                    else {
                        cursorPointing = null;
                    }
                }
            }
        }
        else {
            HitResult hitResult = player.pick(100, RenderStates.getPartialTick(), false);
            
            if (hitResult.getType() == HitResult.Type.BLOCK && (hitResult instanceof BlockHitResult blockHitResult)) {
                // if pointing at a block, use the aligned position on block
                cursorPointing = align(player.level, blockHitResult.getLocation());
            }
        }
        
        updateMessage();
    }
    
    private static void updateMessage() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        if (firstSideLeftBottom == null) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.wand.first_side_left_bottom"), false
            );
            return;
        }
        
        if (firstRightRightBottom == null) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.wand.first_side_right_bottom"), false
            );
            return;
        }
        
        if (firstSideLeftUp == null) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.wand.first_side_left_up"), false
            );
            return;
        }
        
        if (secondSideLeftBottom == null) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.wand.second_side_left_bottom"), false
            );
            return;
        }
        
        if (secondSideRightBottom == null) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.wand.second_side_right_bottom"), false
            );
            return;
        }
        
        if (secondSideLeftUp == null) {
            Minecraft.getInstance().gui.setOverlayMessage(
                Component.translatable("imm_ptl.wand.second_side_left_up"), false
            );
            return;
        }
        
    }
    
    private static Vec3 align(
        Level world, Vec3 vec3
    ) {
        BlockPos blockPos = BlockPos.containing(vec3);
        List<Vec3> relevantPoints = new IntBox(
            blockPos.offset(-1, -1, -1),
            blockPos.offset(1, 1, 1)
        ).stream().flatMap(
            pos -> {
                BlockState blockState = world.getBlockState(pos);
                VoxelShape collisionShape = blockState.getCollisionShape(world, pos)
                    .move(pos.getX(), pos.getY(), pos.getZ());
                List<AABB> aabbs = collisionShape.toAabbs();
                if (aabbs.size() > 1) {
                    // in the case of hopper, not all of its collision boxes are symmetric
                    // without this, the north and south side of top edge mid-point of hopper cannot be selected
                    aabbs.add(new AABB(pos));
                }
                return aabbs.stream();
            }
        ).flatMap(
            box -> Helper.verticesAndEdgeMidpoints(box).stream()
        ).toList();
        
        if (relevantPoints.isEmpty()) {
            // if there are no block nearby
            // just align it to half-block points
            return new Vec3(
                Math.round(vec3.x * 2) / 2.0,
                Math.round(vec3.y * 2) / 2.0,
                Math.round(vec3.z * 2) / 2.0
            );
        }

        relevantPoints = Helper.deduplicateWithPrecision(relevantPoints, 4096);
        
        return relevantPoints.stream().min(
            Comparator.comparingDouble(
                p -> p.distanceTo(vec3)
            )
        ).orElse(null);
    }
    
    // when the bottom left and bottom right are determined,
    // the left up point can only be in a plane because the portal is a rectangle
    @Nullable
    private static Plane getCursorLimitingPlane() {
        if (firstSideLeftBottom != null && firstRightRightBottom != null && firstSideLeftUp == null) {
            return new Plane(
                firstSideLeftBottom,
                firstRightRightBottom.subtract(firstSideLeftBottom).normalize()
            );
        }
        
        if (secondSideLeftBottom != null && secondSideRightBottom != null && secondSideLeftUp == null) {
            return new Plane(
                secondSideLeftBottom,
                secondSideRightBottom.subtract(secondSideLeftBottom).normalize()
            );
        }
        
        return null;
    }
    
    // the pair contains the center of the circle and the radium
    @Nullable
    private static Pair<Vec3, Double> getCursorLimitingCircle() {
        if (firstSideLeftBottom != null && firstRightRightBottom != null && firstSideLeftUp != null &&
            secondSideLeftBottom != null && secondSideRightBottom != null &&
            secondSideLeftUp == null
        ) {
            Vec3 firstSideHorizontalAxis = firstRightRightBottom.subtract(firstSideLeftBottom);
            Vec3 firstSideVerticalAxis = firstSideLeftUp.subtract(firstSideLeftBottom);
            double firstSideWidth = firstSideHorizontalAxis.length();
            double firstSideHeight = firstSideVerticalAxis.length();
            
            if (Math.abs(firstSideWidth) < 0.001) {
                return null;
            }
            
            double heightDivideWidth = firstSideHeight / firstSideWidth;
            
            Vec3 secondSideHorizontalAxis = secondSideRightBottom.subtract(secondSideLeftBottom);
            double secondSideWidth = secondSideHorizontalAxis.length();
            
            double secondSideHeight = secondSideWidth * heightDivideWidth;
            
            return Pair.of(secondSideLeftBottom, secondSideHeight);
        }
        
        return null;
    }
    
    private static void finish() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        if (firstSideLeftBottom == null || firstRightRightBottom == null || firstSideLeftUp == null ||
            secondSideLeftBottom == null || secondSideRightBottom == null || secondSideLeftUp == null ||
            firstSideDimension == null || secondSideDimension == null
        ) {
            LOGGER.error("Portal wand interaction is not finished");
            return;
        }
        
        McRemoteProcedureCall.tellServerToInvoke(
            "qouteall.imm_ptl.peripheral.portal_generation.ServerPortalWandInteraction.RemoteCallables.finish",
            firstSideDimension, firstSideLeftBottom, firstRightRightBottom, firstSideLeftUp,
            secondSideDimension, secondSideLeftBottom, secondSideRightBottom, secondSideLeftUp
        );
        
        reset();
    }
    
    // ARGB
    private static final int colorOfFirstSideLeftBottom = 0xffeb2feb;
    private static final int colorOfFirstSideRightBottom = 0xffe63262;
    private static final int colorOfFirstSideLeftUp = 0xffffd9b5;
    
    private static final int colorOfSecondSideLeftBottom = 0xfff5fc6a;
    private static final int colorOfSecondSideRightBottom = 0xff8cfa43;
    
    private static final int colorOfCursor = 0xffffffff;
    
    public static void render(
        PoseStack matrixStack,
        MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        ClientLevel world = Minecraft.getInstance().level;
        
        if (world == null) {
            return;
        }
        
        // interpolate the cursor
        if (cursorPointing != null) {
            if (renderedCursorPointing == null) {
                renderedCursorPointing = cursorPointing;
            }
            else {
                renderedCursorPointing = renderedCursorPointing.lerp(cursorPointing, 0.1);
            }
        }
        else {
            renderedCursorPointing = null;
        }
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        Vec3 cameraPos = new Vec3(camX, camY, camZ);
        
        // render the cursor
        if (renderedCursorPointing != null) {
            renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedCursorPointing,
                colorOfCursor,
                matrixStack);
        }
        
        ResourceKey<Level> currDim = world.dimension();
        
        // render the existing anchor points
        if (firstSideDimension != null) {
            if (currDim == firstSideDimension) {
                if (firstSideLeftBottom != null) {
                    renderSmallCubeFrame(
                        vertexConsumer, cameraPos, firstSideLeftBottom,
                        colorOfFirstSideLeftBottom, matrixStack
                    );
                }
                if (firstRightRightBottom != null) {
                    renderSmallCubeFrame(
                        vertexConsumer, cameraPos, firstRightRightBottom,
                        colorOfFirstSideRightBottom, matrixStack
                    );
                }
                if (firstSideLeftUp != null) {
                    renderSmallCubeFrame(
                        vertexConsumer, cameraPos, firstSideLeftUp,
                        colorOfFirstSideLeftUp, matrixStack
                    );
                }
            }
            
            if (currDim == secondSideDimension) {
                if (secondSideLeftBottom != null) {
                    renderSmallCubeFrame(
                        vertexConsumer, cameraPos, secondSideLeftBottom,
                        colorOfSecondSideLeftBottom, matrixStack
                    );
                }
                if (secondSideRightBottom != null) {
                    renderSmallCubeFrame(
                        vertexConsumer, cameraPos, secondSideRightBottom,
                        colorOfSecondSideRightBottom, matrixStack
                    );
                }
            }
        }
        
        VertexConsumer debugLineStripConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(1));
        
        // render the limiting plane
        if (renderedPlane != null) {
            renderPlane(
                // use the debug line strip to make it more clear
                debugLineStripConsumer,
                cameraPos, renderedPlane,
                0xff037bfc,
                matrixStack
            );
            
            if (renderedCircle != null) {
                Pair<Vec3, Double> circle = getCursorLimitingCircle();
                if (circle != null) {
                    renderCircle(
                        debugLineStripConsumer, cameraPos,
                        renderedPlane, circle,
                        0xff03fce3,
                        matrixStack
                    );
                }
            }
        }
        
        if (firstSideLeftBottom != null && firstRightRightBottom != null && firstSideLeftUp != null) {
            renderFirstSidePortalArea(
                debugLineStripConsumer,
                cameraPos,
                firstSideLeftBottom, firstRightRightBottom, firstSideLeftUp,
                0xffffffff,
                matrixStack
            );
        }
    }
    
    private static void renderSmallCubeFrame(
        VertexConsumer vertexConsumer, Vec3 cameraPos, Vec3 boxCenter,
        int color,
        PoseStack matrixStack
    ) {
        Random random = new Random(color);
        
        double boxSize = boxCenter.distanceTo(cameraPos) * 0.04;
        
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
        
        Vec3 boxLowerPoint = boxCenter.add(-boxSize / 2, -boxSize / 2, -boxSize / 2);
        
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
    
    private static DQuaternion getRandomSmoothRotation(Random random) {
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
    
    @NotNull
    private static Vec3 randomVec(Random random) {
        return new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
    }
    
    private static void renderPlane(
        VertexConsumer vertexConsumer, Vec3 cameraPos, Plane plane, int color, PoseStack matrixStack
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        Vec3 planeCenter = plane.pos;
        Vec3 normal = plane.normal;
        
        Vec3 anyVecNonNormal = new Vec3(0, 1, 0);
        if (Math.abs(normal.dot(anyVecNonNormal)) > 0.9) {
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
        
        int lineNumPerSide = 10;
        double lineInterval = player.getEyePosition(RenderStates.getPartialTick()).distanceTo(planeCenter) * 0.2;
        double lineLenPerSide = lineNumPerSide * lineInterval;
        
        for (int ix = -lineNumPerSide; ix <= lineNumPerSide; ix++) {
            Vec3 lineStart = planeX.scale(ix * lineInterval)
                .add(planeY.scale(-lineLenPerSide));
            Vec3 lineEnd = planeX.scale(ix * lineInterval)
                .add(planeY.scale(lineLenPerSide));
            
            putLineToLineStrip(vertexConsumer, color, normal, matrix, lineStart, lineEnd);
        }
        
        for (int iy = -lineNumPerSide; iy <= lineNumPerSide; iy++) {
            Vec3 lineStart = planeY.scale(iy * lineInterval)
                .add(planeX.scale(-lineLenPerSide));
            Vec3 lineEnd = planeY.scale(iy * lineInterval)
                .add(planeX.scale(lineLenPerSide));
            
            putLineToLineStrip(vertexConsumer, color, normal, matrix, lineStart, lineEnd);
        }
        
        matrixStack.popPose();
    }
    
    private static void renderCircle(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Plane plane,
        Pair<Vec3, Double> circle,
        int color, PoseStack matrixStack
    ) {
        Vec3 planeCenter = plane.pos;
        Vec3 normal = plane.normal;
        
        Vec3 anyVecNonNormal = new Vec3(0, 1, 0);
        if (Math.abs(normal.dot(anyVecNonNormal)) > 0.9) {
            anyVecNonNormal = new Vec3(1, 0, 0);
        }
        
        Vec3 planeX = normal.cross(anyVecNonNormal).normalize();
        Vec3 planeY = normal.cross(planeX).normalize();
        
        Vec3 circleCenter = circle.getFirst();
        double circleRadius = circle.getSecond();
        
        matrixStack.pushPose();
        
        matrixStack.translate(
            circleCenter.x - cameraPos.x,
            circleCenter.y - cameraPos.y,
            circleCenter.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        int vertexNum = 50;
        
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
    
    private static void renderFirstSidePortalArea(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Vec3 firstSideLeftBottom, Vec3 firstRightRightBottom, Vec3 firstSideLeftUp,
        int color, PoseStack matrixStack
    ) {
        int separation = 10;
        
        Vec3 xAxis = firstRightRightBottom.subtract(firstSideLeftBottom);
        Vec3 yAxis = firstSideLeftUp.subtract(firstSideLeftBottom);
        
        Vec3 normal = xAxis.cross(yAxis).normalize();
        
        matrixStack.pushPose();
        matrixStack.translate(
            firstSideLeftBottom.x - cameraPos.x,
            firstSideLeftBottom.y - cameraPos.y,
            firstSideLeftBottom.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        for (int i = 0; i <= separation; i++) {
            double ratio = (double) i / separation;
            
            Vec3 lineStart = xAxis.scale(ratio);
            Vec3 lineEnd = xAxis.scale(ratio).add(yAxis);
            
            putLineToLineStrip(vertexConsumer, color, normal, matrix, lineStart, lineEnd);
        }
        
        for (int i = 0; i <= separation; i++) {
            double ratio = (double) i / separation;
            
            Vec3 lineStart = yAxis.scale(ratio);
            Vec3 lineEnd = yAxis.scale(ratio).add(xAxis);
            
            putLineToLineStrip(vertexConsumer, color, normal, matrix, lineStart, lineEnd);
        }
        
        matrixStack.popPose();
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
    
    private static void putLineToLineStrip(VertexConsumer vertexConsumer, int color, Vec3 normal, Matrix4f matrix, Vec3 lineStart, Vec3 lineEnd) {
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
    
}
