package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
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
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.Animated;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The process and relevant marking rendering is handled purely on client side.
 * When it finishes, it performs a remote procedure call to create the portal.
 */
@Environment(EnvType.CLIENT)
public class ClientPortalWandInteraction {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static Animated<Vec3> cursor = new Animated<>(
        Animated.VEC_3_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.circle::mapProgress,
        null
    );
    
    public static Animated<Double> renderedPlaneScale = new Animated<>(
        Animated.DOUBLE_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        0.0
    );
    
    @Nullable
    public static WithDim<Plane> renderedPlane;
    
    // the proto-portal determined by placed anchors
    @NotNull
    public static ProtoPortal protoPortal = new ProtoPortal();
    
    private static boolean messageInformed = false;
    
    public static void reset() {
        protoPortal.reset();
        renderedPlane = null;
    }
    
    public static void onLeftClick() {
        undo();
    }
    
    private static void undo() {
        protoPortal.undo();
    }
    
    public static void onRightClick() {
        Vec3 cursorTarget = cursor.getTarget();
        
        if (cursorTarget == null) {
            return;
        }
        
        ClientLevel world = Minecraft.getInstance().level;
        
        if (world == null) {
            return;
        }
        
        protoPortal.tryPlaceCursor(
            world.dimension(),
            cursorTarget
        );
    
        if (protoPortal.isComplete()) {
            finish();
        }
    }
    
    public static void clearCursorPointing() {
        cursor.clearTarget();
    }
    
    public static void updateDisplay() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        ResourceKey<Level> cursorLimitingDim = protoPortal.getCursorConstraintDim();
        
        if (cursorLimitingDim != null && player.level.dimension() != cursorLimitingDim) {
            cursor.clearTarget();
            return;
        }
        
        WithDim<Plane> limitingPlane = protoPortal.getCursorConstraintPlane();
        WithDim<Circle> limitingCircle = protoPortal.getCursorConstraintCircle();
        
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        Vec3 viewVec = player.getViewVector(RenderStates.getPartialTick());
        
        // update cursor
        Vec3 cursorPointing = null;
        int alignment = IPConfig.getConfig().portalWandCursorAlignment;
        if (limitingPlane != null) {
            renderedPlaneScale.setTarget(1.0, Helper.secondToNano(3.0));
            renderedPlane = limitingPlane;
            
            cursorPointing = limitingPlane.value().raytrace(eyePos, viewVec);
            
            if (cursorPointing != null) {
                // align it and then project onto plane
                // aligning may cause it to be out of the plane
                cursorPointing = align(player.level, cursorPointing, alignment);
                cursorPointing = limitingPlane.value().getProjection(cursorPointing);
                
                if (limitingCircle != null) {
                    // align it into the circle
                    cursorPointing = limitingCircle.value().limitToCircle(cursorPointing);
                }
            }
        }
        else {
            renderedPlaneScale.setTarget(0.0, Helper.secondToNano(0.5));
            
            HitResult hitResult = player.pick(64, RenderStates.getPartialTick(), false);
            
            if (hitResult.getType() == HitResult.Type.BLOCK && (hitResult instanceof BlockHitResult blockHitResult)) {
                // if pointing at a block, use the aligned position on block
                cursorPointing = align(player.level, blockHitResult.getLocation(), alignment);
            }
        }
        
        // remove cursor if the placement is invalid
        
        if (cursorPointing != null) {
            ProtoPortal pendingState = protoPortal.copy();
            boolean canPlace = pendingState.tryPlaceCursor(player.level.dimension(), cursorPointing);
            if (!canPlace || !pendingState.isValidPlacement()) {
                cursorPointing = null;
                pendingState = null;
            }
            
            MutableComponent promptMessage = protoPortal.getPromptMessage(pendingState);
            if (promptMessage != null) {
                Minecraft.getInstance().gui.setOverlayMessage(
                    promptMessage, false
                );
            }
        }
        
