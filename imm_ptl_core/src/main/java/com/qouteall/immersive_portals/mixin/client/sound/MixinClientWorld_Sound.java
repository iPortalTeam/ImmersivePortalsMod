package com.qouteall.immersive_portals.mixin.client.sound;

import com.qouteall.immersive_portals.teleportation.CrossPortalSound;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class MixinClientWorld_Sound {
    
    @Shadow
    @Final
    private MinecraftClient client;
    
    @Inject(
        method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySound(
        double x, double y, double z,
        SoundEvent sound, SoundCategory category, float volume, float pitch, boolean bl,
        CallbackInfo ci
    ) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        Vec3d soundPos = new Vec3d(x, y, z);
        
        if (!portal_isPosNearPlayer(soundPos)) {
            Vec3d transformedSoundPosition =
                CrossPortalSound.getTransformedSoundPosition(this_, soundPos);
            if (transformedSoundPosition != null) {
                portal_playSound(
                    transformedSoundPosition.x, transformedSoundPosition.y, transformedSoundPosition.z,
                    sound, category, volume, pitch, bl
                );
                ci.cancel();
            }
            else {
                if (CrossPortalSound.isPlayerWorld(this_)) {
                    // play normally
                }
                else {
                    // do not play remote sound when no portal can transfer the sound
                    ci.cancel();
                }
            }
            
        }
    }
    
    @Inject(
        method = "playSoundFromEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySoundFromEntity(
        PlayerEntity player, Entity entity,
        SoundEvent sound, SoundCategory category, float volume, float pitch, CallbackInfo ci
    ) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        
        if (!portal_isPosNearPlayer(entity.getPos())) {
            Vec3d entityPos = entity.getPos();
            Vec3d transformedSoundPosition = CrossPortalSound.getTransformedSoundPosition(
                this_, entityPos
            );
            
            if (transformedSoundPosition != null) {
                entity.setPos(transformedSoundPosition.x, transformedSoundPosition.y, transformedSoundPosition.z);
                EntityTrackingSoundInstance sound1 = new EntityTrackingSoundInstance(
                    sound, category, volume, pitch, entity
                );
                client.getSoundManager().play(sound1);
                entity.setPos(entityPos.x, entityPos.y, entityPos.z);
                ci.cancel();
            }
            else {
                if (CrossPortalSound.isPlayerWorld(this_)) {
                    // play normally
                }
                else {
                    // do not play remote sound when no portal can transfer the sound
                    ci.cancel();
                }
            }
        }
    }
    
    private void portal_playSound(
        double x, double y, double z,
        SoundEvent sound, SoundCategory category, float volume, float pitch, boolean bl
    ) {
        double d = client.gameRenderer.getCamera().getPos().squaredDistanceTo(x, y, z);
        PositionedSoundInstance positionedSoundInstance = new PositionedSoundInstance(sound, category, volume, pitch, x, y, z);
        if (bl && d > 100.0D) {
            double e = Math.sqrt(d) / 40.0D;
            client.getSoundManager().play(positionedSoundInstance, (int) (e * 20.0D));
        }
        else {
            client.getSoundManager().play(positionedSoundInstance);
        }
    }
    
    private boolean portal_isPosNearPlayer(Vec3d pos) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        
        ClientPlayerEntity player = client.player;
        
        if (this_ != player.world) {
            return false;
        }
        
        return pos.squaredDistanceTo(player.getPos()) < 64 * 64;
    }
    
}
