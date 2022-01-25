package qouteall.imm_ptl.core.mixin.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.teleportation.CrossPortalSound;

@Mixin(ClientLevel.class)
public class MixinClientWorld_Sound {
    
    @Shadow
    @Final
    private Minecraft minecraft;
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySound(
        double x, double y, double z,
        SoundEvent sound, SoundSource category, float volume, float pitch, boolean repeat,
        CallbackInfo ci
    ) {
        ClientLevel this_ = (ClientLevel) (Object) this;
        Vec3 soundPos = new Vec3(x, y, z);
        
        if (!portal_isPosNearPlayer(soundPos)) {
            SimpleSoundInstance crossPortalSound = CrossPortalSound.createCrossPortalSound(
                this_, sound, category, soundPos, volume, pitch
            );
            if (crossPortalSound != null) {
                portal_playSound(crossPortalSound, repeat);
                ci.cancel();
            }
            else if (!CrossPortalSound.isPlayerWorld(this_)) {
                // do not play remote sound when no portal can transfer the sound
                ci.cancel();
            }
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/client/multiplayer/ClientLevel;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onPlaySoundFromEntity(
        Player player, Entity entity,
        SoundEvent sound, SoundSource category, float volume, float pitch, CallbackInfo ci
    ) {
        ClientLevel this_ = (ClientLevel) (Object) this;
        
        if (!portal_isPosNearPlayer(entity.position())) {
            SimpleSoundInstance crossPortalSound = CrossPortalSound.createCrossPortalSound(
                this_, sound, category, entity.position(), volume, pitch
            );
            
            if (crossPortalSound != null) {
                minecraft.getSoundManager().play(crossPortalSound);
                ci.cancel();
            }
            else if (!CrossPortalSound.isPlayerWorld(this_)) {
                // do not play remote sound when no portal can transfer the sound
                ci.cancel();
            }
        }
    }
    
    private void portal_playSound(SimpleSoundInstance sound, boolean repeat) {
        double d = minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(sound.getX(), sound.getY(), sound.getZ());
        if (repeat && d > 100.0D) {
            double e = Math.sqrt(d) / 40.0D;
            minecraft.getSoundManager().playDelayed(sound, (int) (e * 20.0D));
        }
        else {
            minecraft.getSoundManager().play(sound);
        }
    }
    
    private boolean portal_isPosNearPlayer(Vec3 pos) {
        ClientLevel this_ = (ClientLevel) (Object) this;
        
        LocalPlayer player = minecraft.player;
        
        if (player == null) {
            return false;
        }
        
        if (this_ != player.level) {
            return false;
        }
        
        return pos.distanceToSqr(player.position()) < 64 * 64;
    }
    
}
