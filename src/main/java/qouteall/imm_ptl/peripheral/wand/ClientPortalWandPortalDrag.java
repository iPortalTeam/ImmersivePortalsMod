package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;
import qouteall.q_misc_util.my_util.animation.Animated;
import qouteall.q_misc_util.my_util.animation.RenderedPlane;
import qouteall.q_misc_util.my_util.animation.RenderedRect;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientPortalWandPortalDrag {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Nullable
    private static UUID selectedPortalId;
    
    private static final EnumMap<PortalCorner, Vec3> lockedCorners =
        new EnumMap<PortalCorner, Vec3>(PortalCorner.class);
    
    @Nullable
    private static PortalCorner selectedCorner;
    
    public static Animated<Vec3> cursor = new Animated<>(
        Animated.VEC_3_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.circle::mapProgress,
        null
    );
    
    public static Animated<RenderedRect> renderedRect = new Animated<>(
        Animated.RENDERED_RECT_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        null
    );
    
    @Nullable
    public static WithDim<Plane> draggingPlane;
    
    public static Animated<RenderedPlane> renderedPlane = new Animated<>(
        Animated.RENDERED_PLANE_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        RenderedPlane.NONE
    );
    
    public static void reset() {
        selectedPortalId = null;
        lockedCorners.clear();
        selectedCorner = null;
        cursor.clearTarget();
        renderedPlane.clearTarget();
    }
    
    public static void onLeftClick() {
    
    }
    
    public static void onRightClick() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        RenderedPlane target = renderedPlane.getTarget();
        
        if (target != null && target.plane() != null) {
            if (target.plane().dimension() == player.level.dimension()) {
                // start dragging
                draggingPlane = target.plane();
            }
        }
    }
    
    public static void tick() {
    
    }
    
    public static void updateDisplay() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        
        Vec3 viewVec = player.getLookAngle();
        
        if (draggingPlane != null) {
            if (player.level.dimension() == draggingPlane.dimension()) {
                Vec3 cursorPos = draggingPlane.value().raytrace(
                    eyePos,
                    eyePos.add(viewVec.scale(20))
                );
                cursor.setTarget(
                    cursorPos, Helper.secondToNano(0.5)
                );
                
                if (cursorPos != null) {
                    onDrag(cursorPos, draggingPlane);
                }
                return;
            }
            else {
                // stop dragging because not in the same dimension
                draggingPlane = null;
            }
        }
        
        Pair<Portal, Vec3> rayTraceResult = PortalUtils.lenientRayTracePortals(
            player.level,
            eyePos,
            eyePos.add(viewVec.scale(20)),
            false,
            Portal::isVisible,
            1.0
        ).orElse(null);
        
        if (rayTraceResult == null) {
            selectedCorner = null;
            cursor.clearTarget();
            renderedPlane.clearTarget();
            return;
        }
        
        Portal portal = rayTraceResult.getFirst();
        Vec3 hitPos = rayTraceResult.getSecond();
        
        if (selectedPortalId == null) {
            selectedPortalId = portal.getUUID();
        }
        else {
            if (!Objects.equals(selectedPortalId, portal.getUUID())) {
                if (canChangePortalSelection()) {
                    // change portal selection
                    selectedPortalId = portal.getUUID();
                    selectedCorner = null;
                    lockedCorners.clear();
                }
                else {
                    return;
                }
            }
        }
        
        PortalCorner corner = getClosestCorner(portal, hitPos);
        
        selectedCorner = corner;
        Vec3 cursorPos = corner.getPos(portal);
        cursor.setTarget(
            cursorPos, Helper.secondToNano(0.5)
        );
        renderedRect.setTarget(
            new RenderedRect(
                portal.getOriginDim(),
                portal.getOriginPos(),
                portal.getOrientationRotation(),
                portal.width, portal.height
            ),
            Helper.secondToNano(1.0)
        );
        
        Pair<Plane, MutableComponent> planeInfo =
            getPlayerFacingPlaneAligned(player, cursorPos, portal);
        Plane plane = planeInfo.getFirst();
        renderedPlane.setTarget(new RenderedPlane(
            new WithDim<>(player.level.dimension(), plane),
            1
        ), Helper.secondToNano(0.5));
    }
    
    private static void onDrag(Vec3 cursorPos, WithDim<Plane> draggingPlane) {
        Validate.notNull(ClientPortalWandPortalDrag.draggingPlane);
        if (selectedCorner == null) {
            LOGGER.error("selectedCorner is null");
            reset();
            return;
        }
    
        Portal portal = getSelectedPortal();
    
        if (portal == null) {
            LOGGER.error("cannot find portal to drag {}", selectedPortalId);
            reset();
            return;
        }
    
        ServerPortalWandInteraction.applyDrag(
            cursorPos, draggingPlane, portal, lockedCorners, selectedCorner
        );
    }
    
    @Nullable
    private static Portal getSelectedPortal() {
        if (selectedPortalId == null) {
            return null;
        }
        
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            selectedPortalId = null;
            return null;
        }
        
        Entity entity = ((IEWorld) player.level).portal_getEntityLookup().get(selectedPortalId);
        
        if (entity instanceof Portal portal) {
            return portal;
        }
        else {
            selectedPortalId = null;
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Pair<Plane, MutableComponent> getPlayerFacingPlaneAligned(
        Player player, Vec3 cursorPos, Portal portal
    ) {
        Pair<Plane, MutableComponent>[] candidates = new Pair[]{
            Pair.of(
                new Plane(cursorPos, new Vec3(1, 0, 0)),
                Component.translatable("imm_ptl.wand.plane.x")
            ),
            Pair.of(
                new Plane(cursorPos, new Vec3(0, 1, 0)),
                Component.translatable("imm_ptl.wand.plane.y")
            ),
            Pair.of(
                new Plane(cursorPos, new Vec3(0, 0, 1)),
                Component.translatable("imm_ptl.wand.plane.z")
            ),
            Pair.of(
                new Plane(cursorPos, portal.axisW),
                Component.translatable("imm_ptl.wand.plane.portal_x")
            ),
            Pair.of(
                new Plane(cursorPos, portal.axisH),
                Component.translatable("imm_ptl.wand.plane.portal_y")
            ),
            Pair.of(
                new Plane(cursorPos, portal.getNormal()),
                Component.translatable("imm_ptl.wand.plane.portal_z")
            )
        };
        
        Vec3 viewVec = player.getLookAngle();
        
        return Stream.concat(
            Arrays.stream(candidates),
            Arrays.stream(candidates).map(p -> Pair.of(p.getFirst().getOpposite(), p.getSecond()))
        ).min(
            Comparator.comparingDouble(p -> p.getFirst().normal.dot(viewVec))
        ).orElseThrow();
    }
    
    private static final int colorOfCursor = 0xff03fcfc;
    private static final int[] colorOfPortalSelection = new int[]{
        0xffec03fc, 0xfffca903
    };
    private static final int colorOfPlane = 0xffffffff;
    
    public static void render(
        PoseStack matrixStack,
        MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        if (PortalRendering.isRendering()) {
            return;
        }
        
        ResourceKey<Level> currDim = player.level.dimension();
        
        Vec3 cameraPos = new Vec3(camX, camY, camZ);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        
        
        Vec3 renderedCursor = cursor.getCurrent();
        if (renderedCursor != null) {
            WireRenderingHelper.renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedCursor,
                colorOfCursor, matrixStack
            );
        }
        
        RenderedRect rect = renderedRect.getCurrent();
        if (rect != null && rect.dimension() == currDim) {
            WireRenderingHelper.renderRectLine(
                vertexConsumer, cameraPos, rect,
                10, colorOfPortalSelection[0], 0.99,
                matrixStack
            );
            WireRenderingHelper.renderRectLine(
                vertexConsumer, cameraPos, rect,
                10, colorOfPortalSelection[1], 1.01,
                matrixStack
            );
        }
        
        VertexConsumer debugLineStripConsumer = bufferSource.getBuffer(RenderType.debugLineStrip(1));
        
        RenderedPlane plane = renderedPlane.getCurrent();
        if (plane != null && plane.plane() != null && plane.plane().dimension() == currDim) {
            Plane planeValue = plane.plane().value();
            
            // make the plane to follow the cursor
            // even the animation of the two are in different curve and duration
            if (renderedCursor != null) {
                planeValue = planeValue.getParallelPlane(renderedCursor);
            }
            
            WireRenderingHelper.renderPlane(
                debugLineStripConsumer, cameraPos, planeValue,
                plane.scale(), colorOfPlane,
                matrixStack
            );
        }
        
    }
    
    private static boolean canChangePortalSelection() {
        return draggingPlane == null && lockedCorners.isEmpty();
    }
    
    private static PortalCorner getClosestCorner(Portal portal, Vec3 hitPos) {
        return Arrays.stream(PortalCorner.values())
            .min(Comparator.comparingDouble(
                (PortalCorner corner) ->
                    corner.getPos(portal).distanceTo(hitPos)
            )).orElseThrow();
    }
}
