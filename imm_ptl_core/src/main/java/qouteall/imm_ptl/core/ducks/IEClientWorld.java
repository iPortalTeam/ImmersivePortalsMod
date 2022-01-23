package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.entity.EntityTickList;
import qouteall.imm_ptl.core.portal.Portal;

import javax.annotation.Nullable;
import java.util.List;

public interface IEClientWorld {
    ClientPacketListener getNetHandler();
    
    void setNetHandler(ClientPacketListener handler);
    
    @Nullable
    List<Portal> getGlobalPortals();
    
    void setGlobalPortals(List<Portal> arg);
    
    void resetWorldRendererRef();
    
    EntityTickList ip_getEntityList();
}
