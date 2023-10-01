package qouteall.imm_ptl.core.ducks;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface IEEntityTracker {
    Entity ip_getEntity();
    
    void ip_updateEntityTrackingStatus(ServerPlayer player);
    
    void ip_onDimensionRemove();
    
    void ip_resendSpawnPacketToTrackers();
    
    void ip_stopTrackingToAllPlayers();
    
    void ip_tickEntry();
    
    SectionPos ip_getLastCameraPosition();
    
    void ip_setLastCameraPosition(SectionPos arg);
    
}
