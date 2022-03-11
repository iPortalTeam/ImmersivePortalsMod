package qouteall.imm_ptl.core.ducks;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface IEEntityTracker {
    Entity getEntity_();
    
    void updateEntityTrackingStatus(ServerPlayer player);
    
    void onPlayerRespawn(ServerPlayer oldPlayer);
    
    void ip_onDimensionRemove();
    
    void resendSpawnPacketToTrackers();
    
    void stopTrackingToAllPlayers_();
    
    void tickEntry();
    
    SectionPos getLastCameraPosition();
    
    void setLastCameraPosition(SectionPos arg);
}
