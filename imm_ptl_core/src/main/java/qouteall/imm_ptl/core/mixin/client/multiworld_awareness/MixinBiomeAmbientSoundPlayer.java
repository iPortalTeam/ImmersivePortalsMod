package qouteall.imm_ptl.core.mixin.client.multiworld_awareness;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeAmbientSoundsHandler.class)
public class MixinBiomeAmbientSoundPlayer {
    @Mutable
    @Shadow
    @Final
    private BiomeManager biomeManager;
    
    @Shadow
    @Final
    private LocalPlayer player;
    
    // change the biomeAccess field when player dimension changes
    @Inject(method = "Lnet/minecraft/client/resources/sounds/BiomeAmbientSoundsHandler;tick()V", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        biomeManager = player.level().getBiomeManager();
    }
}
