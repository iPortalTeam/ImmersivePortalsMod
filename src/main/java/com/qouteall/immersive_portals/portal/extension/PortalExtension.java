package com.qouteall.immersive_portals.portal.extension;

import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.TransformationManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

// the additional features of a portal
public class PortalExtension {
    public double motionAffinity = 0;
    public boolean isSpecialFlippingPortal = false;
    
    public PortalExtension() {
    
    }
    
    public void readFromNbt(CompoundTag compoundTag) {
        if (compoundTag.contains("motionAffinity")) {
            motionAffinity = compoundTag.getDouble("motionAffinity");
        }
        if (compoundTag.contains("isSpecialFlippingPortal")) {
            isSpecialFlippingPortal = compoundTag.getBoolean("isSpecialFlippingPortal");
        }
        
    }
    
    public void writeToNbt(CompoundTag compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
        if (isSpecialFlippingPortal) {
            compoundTag.putBoolean("isSpecialFlippingPortal", isSpecialFlippingPortal);
        }
    }
    
    public void tick(Portal portal) {
        if (portal.world.isClient()) {
            tickClient(portal);
        }
    }
    
    @Environment(EnvType.CLIENT)
    private void tickClient(Portal portal) {
        if (isSpecialFlippingPortal) {
            
            updateFlippingPortalClientRotation(portal);
        }
    }
    
    /**
     * The flipping portal rotates 180 degrees.
     * The rotating axis can be in one plane and the actual transformation won't change
     * Change the rotating axis so that the rotating animation after the player cross the portal will be better
     * The default way is to not change the player's looking vec
     * {@link TransformationManager#onClientPlayerTeleported(Portal)}
     */
    @Environment(EnvType.CLIENT)
    private void updateFlippingPortalClientRotation(Portal portal) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null) {
            Vec3d cameraPos = player.getCameraPosVec(1);
            Vec3d delta = cameraPos.subtract(portal.getPos());
            Vec3d newRotatingAxis = delta.crossProduct(portal.getNormal()).normalize();
            if (newRotatingAxis.lengthSquared() > 0.1) {
                portal.rotation = new Quaternion(
                    new Vector3f(newRotatingAxis),
                    180, true
                );
            }
        }
    }
}
