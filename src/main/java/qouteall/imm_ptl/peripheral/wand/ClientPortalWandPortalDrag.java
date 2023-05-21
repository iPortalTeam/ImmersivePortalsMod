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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Sphere;
import qouteall.q_misc_util.my_util.WithDim;
import qouteall.q_misc_util.my_util.animation.Animated;
import qouteall.q_misc_util.my_util.animation.RenderedPlane;
import qouteall.q_misc_util.my_util.animation.RenderedRect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class ClientPortalWandPortalDrag {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Nullable
    private static UUID selectedPortalId;
    
    private static final EnumSet<PortalCorner> lockedCorners =
        EnumSet.noneOf(PortalCorner.class);
    
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
    
    // not null means dragging
    @Nullable
    public static DraggingContext draggingContext;
    
    private static record DraggingContext(
        ResourceKey<Level> dimension,
        @NotNull
        UUID portalId,
        @Nullable
        Plane limitingPlane,
        @Nullable
        Sphere limitingSphere,
        EnumSet<PortalCorner> lockedCorners,
        PortalCorner draggingCorner
    ) {
        
    }
    
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
        draggingContext = null;
    }
    
    public static void onLeftClick() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        if (selectedCorner != null) {
            if (lockedCorners.contains(selectedCorner)) {
                lockedCorners.remove(selectedCorner);
            }
            else {
                if (lockedCorners.size() >= 2) {
                    player.sendSystemMessage(Component.translatable("imm_ptl.wand.lock_limit"));
                    return;
                }
                
                lockedCorners.add(selectedCorner);
            }
        }
    }
    
    public static void onRightClick() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        if (isDragging()) {
            return;
        }
        
        startDragging();
    }
    
    public static void tick() {
        
    }
    
    public static boolean isDragging() {
        return draggingContext != null;
    }
    
    public static void updateDisplay() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        if (draggingContext != null) {
            handleDragging(player, draggingContext);
            return;
        }
        
        Portal originalSelectedPortal = getSelectedPortal();
        if (originalSelectedPortal == null) {
            reset();
        }
        
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        Vec3 viewVec = player.getLookAngle();
        
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
                if (originalSelectedPortal != null && Portal.isFlippedPortal(originalSelectedPortal, portal)) {
                    // going to select its flipped portal. transfer the locks and selection
                    List<PortalCorner> transferred = lockedCorners.stream().map(
                        c -> getClosestCorner(portal, c.getPos(originalSelectedPortal))
                    ).toList();
                    lockedCorners.clear();
                    lockedCorners.addAll(transferred);
                    if (selectedCorner != null) {
                        selectedCorner = getClosestCorner(portal, selectedCorner.getPos(originalSelectedPortal));
                    }
                }
                else {
                    // change portal selection, clear the locks and selection
                    selectedCorner = null;
                    lockedCorners.clear();
                }
                selectedPortalId = portal.getUUID();
            }
        }
        
        PortalCorner corner = getClosestCorner(portal, hitPos);
        
        selectedCorner = corner;
        Vec3 cursorPos = corner.getPos(portal);
        cursor.setTarget(
            cursorPos, Helper.secondToNano(0.5)
        );
        renderedRect.setTarget(
            portalToRenderedRect(portal),
            Helper.secondToNano(1.0)
        );
    }
    
    @NotNull
    private static RenderedRect portalToRenderedRect(Portal portal) {
        return new RenderedRect(
            portal.getOriginDim(),
            portal.getOriginPos(),
            portal.getOrientationRotation(),
            portal.width, portal.height
        );
    }
    
    private static void handleDragging(LocalPlayer player, DraggingContext draggingContext) {
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        Vec3 viewVec = player.getLookAngle();
        
        ResourceKey<Level> currDim = player.level.dimension();
        
        if (draggingContext.dimension != currDim) {
            stopDragging();
            return;
        }
        
        if (!Minecraft.getInstance().options.keyUse.isDown()) {
            stopDragging();
            return;
        }
        
        Vec3 cursorPos = null;
        
        Plane plane = draggingContext.limitingPlane();
        Sphere sphere = draggingContext.limitingSphere;
        
        int alignment = IPConfig.getConfig().portalWandCursorAlignment;
        
        if (plane != null) {
            cursorPos = plane.rayTrace(eyePos, viewVec);
            
            if (cursorPos != null) {
                cursorPos = align(cursorPos, alignment);
                
                cursorPos = plane.getProjection(cursorPos);
            }
        }
        else if (sphere != null) {
            cursorPos = sphere.rayTrace(eyePos, viewVec);
            
            if (cursorPos != null) {
                cursorPos = align(cursorPos, alignment);
                
                cursorPos = sphere.projectToSphere(cursorPos);
            }
        }
        
        if (cursorPos != null) {
            Vec3 originalCursorTarget = cursor.getTarget();
            if (originalCursorTarget != null && originalCursorTarget.distanceTo(cursorPos) < 0.001) {
                return;
            }
            
            cursor.setTarget(cursorPos, Helper.secondToNano(0.5));
            onDrag(cursorPos, draggingContext);
        }
    }
    
    private static Vec3 align(Vec3 pos, int alignment) {
        if (alignment == 0) {
            return pos;
        }
        
        return new Vec3(
            Math.round(pos.x() * (double) alignment) / (double) alignment,
            Math.round(pos.y() * (double) alignment) / (double) alignment,
            Math.round(pos.z() * (double) alignment) / (double) alignment
        );
    }
    
    private static void startDragging() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        if (isDragging()) {
            LOGGER.error("start dragging when already dragging");
            return;
        }
        
        if (!Minecraft.getInstance().options.keyUse.isDown()) {
            LOGGER.error("start dragging when not right clicking");
            return;
        }
        
        Portal portal = getSelectedPortal();
        
        if (portal == null) {
            return;
        }
        
        Vec3 cursorPos = cursor.getTarget();
        
        if (cursorPos == null) {
            return;
        }
        
        if (selectedCorner == null) {
            return;
        }
        
        if (lockedCorners.contains(selectedCorner)) {
            return;
        }
        
        List<PortalCorner> lockedCornersList = lockedCorners.stream().toList();
        
        int lockCornerNum = lockedCornersList.size();
        
        ResourceKey<Level> currDim = player.level.dimension();
        if (lockCornerNum == 0 || lockCornerNum == 1) {
            Pair<Plane, MutableComponent> info = getPlayerFacingPlaneAligned(player, cursorPos, portal);
            Plane plane = info.getFirst();
            
            renderedPlane.setTarget(new RenderedPlane(
                new WithDim<>(currDim, plane),
                1
            ), Helper.secondToNano(0.5));
            
            draggingContext = new DraggingContext(
                currDim,
                portal.getUUID(),
                plane,
                null,
                EnumSet.copyOf(lockedCorners),
                selectedCorner
            );
            
        }
        else if (lockCornerNum == 2) {
            PortalCorner.DraggingConstraint constraint = PortalCorner.getDraggingConstraintWith2LockedCorners(
                lockedCornersList.get(0), lockedCornersList.get(0).getPos(portal),
                lockedCornersList.get(1), lockedCornersList.get(1).getPos(portal),
                selectedCorner
            );
            
            if (constraint.plane() != null) {
                renderedPlane.setTarget(new RenderedPlane(
                    new WithDim<>(currDim, constraint.plane()), 1
                ), Helper.secondToNano(0.5));
            }
            
            draggingContext = new DraggingContext(
                currDim,
                portal.getUUID(),
                constraint.plane(),
                constraint.sphere(),
                EnumSet.copyOf(lockedCorners),
                selectedCorner
            );
        }
        else {
            LOGGER.error("invalid lockCornerNum {}", lockCornerNum);
            return;
        }
    }
    
    private static void stopDragging() {
        draggingContext = null;
        renderedPlane.clearTarget();
        renderedRect.clearTarget();
    }
    
    private static void onDrag(Vec3 cursorPos, DraggingContext draggingContext) {
        Portal portal = getPortalByUUID(draggingContext.portalId());
        
        if (portal == null) {
            LOGGER.error("cannot find portal to drag {}", selectedPortalId);
            reset();
            return;
        }
        
        PortalWandInteraction.DraggingInfo draggingInfo = new PortalWandInteraction.DraggingInfo(
            cursorPos, lockedCorners, selectedCorner
        );
        
        McRemoteProcedureCall.tellServerToInvoke(
            "qouteall.imm_ptl.peripheral.wand.PortalWandInteraction.RemoteCallables.requestApplyDrag",
            selectedPortalId, draggingInfo
        );
    }
    
    @Nullable
    private static Portal getSelectedPortal() {
        return getPortalByUUID(selectedPortalId);
    }
    
    @Nullable
    private static Portal getPortalByUUID(UUID portalId) {
        if (portalId == null) {
            return null;
        }
        
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return null;
        }
        
        Entity entity = ((IEWorld) player.level).portal_getEntityLookup().get(portalId);
        
        if (entity instanceof Portal portal) {
            return portal;
        }
        else {
            return null;
        }
    }
    
    private static Pair<Plane, MutableComponent> getPlayerFacingPlaneAligned(
        Player player, Vec3 cursorPos, Portal portal
    ) {
        ArrayList<Pair<Plane, MutableComponent>> candidates = new ArrayList<>();
        
        Vec3 X = new Vec3(1, 0, 0);
        Vec3 Y = new Vec3(0, 1, 0);
        Vec3 Z = new Vec3(0, 0, 1);
        
        candidates.add(Pair.of(
            new Plane(cursorPos, X),
            Component.translatable("imm_ptl.wand.plane.x")
        ));
        candidates.add(Pair.of(
            new Plane(cursorPos, Y),
            Component.translatable("imm_ptl.wand.plane.y")
        ));
        candidates.add(Pair.of(
            new Plane(cursorPos, Z),
            Component.translatable("imm_ptl.wand.plane.z")
        ));
        
        Predicate<Vec3> isOrthodox = p ->
            Math.abs(p.dot(X)) > 0.99 || Math.abs(p.dot(Y)) > 0.99 || Math.abs(p.dot(Z)) > 0.99;
        
        if (!isOrthodox.test(portal.axisW)) {
            candidates.add(Pair.of(
                new Plane(cursorPos, portal.axisW),
                Component.translatable("imm_ptl.wand.plane.portal_x")
            ));
        }
        
        if (!isOrthodox.test(portal.axisH)) {
            candidates.add(Pair.of(
                new Plane(cursorPos, portal.axisH),
                Component.translatable("imm_ptl.wand.plane.portal_y")
            ));
        }
        
        if (!isOrthodox.test(portal.getNormal())) {
            candidates.add(Pair.of(
                new Plane(cursorPos, portal.getNormal()),
                Component.translatable("imm_ptl.wand.plane.portal_z")
            ));
        }
        
        Vec3 viewVec = player.getLookAngle();
        
        return Stream.concat(
            candidates.stream(),
            candidates.stream().map(p -> Pair.of(p.getFirst().getOpposite(), p.getSecond()))
        ).min(
            Comparator.comparingDouble(p -> p.getFirst().normal.dot(viewVec))
        ).orElseThrow();
    }
    
    private static final int colorOfCursor = 0xff03fcfc;
    private static final int colorOfRect1 = 0xffec03fc;
    private static final int colorOfRect2 = 0xfffca903;
    private static final int colorOfPlane = 0xffffffff;
    private static final int colorOfLock = 0xffffffff;
    
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
        
        Vec3 renderedCursor = getCursorToRender();
        if (renderedCursor != null) {
            WireRenderingHelper.renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedCursor,
                colorOfCursor, matrixStack
            );
        }
        
        if (draggingContext != null) {
            Portal portal = getPortalByUUID(draggingContext.portalId);
            if (portal != null) {
                RenderedRect rect = portalToRenderedRect(portal);
                renderRectAndLock(
                    matrixStack, cameraPos, vertexConsumer,
                    rect,
                    draggingContext.lockedCorners()
                );
                renderedRect.setTarget(rect, 0);
            }
        }
        else {
            RenderedRect rect = renderedRect.getCurrent();
            if (rect != null && rect.dimension() == currDim) {
                renderRectAndLock(matrixStack, cameraPos, vertexConsumer, rect, lockedCorners);
            }
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
    
    @Nullable
    private static Vec3 getCursorToRender() {
        if (draggingContext != null) {
            Portal portal = getPortalByUUID(draggingContext.portalId);
            if (portal != null) {
                return draggingContext.draggingCorner().getPos(portal);
            }
            return null;
        }
        
        return cursor.getCurrent();
    }
    
    private static void renderRectAndLock(
        PoseStack matrixStack, Vec3 cameraPos, VertexConsumer vertexConsumer,
        RenderedRect rect, EnumSet<PortalCorner> lockedCorners
    ) {
        WireRenderingHelper.renderRectLine(
            vertexConsumer, cameraPos, rect,
            10, colorOfRect1, 0.99, 1,
            matrixStack
        );
        WireRenderingHelper.renderRectLine(
            vertexConsumer, cameraPos, rect,
            10, colorOfRect2, 1.01, -1,
            matrixStack
        );
        
        for (PortalCorner lockedCorner : lockedCorners) {
            Vec3 lockPos = rect.orientation().getAxisW()
                .scale(lockedCorner.getXSign() * rect.width() / 2)
                .add(rect.orientation().getAxisH()
                    .scale(lockedCorner.getYSign() * rect.height() / 2)
                ).add(rect.center());
            
            WireRenderingHelper.renderLockShape(
                vertexConsumer, cameraPos,
                lockPos, colorOfLock, matrixStack
            );
        }
    }
    
    private static PortalCorner getClosestCorner(Portal portal, Vec3 hitPos) {
        return Arrays.stream(PortalCorner.values())
            .min(Comparator.comparingDouble(
                (PortalCorner corner) ->
                    corner.getPos(portal).distanceTo(hitPos)
            )).orElseThrow();
    }
}
