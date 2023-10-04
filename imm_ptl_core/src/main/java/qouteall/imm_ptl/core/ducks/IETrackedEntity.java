package qouteall.imm_ptl.core.ducks;

import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;

public interface IETrackedEntity {
    Entity ip_getEntity();
    
    void ip_updateEntityTrackingStatus();
    
    void ip_onDimensionRemove();
    
    void ip_resendSpawnPacketToTrackers();
    
    void ip_stopTrackingToAllPlayers();
    
    void ip_tickEntry();
    
    SectionPos ip_getLastSectionPos();
    
    void ip_setLastSectionPos(SectionPos arg);
    
}
