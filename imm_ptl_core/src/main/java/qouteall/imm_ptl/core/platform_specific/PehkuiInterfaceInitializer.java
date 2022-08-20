package qouteall.imm_ptl.core.platform_specific;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import virtuoel.pehkui.api.ScaleData;
import virtuoel.pehkui.api.ScaleTypes;
import virtuoel.pehkui.util.ScaleUtils;

public class PehkuiInterfaceInitializer {
    
    public static class OnPehkuiPresent extends PehkuiInterface.Invoker {
        
        private boolean loggedErrorMessage = false;
        
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
            onEntityTeleportedServer(entity, portal);
        }
        
        private void logErrorMessage(Entity entity, Throwable e, String situation) {
            e.printStackTrace();
            entity.sendSystemMessage(
                Component.literal("Something went wrong with Pehkui (" + situation + ")")
            );
        }
        
        @Override
        public float getBaseScale(Entity entity, float tickDelta) {
            try {
                return ScaleTypes.BASE.getScaleData(entity).getBaseScale(tickDelta);
            }
            catch (Throwable e) {
                if (!loggedErrorMessage) {
                    loggedErrorMessage = true;
                    logErrorMessage(entity, e, "getting scale");
                }
                return super.getBaseScale(entity, tickDelta);
            }
        }
        
        @Override
        public void setBaseScale(Entity entity, float scale) {
            try {
                final ScaleData data = ScaleTypes.BASE.getScaleData(entity);
                data.setScale(scale);
            }
            catch (Throwable e) {
                if (!loggedErrorMessage) {
                    loggedErrorMessage = true;
                    logErrorMessage(entity, e, "setting scale");
                }
            }
        }
        
        @Override
        public float computeThirdPersonScale(Entity entity, float tickDelta) {
            try {
                return ScaleTypes.THIRD_PERSON.getScaleData(entity).getScale(tickDelta);
            }
            catch (Throwable e) {
                if (!loggedErrorMessage) {
                    loggedErrorMessage = true;
                    logErrorMessage(entity, e, "getting third person scale");
                }
                return super.computeThirdPersonScale(entity, tickDelta);
            }
        }
        
        @Override
        public float computeBlockReachScale(Entity entity, float tickDelta) {
            try {
                return ScaleTypes.BLOCK_REACH.getScaleData(entity).getScale(tickDelta);
            }
            catch (Throwable e) {
                if (!loggedErrorMessage) {
                    loggedErrorMessage = true;
                    logErrorMessage(entity, e, "getting reach scale");
                }
                return super.computeBlockReachScale(entity, tickDelta);
            }
        }
        
        @Override
        public float computeMotionScale(Entity entity, float tickDelta) {
            try {
                return ScaleTypes.MOTION.getScaleData(entity).getScale(tickDelta);
            }
            catch (Throwable e) {
                if (!loggedErrorMessage) {
                    loggedErrorMessage = true;
                    logErrorMessage(entity, e, "getting motion scale");
                }
                return super.computeMotionScale(entity, tickDelta);
            }
        }
    }
    
    public static void init() {
        PehkuiInterface.invoker = new OnPehkuiPresent();
    }
    
    @Environment(EnvType.CLIENT)
    private static void onPlayerTeleportedClient(Portal portal) {
        if (portal.hasScaling() && portal.teleportChangesScale) {
            Minecraft client = Minecraft.getInstance();
            
            LocalPlayer player = client.player;
            
            Validate.notNull(player);
            
            doScalingForEntity(player, portal);
            
            IECamera camera = (IECamera) client.gameRenderer.getMainCamera();
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
        Vec3 eyePos = McHelper.getEyePos(entity);
        Vec3 lastTickEyePos = McHelper.getLastTickEyePos(entity);
        
        float oldScale = PehkuiInterface.invoker.getBaseScale(entity);
        float newScale = transformScale(portal, oldScale);
        
        if (!entity.level.isClientSide && isScaleIllegal(newScale)) {
            newScale = 1;
            entity.sendSystemMessage(
                Component.literal("Scale out of range")
            );
        }
        
        PehkuiInterface.invoker.setBaseScale(entity, newScale);
        
        if (!entity.level.isClientSide) {
            McHelper.setEyePos(entity, eyePos, lastTickEyePos);
            McHelper.updateBoundingBox(entity);
    
            float scaleTest = ScaleUtils.getEyeHeightScale(entity, 0.5f);
            Validate.isTrue(scaleTest >0.0001);
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
        return (scale > IPGlobal.scaleLimit) || (scale < (1.0f / (IPGlobal.scaleLimit * 2)));
    }
    
}
