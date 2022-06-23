package qouteall.imm_ptl.core.ducks;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IECustomPayloadPacket {
    void ip_setRedirectedDimension(ResourceKey<Level> dimension);
    
    void ip_setRedirectedPacket(Packet<ClientGamePacketListener> packet);
    
    ResourceKey<Level> ip_getRedirectedDimension();
    
    Packet<ClientGamePacketListener> ip_getRedirectedPacket();
}
