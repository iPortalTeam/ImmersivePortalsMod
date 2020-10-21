package com.qouteall.immersive_portals.ducks;

import net.minecraft.world.MutableWorldProperties;

public interface IEWorld {
    
    MutableWorldProperties myGetProperties();
    
    void portal_setWeather(float rainGradPrev, float rainGrad, float thunderGradPrev, float thunderGrad);
}
