package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEWorld;

@Mixin(Level.class)
public abstract class MixinWorld implements IEWorld {
    
    @Shadow
    @Final
    protected WritableLevelData levelData;
    
    @Shadow
    public abstract ResourceKey<Level> dimension();
    
    @Shadow
    protected float rainLevel;
    
    @Shadow
    protected float thunderLevel;
    
    @Shadow
    protected float oRainLevel;
    
    @Shadow
    protected float oThunderLevel;
    
    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();
    
    @Shadow
    @Final
    private Thread thread;
    
    // Fix overworld rain cause nether fog change
    @Inject(method = "Lnet/minecraft/world/level/Level;prepareWeather()V", at = @At("TAIL"))
    private void onInitWeatherGradients(CallbackInfo ci) {
        if (dimension() == Level.NETHER) {
            rainLevel = 0;
            oRainLevel = 0;
            thunderLevel = 0;
            oThunderLevel = 0;
        }
    }
    
    @Override
    public WritableLevelData myGetProperties() {
        return levelData;
    }
    
    @Override
    public void portal_setWeather(float rainGradPrev, float rainGrad, float thunderGradPrev, float thunderGrad) {
        oRainLevel = rainGradPrev;
        rainLevel = rainGrad;
        oThunderLevel = thunderGradPrev;
        thunderLevel = thunderGrad;
    }
    
    @Override
    public LevelEntityGetter<Entity> portal_getEntityLookup() {
        return getEntities();
    }
    
    @Override
    public Thread portal_getThread() {
        return thread;
    }
}