        if (cursorPointing != null) {
            cursor.setTarget(cursorPointing, Helper.secondToNano(0.5));
        }
        else {
            cursor.clearTarget();
        }
    }
    
    private static Vec3 align(
        Level world, Vec3 vec3, int gridCount
    ) {
        if (gridCount == 0) {
            return vec3;
        }
        
        BlockPos blockPos = BlockPos.containing(vec3);
        return new IntBox(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))
            .stream()
            .flatMap(
                pos -> {
                    BlockState blockState = world.getBlockState(pos);
                    VoxelShape collisionShape = blockState.getCollisionShape(world, pos)
                        .move(pos.getX(), pos.getY(), pos.getZ());
                    List<AABB> aabbs = collisionShape.toAabbs();
                    if (aabbs.size() != 1) {
                        // in the case of hopper, not all of its collision boxes are symmetric
                        // without this, the north and south side of top edge mid-point of hopper cannot be selected
                        // also make air blocks alignable
                        aabbs.add(new AABB(pos));
                    }
                    return aabbs.stream();
                }
            )
            .map(box -> Helper.alignToBoxSurface(box, vec3, gridCount))
            .min(
                Comparator.comparingDouble(
                    p -> p.distanceToSqr(vec3)
                )
            ).orElse(null);
    }
    
    /**
     * {@link ServerPortalWandInteraction.RemoteCallables#finish(ServerPlayer, ProtoPortal)}
     */
    public static void finish() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        McRemoteProcedureCall.tellServerToInvoke(
            "qouteall.imm_ptl.peripheral.wand.ServerPortalWandInteraction.RemoteCallables.finish",
            protoPortal
        );
        
        reset();
    }
    
    // ARGB
    private static final int colorOfFirstSideLeftBottom = 0xfffb00ff;
    private static final int colorOfFirstSideRightBottom = 0xffe63262;
    private static final int colorOfFirstSideLeftUp = 0xfffcef60;
    
    private static final int colorOfSecondSideLeftBottom = 0xffaeff57;
    private static final int colorOfSecondSideRightBottom = 0xff57ffd2;
    private static final int colorOfSecondSideLeftUp = 0xffbdb3ff;
    
    private static final int colorOfPlane = 0xffafd3fa;
    private static final int colorOfCircle = 0xff03fce3;
    private static final int colorOfFirstPortalArea = 0xfffc9003;
    private static final int colorOfSecondPortalArea = 0xff60f2fc;

