package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.animation.Animated;
import qouteall.q_misc_util.my_util.IntBox;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.animation.RenderedPlane;
import qouteall.q_misc_util.my_util.WithDim;

import java.util.Comparator;
import java.util.List;

/**
 * The process and relevant marking rendering is handled purely on client side.
 * When it finishes, it performs a remote procedure call to create the portal.
 */
@Environment(EnvType.CLIENT)
public class ClientPortalWandPortalCreation {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final Animated<Vec3> cursor = new Animated<>(
        Animated.VEC_3_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.circle::mapProgress,
        null
    );
    
    public static final Animated<RenderedPlane> renderedPlane = new Animated<>(
        Animated.RENDERED_PLANE_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        RenderedPlane.NONE
    );
    
    // the proto-portal determined by placed anchors
    @NotNull
    public static ProtoPortal protoPortal = new ProtoPortal();
    
    public static void reset() {
        protoPortal.reset();
        renderedPlane.clearTarget();
    }
    
    public static void undo() {
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
            renderedPlane.setTarget(
                new RenderedPlane(limitingPlane, 1.0),
                Helper.secondToNano(3.0)
            );
            
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
            renderedPlane.setTarget(
                RenderedPlane.NONE, Helper.secondToNano(0.5)
            );
            
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
    
    public static void render(
        PoseStack matrixStack,
        MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        ResourceKey<Level> currDim = player.level.dimension();
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        Vec3 cameraPos = new Vec3(camX, camY, camZ);
        
        WithDim<Circle> circle = protoPortal.getCursorConstraintCircle();
        
        Vec3 renderedCursor = ClientPortalWandPortalCreation.cursor.getCurrent();
        
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
            WireRenderingHelper.renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedProtoPortal.firstSide.leftBottom,
                colorOfFirstSideLeftBottom, matrixStack
            );
            
            if (renderedProtoPortal.firstSide.rightBottom != null) {
                WireRenderingHelper.renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.firstSide.rightBottom,
                    colorOfFirstSideRightBottom, matrixStack
                );
            }
            if (renderedProtoPortal.firstSide.leftTop != null) {
                WireRenderingHelper.renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.firstSide.leftTop,
                    colorOfFirstSideLeftUp, matrixStack
                );
                
                WireRenderingHelper.renderPortalAreaGrid(
                    vertexConsumer,
                    cameraPos,
                    renderedProtoPortal.firstSide,
                    colorOfFirstPortalArea,
                    matrixStack
                );
            }
        }
        
        if (renderedProtoPortal.secondSide != null && currDim == renderedProtoPortal.secondSide.dimension) {
            WireRenderingHelper.renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedProtoPortal.secondSide.leftBottom,
                colorOfSecondSideLeftBottom, matrixStack
            );
            
            if (renderedProtoPortal.secondSide.rightBottom != null) {
                WireRenderingHelper.renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.secondSide.rightBottom,
                    colorOfSecondSideRightBottom, matrixStack
                );
            }
            if (renderedProtoPortal.secondSide.leftTop != null) {
                WireRenderingHelper.renderSmallCubeFrame(
                    vertexConsumer, cameraPos, renderedProtoPortal.secondSide.leftTop,
                    colorOfSecondSideLeftUp, matrixStack
                );
                
                WireRenderingHelper.renderPortalAreaGrid(
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
            WireRenderingHelper.renderCircle(
                debugLineStripConsumer, cameraPos,
                renderedCircle.value(),
                colorOfCircle,
                matrixStack
            );
        }
        
        // render the plane (don't render the plane if renders circle)
        RenderedPlane currRenderedPlane = renderedPlane.getCurrent();
        if (currRenderedPlane != null &&
            currRenderedPlane.plane() != null &&
            currRenderedPlane.plane().dimension() == currDim &&
            renderedCircle == null
        ) {
            double scale = currRenderedPlane.scale();
            if (scale > 0.01) {
                WireRenderingHelper.renderPlane(
                    debugLineStripConsumer,
                    cameraPos, currRenderedPlane.plane().value(),
                    scale,
                    colorOfPlane,
                    matrixStack
                );
            }
        }
    }
    
}
