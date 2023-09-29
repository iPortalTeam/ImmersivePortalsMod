package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerCommonPacketListenerImpl.class)
public interface IEServerCommonPacketListenerImpl {
    @Accessor("connection")
    Connection ip_getConnection();
}
