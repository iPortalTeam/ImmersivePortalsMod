package qouteall.imm_ptl.core.teleportation;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.Comparator;

@Environment(EnvType.CLIENT)
public class CrossPortalSound {
    public static final float VOLUME_RADIUS_MULT = 16f;
    public static final float MIN_SOUND_RADIUS = 16f;
    
    public static boolean isPlayerWorld(ClientWorld world) {
        return world.getRegistryKey() == RenderStates.originalPlayerDimension;
    }
    
    @Nullable
    public static PositionedSoundInstance createCrossPortalSound(
        ClientWorld soundWorld,
        SoundEvent sound,
        SoundCategory category,
        Vec3d soundPos,
        float soundVol,
        float soundPitch
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) {
            return null;
        }
        
        soundWorld.getProfiler().push("cross_portal_sound");
        
        double soundRadius = Math.min(64, Math.max(VOLUME_RADIUS_MULT * soundVol, MIN_SOUND_RADIUS));
        Vec3d playerCameraPos = RenderStates.originalPlayerPos.add(
            0, client.player.getStandingEyeHeight(), 0
        );
        
        // find portals in range of the sound
        PositionedSoundInstance result = IPMcHelper.getNearbyPortals(
            soundWorld, soundPos, soundRadius
        ).filter(
            // keep portals in range of the player
            portal -> portal.getDestDim() == RenderStates.originalPlayerDimension &&
                isPlayerInRange(portal, soundPos, soundRadius, playerCameraPos)
        ).min(
            // use portal that is closest to the sound
            Comparator.comparingDouble(portal -> getPortalDistance(portal, soundPos))
        ).map(
            portal -> {
                // set sound position to the point the sound would exit the portal
                Vec3d soundEnterPortalPoint = portal.getNearestPointInPortal(soundPos);
                Vec3d soundExitPortalPoint = portal.transformPoint(soundEnterPortalPoint);
                
                // reduce volume based on distance from the sound source to the portal entry point
                float volumeToEnterPortal =
                    (float) soundEnterPortalPoint.distanceTo(soundPos) / VOLUME_RADIUS_MULT;
                float volumeAtPortal = Math.max(0, soundVol - volumeToEnterPortal);
                
                return new PositionedSoundInstance(
                    sound,
                    category,
                    volumeAtPortal,
                    soundPitch,
                    soundExitPortalPoint.getX(),
                    soundExitPortalPoint.getY(),
                    soundExitPortalPoint.getZ()
                );
            }
        ).orElse(null);
        
        soundWorld.getProfiler().pop();
        
        return result;
    }
    
    private static boolean isPlayerInRange(
        Portal portal, Vec3d soundPos, double soundRadius, Vec3d playerCameraPos
    ) {
        Vec3d soundExitPoint = portal.transformPoint(portal.getNearestPointInPortal(soundPos));
        return soundExitPoint.isInRange(playerCameraPos, soundRadius);
    }
    
    private static double getPortalDistance(Portal portal, Vec3d soundPos) {
        Vec3d soundEnterPortalPoint = portal.getNearestPointInPortal(soundPos);
        return soundEnterPortalPoint.squaredDistanceTo(soundPos);
    }
}
