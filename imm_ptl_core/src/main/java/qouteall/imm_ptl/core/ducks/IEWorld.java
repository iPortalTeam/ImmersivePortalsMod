package qouteall.imm_ptl.core.ducks;

import net.minecraft.entity.Entity;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.entity.EntityLookup;

public interface IEWorld {
    
    MutableWorldProperties myGetProperties();
    
    void portal_setWeather(float rainGradPrev, float rainGrad, float thunderGradPrev, float thunderGrad);
    
    EntityLookup<Entity> portal_getEntityLookup();
    
    Thread portal_getThread();
}
