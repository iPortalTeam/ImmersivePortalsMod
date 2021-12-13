package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.PehkuiInterface;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.portal.Portal;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;

public class PehkuiInterfaceInitializer {
    
    public static class OnPehkuiPresent extends PehkuiInterface.Invoker {
        @Override
        public boolean isPehkuiPresent() {
            return true;
        }
        
        @Override
        public void onClientPlayerTeleported(Portal portal) {
            onPlayerTeleportedClient(portal);
        }
        
        @Override
        public void onServerEntityTeleported(Entity entity, Portal portal) {
            try {
                onEntityTeleportedServer(entity, portal);
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public float getScale(Entity entity) {
            try {
                return ScaleTypes.BASE.getScaleData(entity).getScale();
            }
            catch (Throwable e) {
                e.printStackTrace();
                return 1;
            }
        }
        
        @Override
        public float getMotionScale(Entity entity) {
            try {
                return ScaleTypes.MOTION.getScaleData(entity).getScale();
            }
            catch (Throwable e) {
                e.printStackTrace();
                return 1;
            }
        }
    }
    
    
    public static void init() {
        PehkuiInterface.invoker = new OnPehkuiPresent();
    }
    
    @Environment(EnvType.CLIENT)
    private static void onPlayerTeleportedClient(Portal portal) {
        try {
            if (portal.hasScaling()) {
                if (!portal.teleportChangesScale) {
                    return;
                }
                
                MinecraftClient client = MinecraftClient.getInstance();
                
                ClientPlayerEntity player = client.player;
                
                Validate.notNull(player);
                
                ScaleData scaleData = ScaleTypes.BASE.getScaleData(player);
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
        catch (Throwable e) {
            e.printStackTrace();
            CHelper.printChat("Something went wrong with Pehkui");
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
            ScaleData scaleData = ScaleTypes.BASE.getScaleData(entity);
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
            
            IPGlobal.serverTaskList.addTask(() -> {
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
        return (scale > IPGlobal.scaleLimit) || (scale < (1.0f / (IPGlobal.scaleLimit * 2)));
    }
    
}
