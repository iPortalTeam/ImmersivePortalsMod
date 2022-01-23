package qouteall.imm_ptl.core.ducks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.WritableLevelData;

public interface IEWorld {
    
    WritableLevelData myGetProperties();
    
    void portal_setWeather(float rainGradPrev, float rainGrad, float thunderGradPrev, float thunderGrad);
    
    LevelEntityGetter<Entity> portal_getEntityLookup();
    
    Thread portal_getThread();
}
