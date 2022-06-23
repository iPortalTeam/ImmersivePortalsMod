package qouteall.imm_ptl.core.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.entity_sync.MixinServerGamePacketListenerImpl_E;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;

import javax.annotation.Nullable;

public class IPCommonNetwork {
    
    private static final ThreadLocal<ResourceKey<Level>> tlForceRedirect =
        ThreadLocal.withInitial(() -> null);
    
    @Nullable
    private static ResourceKey<Level> forceRedirect = null;
    
    public static void withForceRedirect(ServerLevel world, Runnable func) {
        Validate.isTrue(
            ((IEWorld) world).portal_getThread() == Thread.currentThread(),
            "Maybe a mod is trying to add entity in a non-server thread. This is probably not IP's issue"
        );
        
        if (IPGlobal.useThreadLocalServerPacketRedirect) {
            ResourceKey<Level> oldForceRedirect = tlForceRedirect.get();
            tlForceRedirect.set(world.dimension());
            try {
                func.run();
            }
            finally {
                tlForceRedirect.set(oldForceRedirect);
            }
        }
        else {
            ResourceKey<Level> oldForceRedirect = forceRedirect;
            forceRedirect = world.dimension();
            try {
                func.run();
            }
            finally {
                forceRedirect = oldForceRedirect;
            }
        }
    }
    
    /**
     * If it's not null, all sent packets will be wrapped into redirected packet
     * {@link MixinServerGamePacketListenerImpl_E}
     */
    @Nullable
    public static ResourceKey<Level> getForceRedirectDimension() {
        if (IPGlobal.useThreadLocalServerPacketRedirect) {
            return tlForceRedirect.get();
        }
        else {
            return forceRedirect;
        }
    }
    
    // avoid duplicate redirect nesting
    public static void sendRedirectedPacket(
        ServerGamePacketListenerImpl serverPlayNetworkHandler,
        Packet<ClientGamePacketListener> packet,
        ResourceKey<Level> dimension
    ) {
        if (getForceRedirectDimension() == dimension) {
            serverPlayNetworkHandler.send(packet);
        }
        else {
            serverPlayNetworkHandler.send(
                IPNetworking.createRedirectedMessage(
                    dimension,
                    packet
                )
            );
        }
    }
    
    public static void validateForceRedirecting() {
        Validate.isTrue(getForceRedirectDimension() != null);
    }
    
    // avoid crash in dedicated server
    public static void do_handleRedirectedPacketFromNetworkingThread(
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet,
        ClientGamePacketListener handler
    ) {
        IPCommonNetworkClient.handleRedirectedPacketFromNetworkingThread(dimension, packet, handler);
    }
    
}
