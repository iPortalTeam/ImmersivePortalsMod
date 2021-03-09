package com.qouteall.immersive_portals.teleportation;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

public class CrossPortalSound {
    @Nullable
    public static Vec3d getTransformedSoundPosition(
        ClientWorld soundWorld,
        Vec3d soundPos
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) {
            return null;
        }
        
        soundWorld.getProfiler().push("cross_portal_sound");
        
        Vec3d result = McHelper.getNearbyPortals(
            soundWorld, soundPos, 10
        ).filter(
            portal -> portal.getDestDim() == RenderStates.originalPlayerDimension &&
                portal.transformPoint(soundPos).distanceTo(RenderStates.originalPlayerPos) < 20
        ).findFirst().map(
            portal -> {
                // sound goes to portal then goes through portal then goes to player
                
                
                Vec3d playerCameraPos = RenderStates.originalPlayerPos.add(
                    0, client.player.getStandingEyeHeight(), 0
                );
                
                Vec3d soundEnterPortalPoint = portal.getNearestPointInPortal(soundPos);
                double soundEnterPortalDistance = soundEnterPortalPoint.distanceTo(soundPos);
                Vec3d soundExitPortalPoint = portal.transformPoint(soundEnterPortalPoint);
                
                Vec3d playerToSoundExitPoint = soundExitPortalPoint.subtract(playerCameraPos);
                
                // the distance between sound source and the portal is applied by
                //  moving the pos further away from the player
                Vec3d projectedPos = portal.getDestPos().add(
                    playerToSoundExitPoint.normalize().multiply(soundEnterPortalDistance)
                );
                
                // lerp to actual position when you get close to the portal
                // this helps smooth the transition when the player is going through the portal
                
                Vec3d actualPos = portal.transformPoint(soundPos);
                
                double playerDistanceToPortalDest = soundExitPortalPoint.distanceTo(playerCameraPos);
                final double fadeDistance = 5.0;
                // 0 means close, 1 means far
                double lerpRatio = MathHelper.clamp(
                    playerDistanceToPortalDest / fadeDistance, 0.0, 1.0
                );
                
                // do the lerp
                Vec3d resultPos = actualPos.add(projectedPos.subtract(actualPos).multiply(lerpRatio));
                
                return resultPos;
            }
        ).orElse(null);
        
        soundWorld.getProfiler().pop();
        
        return result;
    }
}
