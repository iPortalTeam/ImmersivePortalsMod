package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.CHelper;
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

public class PehkuiInterfaceInitializer {
    
    public static void init() {
        if (!O_O.isDedicatedServer()) {
            PehkuiInterface.onClientPlayerTeleported = PehkuiInterfaceInitializer::onPlayerTeleportedClient;
        }
        
        PehkuiInterface.onServerEntityTeleported = PehkuiInterfaceInitializer::onEntityTeleportedServer;
    }
    
    @Environment(EnvType.CLIENT)
    private static void onPlayerTeleportedClient(Portal portal) {
        if (portal.hasScaling() && portal.teleportChangesScale) {
            MinecraftClient client = MinecraftClient.getInstance();
            
            ClientPlayerEntity player = client.player;
            
            Validate.notNull(player);
            
            doScalingForEntity(player, portal);
            
            IECamera camera = (IECamera) client.gameRenderer.getCamera();
            camera.setCameraY(
                ((float) (camera.getCameraY() * portal.scaling)),
                ((float) (camera.getLastCameraY() * portal.scaling))
            );
        }
    }
    
    private static void onEntityTeleportedServer(Entity entity, Portal portal) {
        if (portal.hasScaling() && portal.teleportChangesScale) {
            doScalingForEntity(entity, portal);
            
            if (entity.getVehicle() != null) {
                doScalingForEntity(entity.getVehicle(), portal);
            }
        }
    }
    
    private static void doScalingForEntity(Entity entity, Portal portal) {
        Vec3d eyePos = McHelper.getEyePos(entity);
        Vec3d lastTickEyePos = McHelper.getLastTickEyePos(entity);
        
        try {
            float oldScale = PehkuiInterface.getBaseScale.apply(entity, 1.0f);
            float newScale = transformScale(portal, oldScale);
            
            if (!entity.world.isClient && isScaleIllegal(newScale)) {
                newScale = 1;
                entity.sendSystemMessage(
                    new LiteralText("Scale out of range"),
                    Util.NIL_UUID
                );
            }
            
            PehkuiInterface.setBaseScale.accept(entity, newScale);
        }
        catch (Throwable e) {
            e.printStackTrace();
            CHelper.printChat("Something went wrong with Pehkui");
        }
        
        if (!entity.world.isClient) {
            ModMain.serverTaskList.addTask(() -> {
                McHelper.setEyePos(entity, eyePos, lastTickEyePos);
                McHelper.updateBoundingBox(entity);
                return true;
            });
        }
        else {
            McHelper.setEyePos(entity, eyePos, lastTickEyePos);
            McHelper.updateBoundingBox(entity);
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
