package qouteall.imm_ptl.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.ducks.IECamera;
import qouteall.imm_ptl.core.portal.Portal;

@SuppressWarnings({"resource", "JavadocReference", "DanglingJavadoc"})
public class ScaleUtils {
    
    /**
     * It's the id of attribute modifier of scale.
     */
    public static final ResourceLocation IPORTAL_SCALING =
        ResourceLocation.fromNamespaceAndPath("iportal", "scaling");
    
    @Environment(EnvType.CLIENT)
    public static void onClientPlayerTeleported(Portal portal) {
        if (portal.hasScaling() && portal.isTeleportChangesScale()) {
            Minecraft client = Minecraft.getInstance();
            
            LocalPlayer player = client.player;
            
            Validate.notNull(player, "Player is null");
            
            doScalingForEntity(player, portal);
            
            IECamera camera = (IECamera) client.gameRenderer.getMainCamera();
            camera.ip_setCameraY(
                ((float) (camera.ip_getCameraY() * portal.getScaling())),
                ((float) (camera.ip_getLastCameraY() * portal.getScaling()))
            );
        }
    }
    
    public static void onServerEntityTeleported(Entity entity, Portal portal) {
        if (portal.hasScaling() && portal.isTeleportChangesScale()) {
            doScalingForEntity(entity, portal);
            
            if (entity.getVehicle() != null) {
                doScalingForEntity(entity.getVehicle(), portal);
            }
        }
    }
    
    public static @Nullable AttributeInstance getScaleAttr(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getAttributes().getInstance(Attributes.SCALE);
        }
        return null;
    }
    
    public static double getScale(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getScale();
        }
        return 1.0;
    }
    
    public static double getBaseScale(Entity entity) {
        AttributeInstance scaleAttr = getScaleAttr(entity);
        if (scaleAttr != null) {
            return scaleAttr.getBaseValue();
        }
        return 1.0;
    }
    
    public static double getIPortalScaling(Entity entity) {
        AttributeInstance scaleAttr = getScaleAttr(entity);
        if (scaleAttr == null) {
            return 1;
        }
        
        AttributeModifier modifier = scaleAttr.getModifier(IPORTAL_SCALING);
        if (modifier == null) {
            return 1;
        }
        
        /**
         * {@link AttributeInstance#calculateValue()}
         */
        return modifier.amount() + 1.0;
    }
    
    public static void setIPortalScaling(Entity entity, double newScale) {
        AttributeInstance scaleAttr = getScaleAttr(entity);
        if (scaleAttr == null) {
            return;
        }
        
        if (Math.abs(newScale - 1.0) < 0.0001) {
            scaleAttr.removeModifier(IPORTAL_SCALING);
            
            // it updates cached eyeHeight field, which is important in feet pos calculation
            entity.refreshDimensions();
            
            return;
        }
        
        /**
         * {@link AttributeInstance#calculateValue()}
         */
        scaleAttr.addOrReplacePermanentModifier(new AttributeModifier(
            IPORTAL_SCALING,
            newScale - 1.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        ));
        
        // it updates cached eyeHeight field, which is important in feet pos calculation
        entity.refreshDimensions();
    }
    
    public static void setBaseScale(Entity entity, double scale) {
        AttributeInstance scaleAttr = getScaleAttr(entity);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(scale);
            
            // it updates cached eyeHeight field, which is important in feet pos calculation
            entity.refreshDimensions();
        }
    }
    
    public static double computeThirdPersonScale(Entity entity) {
        return getScale(entity);
    }
    
    public static double computeBlockReachScale(Entity entity) {
        return getScale(entity);
    }
    
    public static double computeMotionScale(Entity entity) {
        return getScale(entity);
    }
    
    private static void doScalingForEntity(Entity entity, Portal portal) {
        Vec3 eyePos = McHelper.getEyePos(entity);
        Vec3 lastTickEyePos = McHelper.getLastTickEyePos(entity);
        
        double oldScale = ScaleUtils.getIPortalScaling(entity);
        double newScale = transformScale(portal, oldScale);
        
        if (!entity.level().isClientSide && isScaleIllegal(newScale)) {
            newScale = 1;
            entity.sendSystemMessage(
                Component.literal("Scale out of range")
            );
        }
        
        ScaleUtils.setIPortalScaling(entity, newScale);
        
        if (!entity.level().isClientSide) {
            McHelper.setEyePos(entity, eyePos, lastTickEyePos);
            McHelper.updateBoundingBox(entity);
        }
        else {
            McHelper.setEyePos(entity, eyePos, lastTickEyePos);
            McHelper.updateBoundingBox(entity);
        }
    }
    
    private static double transformScale(Portal portal, double oldScale) {
        double result = (double) (oldScale * portal.getScaling());
        
        // avoid deviation accumulating
        if (Math.abs(result - 1.0) < 0.0001) {
            result = 1;
        }
        
        return result;
    }
    
    private static boolean isScaleIllegal(double scale) {
        return (scale > IPGlobal.scaleLimit) || (scale < (1.0 / (IPGlobal.scaleLimit * 2)));
    }
    
}
