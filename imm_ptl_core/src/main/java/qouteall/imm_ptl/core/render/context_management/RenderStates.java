package qouteall.imm_ptl.core.render.context_management;

import net.minecraft.util.math.Vector4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.OFInterface;
import qouteall.imm_ptl.core.ducks.IEGameRenderer;
import qouteall.imm_ptl.core.ducks.IEWorldRenderer;
import qouteall.imm_ptl.core.miscellaneous.FPSMonitor;
import qouteall.imm_ptl.core.mixin.client.particle.IEParticle;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.QueryManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderStates {
    
    public static int frameIndex = 0;
    
    public static RegistryKey<World> originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameMode originalGameMode;
    public static float tickDelta = 0;
    public static Box originalPlayerBoundingBox;
    
    public static Set<RegistryKey<World>> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<PortalLike>>> lastPortalRenderInfos = new ArrayList<>();
    public static List<List<WeakReference<PortalLike>>> portalRenderInfos = new ArrayList<>();
    
    public static Vec3d lastCameraPos = Vec3d.ZERO;
    public static Vec3d cameraPosDelta = Vec3d.ZERO;
    
    public static boolean shouldForceDisableCull = false;
    
    public static long renderStartNanoTime;
    
    public static double viewBobFactor;
    
    //null indicates not gathered
    public static Matrix4f projectionMatrix;
    
    public static Camera originalCamera;
    
    public static int originalCameraLightPacked;
    
    public static String debugText;
    
    public static boolean isLaggy = false;
    
    public static boolean isRenderingEntities = false;
    
    public static boolean renderedScalingPortal = false;
    
//    public static Vector4f viewBobbingOffsetRotated = new Vector4f(0, 0, 0, 1);
//    public static Vec3d viewBobbingOffset = Vec3d.ZERO;
    
    public static Vec3d viewBobbedCameraPos = Vec3d.ZERO;
    
    public static void updatePreRenderInfo(
        float tickDelta_
    ) {
        ClientWorldLoader.initializeIfNeeded();
        
        Entity cameraEntity = MyRenderHelper.client.cameraEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        originalPlayerDimension = cameraEntity.world.getRegistryKey();
        originalPlayerPos = cameraEntity.getPos();
        originalPlayerLastTickPos = McHelper.lastTickPosOf(cameraEntity);
        PlayerListEntry entry = CHelper.getClientPlayerListEntry();
        originalGameMode = entry != null ? entry.getGameMode() : GameMode.CREATIVE;
        tickDelta = tickDelta_;
        
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
        
        FogRendererContext.update();
        
        renderStartNanoTime = System.nanoTime();
        
        updateViewBobbingFactor(cameraEntity);
        
        projectionMatrix = null;
        originalCamera = MyRenderHelper.client.gameRenderer.getCamera();
        
        originalCameraLightPacked = MyRenderHelper.client.getEntityRenderDispatcher()
            .getLight(MyRenderHelper.client.cameraEntity, tickDelta);
        
        updateIsLaggy();
        
        debugText = "";
//        debugText = originalCamera.getPos().toString();
        
        QueryManager.queryStallCounter = 0;
        
        Vec3d velocity = cameraEntity.getVelocity();
        originalPlayerBoundingBox = cameraEntity.getBoundingBox().stretch(
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
            if (FPSMonitor.getMinimumFps() > 15) {
                isLaggy = false;
            }
        }
        else {
            if (lastPortalRenderInfos.size() > 10) {
                if (FPSMonitor.getAverageFps() < 8 || FPSMonitor.getMinimumFps() < 6) {
                    MyRenderHelper.client.inGameHud.setOverlayMessage(
                        new TranslatableText("imm_ptl.laggy"),
                        false
                    );
                    isLaggy = true;
                }
            }
        }
    }
    
    private static void updateViewBobbingFactor(Entity cameraEntity) {
        if (lastPortalRenderInfos.size() != 0) {
            // view bobbing has issue with optifine
            if (OFInterface.isOptifinePresent) {
                setViewBobFactor(0);
                return;
            }
        }

//        if (renderedScalingPortal) {
//            setViewBobFactor(0);
//            renderedScalingPortal = false;
//            return;
//        }
        
        Vec3d cameraPosVec = cameraEntity.getCameraPosVec(tickDelta);
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
    
    private static void setViewBobFactor(double arg) {
        if (arg < viewBobFactor) {
            viewBobFactor = arg;
        }
        else {
            viewBobFactor = MathHelper.lerp(0.1, viewBobFactor, arg);
        }
    }
    
    public static void onTotalRenderEnd() {
        MinecraftClient client = MinecraftClient.getInstance();
        IEGameRenderer gameRenderer = (IEGameRenderer) MinecraftClient.getInstance().gameRenderer;
        gameRenderer.setLightmapTextureManager(ClientWorldLoader
            .getDimensionRenderHelper(client.world.getRegistryKey()).lightmapTexture);
        
        if (getRenderedPortalNum() != 0) {
            //recover chunk renderer dispatcher
            ((IEWorldRenderer) client.worldRenderer).getBuiltChunkStorage().updateCameraPosition(
                client.cameraEntity.getX(),
                client.cameraEntity.getZ()
            );
        }
        
        Vec3d currCameraPos = client.gameRenderer.getCamera().getPos();
        cameraPosDelta = currCameraPos.subtract(lastCameraPos);
        if (cameraPosDelta.lengthSquared() > 1) {
            cameraPosDelta = Vec3d.ZERO;
        }
        lastCameraPos = currCameraPos;
        
        
    }
    
    public static int getRenderedPortalNum() {
        return portalRenderInfos.size();
    }
    
    public static boolean isDimensionRendered(RegistryKey<World> dimensionType) {
        if (dimensionType == originalPlayerDimension) {
            return true;
        }
        return renderedDimensions.contains(dimensionType);
    }
    
    public static boolean shouldRenderParticle(Particle particle) {
        if (((IEParticle) particle).portal_getWorld() != MinecraftClient.getInstance().world) {
            return false;
        }
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            Vec3d particlePos = particle.getBoundingBox().getCenter();
            return renderingPortal.isInside(particlePos, 0.5);
        }
        return true;
    }
}
