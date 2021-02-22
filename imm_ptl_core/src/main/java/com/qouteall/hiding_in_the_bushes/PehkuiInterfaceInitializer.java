package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.PehkuiInterface;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.portal.Portal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleType;

public class PehkuiInterfaceInitializer {
    
    public static void init() {
        if (!O_O.isDedicatedServer()) {
            PehkuiInterface.onClientPlayerTeleported = PehkuiInterfaceInitializer::onPlayerTeleportedClient;
        }
        
        PehkuiInterface.onServerEntityTeleported = PehkuiInterfaceInitializer::onEntityTeleportedServer;
        
        PehkuiInterface.getScale = e -> ScaleData.of(e).getScale();
    }
    
    @Environment(EnvType.CLIENT)
    private static void onPlayerTeleportedClient(Portal portal) {
        if (portal.hasScaling()) {
            if (!portal.teleportChangesScale) {
                return;
            }
            
            MinecraftClient client = MinecraftClient.getInstance();
            
            ClientPlayerEntity player = client.player;
            
            Validate.notNull(player);
            
            ScaleData scaleData = ScaleType.BASE.getScaleData(player);
            Vec3d eyePos = McHelper.getEyePos(player);
            Vec3d lastTickEyePos = McHelper.getLastTickEyePos(player);
            
            float oldScale = scaleData.getBaseScale();
            final float newScale = transformScale(portal, oldScale);
            
            scaleData.setTargetScale(newScale);
            scaleData.setScale(newScale);
            scaleData.setScale(newScale);
            scaleData.tick();
            
            McHelper.setEyePos(player, eyePos, lastTickEyePos);
            McHelper.updateBoundingBox(player);
            
            IECamera camera = (IECamera) client.gameRenderer.getCamera();
            camera.setCameraY(
                ((float) (camera.getCameraY() * portal.scaling)),
                ((float) (camera.getLastCameraY() * portal.scaling))
            );
            
            
        }
    }
    
    private static void onEntityTeleportedServer(Entity entity, Portal portal) {
        doScalingForEntity(entity, portal);
        
        if (entity.getVehicle() != null) {
            doScalingForEntity(entity.getVehicle(), portal);
        }
    }
    
    private static void doScalingForEntity(Entity entity, Portal portal) {
        if (portal.hasScaling()) {
            if (!portal.teleportChangesScale) {
                return;
            }
            ScaleData scaleData = ScaleType.BASE.getScaleData(entity);
            Vec3d eyePos = McHelper.getEyePos(entity);
            Vec3d lastTickEyePos = McHelper.getLastTickEyePos(entity);
            
            float oldScale = scaleData.getBaseScale();
            float newScale = transformScale(portal, oldScale);
            
            if (isScaleIllegal(newScale)) {
                newScale = 1;
                entity.sendSystemMessage(
                    new LiteralText("Scale out of range"),
                    Util.NIL_UUID
                );
            }
            
            scaleData.setTargetScale(newScale);
            scaleData.setScale(newScale);
            scaleData.setScale(newScale);
            scaleData.tick();
            
            ModMain.serverTaskList.addTask(() -> {
                McHelper.setEyePos(entity, eyePos, lastTickEyePos);
                McHelper.updateBoundingBox(entity);
                return true;
            });
            
            scaleData.onUpdate();
        }
    }
    
    private static float transformScale(Portal portal, float oldScale) {
        float result = (float) (oldScale * portal.scaling);
        
        // avoid deviation accumulating
        if (Math.abs(result - 1.0f) < 0.0001f) {
            result = 1;
        }
        
        return result;
    }
    
    private static boolean isScaleIllegal(float scale) {
        return (scale > Global.scaleLimit) || (scale < (1.0f / (Global.scaleLimit * 2)));
    }
    
}
