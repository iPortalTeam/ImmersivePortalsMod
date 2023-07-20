package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import qouteall.imm_ptl.core.portal.Portal;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Map;

public interface IEClientWorld {
    ClientPacketListener getNetHandler();
    
    void setNetHandler(ClientPacketListener handler);
    
    @Nullable
    List<Portal> getGlobalPortals();
    
    void setGlobalPortals(List<Portal> arg);
    
    void resetWorldRendererRef();
    
    EntityTickList ip_getEntityList();
    
    Map<String, MapItemSavedData> ip_getAllMapData();
    
    void ip_addMapData(Map<String, MapItemSavedData> map);
    
    BlockStatePredictionHandler ip_getBlockStatePredictionHandler();
}
