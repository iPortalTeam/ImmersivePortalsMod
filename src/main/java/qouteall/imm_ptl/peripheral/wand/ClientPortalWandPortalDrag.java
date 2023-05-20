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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Animated;
import qouteall.q_misc_util.my_util.RenderedPlane;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ClientPortalWandPortalDrag {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static enum PortalCorner {
        LEFT_BOTTOM, LEFT_TOP, RIGHT_BOTTOM, RIGHT_TOP;
        
        public int getXSign() {
            return switch (this) {
                case LEFT_BOTTOM, LEFT_TOP -> -1;
                case RIGHT_BOTTOM, RIGHT_TOP -> 1;
            };
        }
        
        public int getYSign() {
            return switch (this) {
                case LEFT_BOTTOM, RIGHT_BOTTOM -> -1;
                case LEFT_TOP, RIGHT_TOP -> 1;
            };
        }
        
        public Vec3 getOffset(Portal portal) {
            return portal.axisW.scale((portal.width / 2) * getXSign())
                .add(portal.axisH.scale((portal.height / 2) * getYSign()));
        }
    }
    
    @Nullable
    private static UUID selectedPortalID;
    
    private static final EnumSet<PortalCorner> lockedCorners = EnumSet.noneOf(PortalCorner.class);
    
    @Nullable
    private static PortalCorner selectedCorner;
    
    private static boolean isDragging = false;
    
    public static Animated<Vec3> cursor = new Animated<>(
        Animated.VEC_3_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.circle::mapProgress,
        null
    );
    
    public static Animated<RenderedPlane> renderedPlane = new Animated<>(
        Animated.RENDERED_PLANE_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        RenderedPlane.NONE
    );
    
    public static void reset() {
        selectedPortalID = null;
        lockedCorners.clear();
        selectedCorner = null;
        cursor.clearTarget();
        renderedPlane.clearTarget();
    }
    
    public static void onLeftClick() {
        if (selectedPortalID == null) {
            return;
        }
    }
    
    public static void onRightClick() {
    
    }
    
    public static void updateDisplay() {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
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
            return;
        }
        
        Portal portal = rayTraceResult.getFirst();
        Vec3 hitPos = rayTraceResult.getSecond();
        
        UUID portalId = portal.getUUID();
        
        if (selectedPortalID == null) {
            selectedPortalID = portalId;
        }
        else {
            if (!selectedPortalID.equals(portalId)) {
                if (canChangePortalSelection()) {
                    // change portal selection
                    selectedPortalID = portalId;
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
        cursor.setTarget(
            corner.getOffset(portal), Helper.secondToNano(0.5)
        );
    }
    
    private static final int colorOfCursor = 0xffffffff;
    private static final int colorOfPortalSelection = 0xffffffff;
    
    public static void render(
        PoseStack matrixStack,
        MultiBufferSource.BufferSource bufferSource,
        double camX, double camY, double camZ
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        
        if (player == null) {
            return;
        }
        
        Vec3 cameraPos = new Vec3(camX, camY, camZ);
        
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        
        Vec3 renderedCursor = cursor.getCurrent();
        if (renderedCursor != null) {
            WireRenderingHelper.renderSmallCubeFrame(
                vertexConsumer, cameraPos, renderedCursor,
                colorOfCursor, matrixStack
            );
        }
        
        if (selectedPortalID != null) {
            Entity e = ((IEWorld) player.level).portal_getEntityLookup().get(selectedPortalID);
            if (e instanceof Portal selectedPortal) {
                WireRenderingHelper.renderPortalFrameDottedLine(
                    vertexConsumer, cameraPos, selectedPortal,
                    colorOfPortalSelection, matrixStack,
                    CHelper.getSmoothCycles(50)
                );
            }
        }
        
    }
    
    private static boolean canChangePortalSelection() {
        return !isDragging && lockedCorners.isEmpty();
    }
    
    private static PortalCorner getClosestCorner(Portal portal, Vec3 hitPos) {
        return Arrays.stream(PortalCorner.values())
            .min(Comparator.comparingDouble(
                (PortalCorner corner) ->
                    corner.getOffset(portal).distanceTo(hitPos)
            )).orElseThrow();
    }
}
