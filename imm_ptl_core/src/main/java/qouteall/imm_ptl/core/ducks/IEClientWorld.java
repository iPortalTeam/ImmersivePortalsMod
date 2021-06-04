package qouteall.imm_ptl.core.ducks;

import qouteall.imm_ptl.core.portal.Portal;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import javax.annotation.Nullable;
import java.util.List;

public interface IEClientWorld {
    ClientPlayNetworkHandler getNetHandler();
    
    void setNetHandler(ClientPlayNetworkHandler handler);
    
    @Nullable
    List<Portal> getGlobalPortals();
    
    void setGlobalPortals(List<Portal> arg);
}
