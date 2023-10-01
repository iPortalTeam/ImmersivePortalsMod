package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.List;
import java.util.Map;

public interface IEClientWorld {
    
    @Nullable
    List<Portal> ip_getGlobalPortals();
    
    void ip_setGlobalPortals(List<Portal> arg);
    
    void ip_resetWorldRendererRef();
    
    EntityTickList ip_getEntityList();
    
    Map<String, MapItemSavedData> ip_getAllMapData();
    
    void ip_addMapData(Map<String, MapItemSavedData> map);
    
    BlockStatePredictionHandler ip_getBlockStatePredictionHandler();
}
