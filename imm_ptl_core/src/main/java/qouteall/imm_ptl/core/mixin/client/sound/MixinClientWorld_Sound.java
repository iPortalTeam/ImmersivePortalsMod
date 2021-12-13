package qouteall.imm_ptl.core.mixin.client.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import qouteall.imm_ptl.core.teleportation.CrossPortalSound;

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
        SoundEvent sound, SoundCategory category, float volume, float pitch, boolean repeat,
        CallbackInfo ci
    ) {
        ClientWorld this_ = (ClientWorld) (Object) this;
        Vec3d soundPos = new Vec3d(x, y, z);
        
        if (!portal_isPosNearPlayer(soundPos)) {
            PositionedSoundInstance crossPortalSound = CrossPortalSound.createCrossPortalSound(
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
            PositionedSoundInstance crossPortalSound = CrossPortalSound.createCrossPortalSound(
                this_, sound, category, entity.getPos(), volume, pitch
            );
            
            if (crossPortalSound != null) {
                client.getSoundManager().play(crossPortalSound);
                ci.cancel();
            }
            else if (!CrossPortalSound.isPlayerWorld(this_)) {
                // do not play remote sound when no portal can transfer the sound
                ci.cancel();
            }
        }
    }
    
    private void portal_playSound(PositionedSoundInstance sound, boolean repeat) {
        double d = client.gameRenderer.getCamera().getPos().squaredDistanceTo(sound.getX(), sound.getY(), sound.getZ());
        if (repeat && d > 100.0D) {
            double e = Math.sqrt(d) / 40.0D;
            client.getSoundManager().play(sound, (int) (e * 20.0D));
        }
        else {
            client.getSoundManager().play(sound);
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
