package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.FPSMonitor;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.text.TranslatableText;
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
    
    public static RegistryKey<World> originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameMode originalGameMode;
    public static float tickDelta = 0;
    
    public static Set<RegistryKey<World>> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<Portal>>> lastPortalRenderInfos = new ArrayList<>();
    public static List<List<WeakReference<Portal>>> portalRenderInfos = new ArrayList<>();
    
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
    
    public static void updatePreRenderInfo(
        float tickDelta_
    ) {
        
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
        
        originalCameraLightPacked = MyRenderHelper.client.getEntityRenderManager()
            .getLight(MyRenderHelper.client.cameraEntity, tickDelta);
        
        updateIsLaggy();
        
        debugText = "";
//        MyRenderHelper.debugText = String.valueOf(((IEEntity) client.player).getCollidingPortal());

//        if (ClientTeleportationManager.isTeleportingTick) {
//            Helper.log("frame "+tickDelta_);
//        }
    }
    
    //protect the player from mirror room lag attack
    private static void updateIsLaggy() {
        if (!Global.lagAttackProof) {
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
        if (renderedScalingPortal) {
            setViewBobFactor(0);
            renderedScalingPortal = false;
            return;
        }
        
        Vec3d cameraPosVec = cameraEntity.getCameraPosVec(tickDelta);
        double minPortalDistance = CHelper.getClientNearbyPortals(10)
            .map(portal -> portal.getDistanceToNearestPointInPortal(cameraPosVec))
            .min(Double::compareTo).orElse(100.0);
        if (minPortalDistance < 2) {
            if (minPortalDistance > 1) {
                setViewBobFactor(minPortalDistance - 1);
            }
            else {
                setViewBobFactor(0);
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
        gameRenderer.setLightmapTextureManager(CGlobal.clientWorldLoader
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
    
}
