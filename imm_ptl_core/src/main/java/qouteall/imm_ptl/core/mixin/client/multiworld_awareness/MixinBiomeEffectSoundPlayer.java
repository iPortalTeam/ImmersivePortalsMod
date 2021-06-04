package qouteall.imm_ptl.core.mixin.client.multiworld_awareness;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.BiomeEffectSoundPlayer;
import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeEffectSoundPlayer.class)
public class MixinBiomeEffectSoundPlayer {
    @Mutable
    @Shadow
    @Final
    private BiomeAccess biomeAccess;
    
    @Shadow
    @Final
    private ClientPlayerEntity player;
    
    // change the biomeAccess field when player dimension changes
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        biomeAccess = player.world.getBiomeAccess();
    }
}
