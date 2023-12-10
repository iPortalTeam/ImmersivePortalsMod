package qouteall.q_misc_util.ducks;

import net.minecraft.world.level.storage.LevelStorageSource;
import qouteall.q_misc_util.dimension.DimIntIdMap;

public interface IEMinecraftServer_Misc {
    
    LevelStorageSource.LevelStorageAccess ip_getStorageSource();
    
    void ip_setDimIdRec(DimIntIdMap record);
    
    DimIntIdMap ip_getDimIdRec();
}