//    private static int getCursorColor() {
//        return switch (protoPortal.getStage()) {
//            case PlacingFirstSideLeftBottom -> colorOfFirstSideLeftBottom;
//            case PlacingFirstSideRightBottom -> colorOfFirstSideRightBottom;
//            case PlacingFirstSideLeftTop -> colorOfFirstSideLeftUp;
//            case PlacingSecondSideLeftBottom -> colorOfSecondSideLeftBottom;
//            case PlacingSecondSideRightBottom -> colorOfSecondSideRightBottom;
//            case PlacingSecondSideLeftTop -> colorOfSecondSideLeftUp;
//            case Completed -> 0xffffffff;
//        };
//    }
    
    public static void render(
        PoseStack matrixStack,
        MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        if (player.getMainHandItem().getItem() != PortalWandItem.instance) {
            return;
        }
        
        ResourceKey<Level> currDim = player.level.dimension();
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        Vec3 cameraPos = new Vec3(camX, camY, camZ);
        
        WithDim<Circle> circle = protoPortal.getCursorConstraintCircle();
        
        Vec3 renderedCursor = ClientPortalWandInteraction.cursor.getCurrent();
        
        if (circle != null && renderedCursor != null) {
            // the cursor interpolates along line
            // it may not always be on the circle
            renderedCursor = circle.value().limitToCircle(renderedCursor);
        }
        
        ProtoPortal renderedProtoPortal = protoPortal;
        
        if (renderedCursor != null) {
            ProtoPortal pending = protoPortal.copy();
            boolean canPlace = pending.tryPlaceCursor(currDim, renderedCursor);
            
            if (canPlace) {
                renderedProtoPortal = pending;
            }
        }
        
        if (circle == null) {
            circle = protoPortal.getCursorConstraintCircle();
        }
        
        // render the proto-portal
        if (renderedProtoPortal.firstSide != null && currDim == renderedProtoPortal.firstSide.dimension) {
            renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedProtoPortal.firstSide.leftBottom,
                colorOfFirstSideLeftBottom, matrixStack
            );
            
            if (renderedProtoPortal.firstSide.rightBottom != null) {
                renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.firstSide.rightBottom,
                    colorOfFirstSideRightBottom, matrixStack
                );
            }
            if (renderedProtoPortal.firstSide.leftTop != null) {
                renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.firstSide.leftTop,
                    colorOfFirstSideLeftUp, matrixStack
                );
                
                renderPortalAreaGrid(
                    vertexConsumer,
                    cameraPos,
                    renderedProtoPortal.firstSide,
                    colorOfFirstPortalArea,
                    matrixStack
                );
            }
        }
        
        if (renderedProtoPortal.secondSide != null && currDim == renderedProtoPortal.secondSide.dimension) {
            renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedProtoPortal.secondSide.leftBottom,
                colorOfSecondSideLeftBottom, matrixStack
            );
            
            if (renderedProtoPortal.secondSide.rightBottom != null) {
                renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.secondSide.rightBottom,
                    colorOfSecondSideRightBottom, matrixStack
                );
            }
            if (renderedProtoPortal.secondSide.leftTop != null) {
                renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.secondSide.leftTop,
                    colorOfSecondSideLeftUp, matrixStack
                );
                
                renderPortalAreaGrid(
                    vertexConsumer,
                    cameraPos,
                    renderedProtoPortal.secondSide,
                    colorOfSecondPortalArea,
                    matrixStack
                );
            }
        }
        
        VertexConsumer debugLineStripConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(1));
        
        // render the circle
        WithDim<Circle> renderedCircle = circle != null ?
            circle : renderedProtoPortal.getCursorConstraintCircle();
        if (renderedCircle != null && renderedCircle.dimension() == currDim) {
            renderCircle(
                debugLineStripConsumer, cameraPos,
                renderedCircle.value(),
                colorOfCircle,
                matrixStack
            );
        }
        
        // render the plane (don't render the plane if renders circle)
        if (renderedPlane != null && renderedPlane.dimension() == currDim && renderedCircle == null) {
            Double scale = renderedPlaneScale.getCurrent();
            if (scale != null && scale > 0.01) {
                renderPlane(
                    debugLineStripConsumer,
                    cameraPos, renderedPlane.value(),
                    scale,
                    colorOfPlane,
                    matrixStack
                );
            }
        }
    }
    
    private static void renderSmallCubeFrame(
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
    
    private static void renderCircle(
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
    
    private static void renderPortalAreaGrid(
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
    
    public static void showSettings(Player player) {
        player.sendSystemMessage(Component.translatable("imm_ptl.wand.settings_1"));
        player.sendSystemMessage(Component.translatable("imm_ptl.wand.settings_alignment"));
        
        int[] alignments = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 32, 64};
        
        List<MutableComponent> alignmentSettingTexts = new ArrayList<>();
        for (int alignment : alignments) {
            MutableComponent textWithCommand = IPMcHelper.getTextWithCommand(
                Component.literal("1/" + alignment),
                "/imm_ptl_client_debug wand set_cursor_alignment " + alignment
            );
            alignmentSettingTexts.add(textWithCommand);
        }
        
        alignmentSettingTexts.add(IPMcHelper.getTextWithCommand(
            Component.translatable("imm_ptl.wand.no_alignment"),
            "/imm_ptl_client_debug wand set_cursor_alignment 0"
        ));
        
        player.sendSystemMessage(
            alignmentSettingTexts.stream().reduce(Component.literal(""), (a, b) -> a.append(" ").append(b))
        );
    }
    
}
