package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.particle.Particle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.miscellaneous.ClientPerformanceMonitor;
import qouteall.imm_ptl.core.mixin.client.particle.IEParticle;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.QueryManager;
import com.mojang.math.Matrix4f;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderStates {
    
    public static int frameIndex = 0;
    
    public static ResourceKey<Level> originalPlayerDimension;
    public static Vec3 originalPlayerPos = Vec3.ZERO;
    public static Vec3 originalPlayerLastTickPos = Vec3.ZERO;
    public static GameType originalGameMode;
    public static AABB originalPlayerBoundingBox;
    
    /**
     * This does not always equal Minecraft.getFrameTime.
     * It will be 0 right after ticking.
     */
    public static float tickDelta = 0; // TODO rename to partialTick in MC 1.20
    
    public static Set<ResourceKey<Level>> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<PortalLike>>> lastPortalRenderInfos = new ArrayList<>();
    public static List<List<WeakReference<PortalLike>>> portalRenderInfos = new ArrayList<>();
    public static int portalsRenderedThisFrame = 0;// mixins to sodium use that
    
    public static Vec3 lastCameraPos = Vec3.ZERO;
    public static Vec3 cameraPosDelta = Vec3.ZERO;
    
    public static boolean shouldForceDisableCull = false;
    
    public static long renderStartNanoTime;
    
    public static double viewBobFactor;
    
    public static Matrix4f basicProjectionMatrix;
    
    public static Camera originalCamera;
    
    public static int originalCameraLightPacked;
    
    public static String debugText;
    
    public static boolean isLaggy = false;
    
    public static boolean isRenderingEntities = false;
    
    public static boolean renderedScalingPortal = false;
    
    public static boolean isRenderingPortalWeather = false;
    
    public static void updatePreRenderInfo(
        float tickDelta_
    ) {
        ClientWorldLoader.initializeIfNeeded();
        
        Entity cameraEntity = MyRenderHelper.client.cameraEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        originalPlayerDimension = cameraEntity.level.dimension();
        originalPlayerPos = cameraEntity.position();
        originalPlayerLastTickPos = McHelper.lastTickPosOf(cameraEntity);
        PlayerInfo entry = CHelper.getClientPlayerListEntry();
        originalGameMode = entry != null ? entry.getGameMode() : GameType.CREATIVE;
        tickDelta = tickDelta_;
        
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
        portalsRenderedThisFrame = 0;
        
        FogRendererContext.update();
        
        renderStartNanoTime = System.nanoTime();
        
        updateViewBobbingFactor(cameraEntity);
        
        basicProjectionMatrix = null;
        originalCamera = MyRenderHelper.client.gameRenderer.getMainCamera();
        
        originalCameraLightPacked = MyRenderHelper.client.getEntityRenderDispatcher()
            .getPackedLightCoords(MyRenderHelper.client.cameraEntity, tickDelta);
        
        updateIsLaggy();
        
        debugText = "";
//        debugText = originalCamera.getPos().toString();
        
        QueryManager.queryStallCounter = 0;
        
        Vec3 velocity = McHelper.getWorldVelocity(cameraEntity);
        originalPlayerBoundingBox = cameraEntity.getBoundingBox().expandTowards(
            -velocity.x, -velocity.y, -velocity.z
        );
    }
    
    //protect the player from mirror room lag attack
    private static void updateIsLaggy() {
        if (!IPGlobal.lagAttackProof) {
            isLaggy = false;
            return;
        }
        if (isLaggy) {
            if (ClientPerformanceMonitor.getMinimumFps() > 15) {
                isLaggy = false;
            }
        }
        else {
            if (lastPortalRenderInfos.size() > 10) {
                if (ClientPerformanceMonitor.getAverageFps() < 8 || ClientPerformanceMonitor.getMinimumFps() < 6) {
                    MyRenderHelper.client.gui.setOverlayMessage(
                        Component.translatable("imm_ptl.laggy"),
                        false
                    );
                    isLaggy = true;
                }
            }
        }
    }
    
    private static void updateViewBobbingFactor(Entity cameraEntity) {
//        if (!IrisInterface.invoker.isIrisPresent()) {
//            if (renderedScalingPortal) {
//                setViewBobFactor(0);
//                renderedScalingPortal = false;
//                return;
//            }
//        }
        
        Vec3 cameraPosVec = cameraEntity.getEyePosition(tickDelta);
        double minPortalDistance = CHelper.getClientNearbyPortals(16)
            .map(portal -> portal.getDistanceToNearestPointInPortal(cameraPosVec))
            .min(Double::compareTo).orElse(100.0);
        if (minPortalDistance < 2) {
            if (minPortalDistance < 1) {
                setViewBobFactor(0);
            }
            else {
                setViewBobFactor(minPortalDistance - 1);
            }
        }
        else {
            setViewBobFactor(1);
        }
    }
    
    public static double getViewBobbingOffsetMultiplier() {
        if (!IPGlobal.viewBobbingReduce) {
            return 1;
        }
        
        double allScaling = PortalRendering.getExtraModelViewScaling();
        
        return viewBobFactor * allScaling;
    }
    
    private static void setViewBobFactor(double arg) {
        if (arg < viewBobFactor) {
            viewBobFactor = arg;
        }
        else {
            viewBobFactor = Mth.lerp(0.1, viewBobFactor, arg);
        }
    }
    
    public static void onTotalRenderEnd() {
        Minecraft client = Minecraft.getInstance();
        IEGameRenderer gameRenderer = (IEGameRenderer) Minecraft.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(ClientWorldLoader
            .getDimensionRenderHelper(client.level.dimension()).lightmapTexture);
        
        Vec3 currCameraPos = client.gameRenderer.getMainCamera().getPosition();
        cameraPosDelta = currCameraPos.subtract(lastCameraPos);
        if (cameraPosDelta.lengthSqr() > 1) {
            cameraPosDelta = Vec3.ZERO;
        }
        lastCameraPos = currCameraPos;
        
        
    }
    
    public static int getRenderedPortalNum() {
        return portalRenderInfos.size();
    }
    
    public static boolean isDimensionRendered(ResourceKey<Level> dimensionType) {
        if (dimensionType == originalPlayerDimension) {
            return true;
        }
        return renderedDimensions.contains(dimensionType);
    }
    
    public static boolean shouldRenderParticle(Particle particle) {
        if (((IEParticle) particle).portal_getWorld() != Minecraft.getInstance().level) {
            return false;
        }
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            Vec3 particlePos = particle.getBoundingBox().getCenter();
            return renderingPortal.isInside(particlePos, 0.5);
        }
        return true;
    }
}
