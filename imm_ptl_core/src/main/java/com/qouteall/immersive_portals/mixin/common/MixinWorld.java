package com.qouteall.immersive_portals.mixin.common;

import com.qouteall.immersive_portals.ducks.IEWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class MixinWorld implements IEWorld {
    
    @Shadow
    @Final
    protected MutableWorldProperties properties;
    
    @Shadow
    public abstract RegistryKey<World> getRegistryKey();
    
    @Shadow
    protected float rainGradient;
    
    @Shadow
    protected float thunderGradient;
    
    @Shadow
    protected float rainGradientPrev;
    
    @Shadow
    protected float thunderGradientPrev;
    
    // Fix overworld rain cause nether fog change
    @Inject(method = "initWeatherGradients", at = @At("TAIL"))
    private void onInitWeatherGradients(CallbackInfo ci) {
        if (getRegistryKey() == World.NETHER) {
            rainGradient = 0;
            rainGradientPrev = 0;
            thunderGradient = 0;
            thunderGradientPrev = 0;
        }
    }
    
    @Override
    public MutableWorldProperties myGetProperties() {
        return properties;
    }
}
