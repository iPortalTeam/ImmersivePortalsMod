package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelInfo.class)
public class MixinLevelInfo implements IELevelProperties {
    
    AltiusInfo altiusInfo;
    
    @Inject(
        method = "method_28382",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetGamemode(GameMode gameMode, CallbackInfoReturnable<LevelInfo> cir) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Inject(
        method = "method_28381",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetDifficulty(Difficulty difficulty, CallbackInfoReturnable<LevelInfo> cir) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Inject(
        method = "method_29557",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onSetDatapack(
        DataPackSettings dataPackSettings,
        CallbackInfoReturnable<LevelInfo> cir
    ) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Inject(
        method = "method_28385",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onCopy(CallbackInfoReturnable<LevelInfo> cir) {
        ((MixinLevelInfo) (Object) cir.getReturnValue()).altiusInfo = altiusInfo;
    }
    
    @Override
    public AltiusInfo getAltiusInfo() {
        return altiusInfo;
    }
    
    @Override
    public void setAltiusInfo(AltiusInfo cond) {
        altiusInfo = cond;
    }
}
