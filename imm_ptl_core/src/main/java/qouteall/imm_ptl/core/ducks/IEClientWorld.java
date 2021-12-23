package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.world.EntityList;
import qouteall.imm_ptl.core.portal.Portal;

import javax.annotation.Nullable;
import java.util.List;

public interface IEClientWorld {
    ClientPlayNetworkHandler getNetHandler();
    
    void setNetHandler(ClientPlayNetworkHandler handler);
    
    @Nullable
    List<Portal> getGlobalPortals();
    
    void setGlobalPortals(List<Portal> arg);
    
    void resetWorldRendererRef();
    
    EntityList ip_getEntityList();
}
