package qouteall.imm_ptl.peripheral.wand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalUtils;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.render.TransformationManager;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.peripheral.ImmPtlCustomOverlay;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.AARotation;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.WithDim;
import qouteall.q_misc_util.my_util.animation.Animated;
import qouteall.q_misc_util.my_util.animation.RenderedPoint;

import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ClientPortalWandPortalCopy {
    
    private static sealed interface Status permits Status_SelectPortal, Status_PlacingPortal {}
    
    private static final class Status_SelectPortal implements Status {
        public @Nullable UUID selectedPortalId;
    }
    
    private static final class Status_PlacingPortal implements Status {
        public final @NotNull PlacementRequirement placementRequirement;
        public final boolean isCut;
        
        public @Nullable UnilateralPortalState pendingPlacement;
        
        private Status_PlacingPortal(@NotNull PlacementRequirement placementRequirement, boolean isCut) {
            this.placementRequirement = placementRequirement;
            this.isCut = isCut;
        }
    }
    
    private static @NotNull Status status = new Status_SelectPortal();
    
    public static record PlacementRequirement(
        double width, double height
    ) {}
    
    public static Animated<RenderedPoint> cursor = new Animated<>(
        Animated.RENDERED_POINT_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        RenderedPoint.EMPTY
    );
    
    public static Animated<UnilateralPortalState> selection = new Animated<>(
        UnilateralPortalState.ANIMATION_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        null
    );
    
    public static Animated<DQuaternion> placementOrientation = new Animated<>(
        Animated.QUATERNION_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        DQuaternion.identity
    );
    
    public static Animated<Double> placementOffsetLen = new Animated<>(
        Animated.DOUBLE_TYPE_INFO,
        () -> RenderStates.renderStartNanoTime,
        TimingFunction.sine::mapProgress,
        0.0
    );
    
    public static void reset() {
        status = new Status_SelectPortal();
        cursor.clearTarget();
        selection.clearTarget();
        placementOrientation.clearTarget();
        placementOffsetLen.clearTarget();
    }
    
    public static void updateDisplay() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        
        if (player == null) {
            return;
        }
        
        Vec3 eyePos = player.getEyePosition(RenderStates.getPartialTick());
        Vec3 viewVec = player.getLookAngle();
        
        if (status instanceof Status_SelectPortal statusSelectPortal) {
            cursor.clearTarget();
            
            Pair<Portal, Vec3> rayTraceResult = PortalUtils.lenientRayTracePortals(
                player.level(),
                eyePos,
                eyePos.add(viewVec.scale(64)),
                false,
                Portal::isVisible,
                1.0
            ).orElse(null);
            
            if (rayTraceResult != null) {
                Portal portal = rayTraceResult.getFirst();
                
                selection.setTarget(portal.getThisSideState(), Helper.secondToNano(0.5));
                statusSelectPortal.selectedPortalId = portal.getUUID();
                
                ImmPtlCustomOverlay.putText(
                    Component.translatable(
                        "imm_ptl.wand.copy.prompt.copy_or_cut",
                        Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage(),
                        Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage()
                    )
                );
            }
            else {
                selection.clearTarget();
                statusSelectPortal.selectedPortalId = null;
                
                ImmPtlCustomOverlay.putText(
                    Component.translatable("imm_ptl.wand.copy.prompt.select_portal")
                );
            }
        }
        else if (status instanceof Status_PlacingPortal statusPlacingPortal) {
            selection.clearTarget();
            
            HitResult hitResult = player.pick(64, RenderStates.getPartialTick(), false);
            int alignment = IPConfig.getConfig().portalWandCursorAlignment;
            
            Vec3 preCursorPos;
            if (hitResult.getType() == HitResult.Type.BLOCK && (hitResult instanceof BlockHitResult blockHitResult)) {
                preCursorPos = blockHitResult.getLocation();
            }
            else {
                preCursorPos = eyePos.add(viewVec.scale(5));
            }
            
            Vec3 cursorPointing = WandUtil.alignOnBlocks(
                player.level(), preCursorPos, alignment
            );
            
            cursor.setTarget(
                new RenderedPoint(
                    new WithDim<>(player.level().dimension(), cursorPointing), 1.0
                ),
                Helper.secondToNano(0.5)
            );
            
            // the camera rotation is used for rotating the world onto local camera,
            // the conjugated rotation is for rotating local camera onto camera in world
            DQuaternion camRotInverse = TransformationManager.getPlayerCameraRotation().getConjugated();
            
            AARotation orientationRot = Arrays.stream(AARotation.values())
                .min(Comparator.comparingDouble(
                    r -> DQuaternion.distance(r.quaternion, camRotInverse)
                ))
                .orElseThrow();
            DQuaternion orientation = orientationRot.quaternion;
            
            PlacementRequirement req = statusPlacingPortal.placementRequirement;
            
            boolean isFacingUpOrDown = player.getXRot() > 45 || player.getXRot() < -45;
            double offsetLen = isFacingUpOrDown ? 0 : req.height() / 2;
            Vec3 offset = orientation.rotate(new Vec3(0, offsetLen, 0));
            Vec3 placementOrigin = cursorPointing.add(offset);
            
            UnilateralPortalState placement = new UnilateralPortalState(
                player.level().dimension(),
                placementOrigin,
                orientation,
                req.width(), req.height()
            );
            
            statusPlacingPortal.pendingPlacement = placement;
            
            placementOrientation.setTarget(orientation, Helper.secondToNano(0.5));
            placementOffsetLen.setTarget(offsetLen, Helper.secondToNano(0.5));
            
            ImmPtlCustomOverlay.putText(
                Component.translatable(
                    "imm_ptl.wand.copy.prompt.place_portal",
                    Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage(),
                    Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage()
                )
            );
        }
        else {
            throw new RuntimeException();
        }
    }
    
    public static void onLeftClick() {
        if (status instanceof Status_SelectPortal statusSelectPortal) {
            // cut the portal
            if (statusSelectPortal.selectedPortalId != null) {
                Portal portal = WandUtil.getClientPortalByUUID(statusSelectPortal.selectedPortalId);
                if (portal != null) {
                    status = new Status_PlacingPortal(
                        new PlacementRequirement(portal.width, portal.height),
                        true
                    );
                    McRemoteProcedureCall.tellServerToInvoke(
                        "qouteall.imm_ptl.peripheral.wand.PortalWandInteraction.RemoteCallables.copyCutPortal",
                        statusSelectPortal.selectedPortalId,
                        true
                    );
                }
            }
        }
        else if (status instanceof Status_PlacingPortal statusPlacingPortal) {
            // discard
            McRemoteProcedureCall.tellServerToInvoke(
                "qouteall.imm_ptl.peripheral.wand.PortalWandInteraction.RemoteCallables.clearPortalClipboard"
            );
            status = new Status_SelectPortal();
        }
    }
    
    public static void onRightClick() {
        if (status instanceof Status_SelectPortal statusSelectPortal) {
            // copy the portal
            if (statusSelectPortal.selectedPortalId != null) {
                Portal portal = WandUtil.getClientPortalByUUID(statusSelectPortal.selectedPortalId);
                if (portal != null) {
                    status = new Status_PlacingPortal(
                        new PlacementRequirement(portal.width, portal.height),
                        false
                    );
                    McRemoteProcedureCall.tellServerToInvoke(
                        "qouteall.imm_ptl.peripheral.wand.PortalWandInteraction.RemoteCallables.copyCutPortal",
                        statusSelectPortal.selectedPortalId,
                        false
                    );
                }
            }
        }
        else if (status instanceof Status_PlacingPortal statusPlacingPortal) {
            // confirm placing
            if (statusPlacingPortal.pendingPlacement != null) {
                McRemoteProcedureCall.tellServerToInvoke(
                    "qouteall.imm_ptl.peripheral.wand.PortalWandInteraction.RemoteCallables.confirmCopyCut",
                    statusPlacingPortal.pendingPlacement.position(),
                    statusPlacingPortal.pendingPlacement.orientation()
                );
                status = new Status_SelectPortal();
            }
        }
    }
    
    private static final int colorOfCursor = 0xff03fcfc;
    private static final int colorOfSelection1 = 0xff52FF92;
    private static final int colorOfSelection2 = 0xffFC933D;
    private static final int colorOfPendingPlacement1 = 0xff00ffff;
    private static final int colorOfPendingPlacement2 = 0xffDC4BFF;
    
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
        
        ResourceKey<Level> currDim = player.level().dimension();
        Vec3 cameraPos = new Vec3(camX, camY, camZ);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
        
        Vec3 cursorPos = null;
        RenderedPoint currentCursor = cursor.getCurrent();
        if (currentCursor != null) {
            WithDim<Vec3> posWithDim = currentCursor.pos();
            if (posWithDim != null) {
                if (posWithDim.dimension() == currDim) {
                    cursorPos = posWithDim.value();
                }
            }
        }
        
        if (cursorPos != null) {
            WireRenderingHelper.renderSmallCubeFrame(
                vertexConsumer, cameraPos, cursorPos,
                colorOfCursor, cursor.getCurrent().scale(), matrixStack
            );
        }
        
        UnilateralPortalState rect = selection.getCurrent();
        
        if (rect != null) {
            if (rect.dimension() == currDim) {
                WireRenderingHelper.renderRectFrameFlow(
                    matrixStack, cameraPos,
                    vertexConsumer, rect,
                    colorOfSelection1,
                    colorOfSelection2
                );
            }
        }
        
        if (status instanceof Status_PlacingPortal statusPlacingPortal) {
            DQuaternion orientation = placementOrientation.getCurrent();
            Double offsetLen = placementOffsetLen.getCurrent();
            Validate.isTrue(orientation != null);
            Validate.isTrue(offsetLen != null);
            
            Vec3 offset = orientation.rotate(new Vec3(0, offsetLen, 0));
            
            if (cursorPos != null) {
                PlacementRequirement req = statusPlacingPortal.placementRequirement;
                UnilateralPortalState placement = new UnilateralPortalState(
                    currDim,
                    cursorPos.add(offset),
                    orientation,
                    req.width(), req.height()
                );
                
                WireRenderingHelper.renderRectFrameFlow(
                    matrixStack, cameraPos,
                    vertexConsumer, placement,
                    colorOfPendingPlacement1,
                    colorOfPendingPlacement2
                );
            }
        }
    }
}
