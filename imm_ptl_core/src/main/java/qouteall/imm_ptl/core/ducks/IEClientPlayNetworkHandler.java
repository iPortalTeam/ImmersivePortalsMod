package qouteall.imm_ptl.core.ducks;

import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Map;

public interface IEClientPlayNetworkHandler {
    void ip_setWorld(ClientLevel world);
    
    Map getPlayerListEntries();
    
    void setPlayerListEntries(Map value);
}
