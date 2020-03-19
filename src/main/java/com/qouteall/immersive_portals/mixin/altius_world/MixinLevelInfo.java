package com.qouteall.immersive_portals.mixin.altius_world;

import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelInfo.class)
public class MixinLevelInfo implements IELevelProperties {
    
    AltiusInfo altiusInfo;
    
    @Override
    public AltiusInfo getAltiusInfo() {
        return altiusInfo;
    }
    
    @Override
    public void setAltiusInfo(AltiusInfo cond) {
        altiusInfo = cond;
    }
}
