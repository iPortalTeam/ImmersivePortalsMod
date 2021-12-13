package qouteall.imm_ptl.core.network;

import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.entity_sync.MixinServerPlayNetworkHandler_E;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;

import javax.annotation.Nullable;

public class IPCommonNetwork {
    
    @Nullable
    private static RegistryKey<World> forceRedirect = null;
    
    public static void withForceRedirect(ServerWorld world, Runnable func) {
        Validate.isTrue(
            ((IEWorld) world).portal_getThread() == Thread.currentThread(),
            "Maybe a mod is trying to add entity in a non-server thread. This is probably not IP's issue"
        );
        
        RegistryKey<World> oldForceRedirect = forceRedirect;
        forceRedirect = world.getRegistryKey();
        try {
            func.run();
        }
        finally {
            forceRedirect = oldForceRedirect;
        }
    }
    
    /**
     * If it's not null, all sent packets will be wrapped into redirected packet
     * {@link MixinServerPlayNetworkHandler_E}
     */
    @Nullable
    public static RegistryKey<World> getForceRedirectDimension() {
        return forceRedirect;
    }
    
    // avoid duplicate redirect nesting
    public static void sendRedirectedPacket(
        ServerPlayNetworkHandler serverPlayNetworkHandler,
        Packet<?> packet,
        RegistryKey<World> dimension
    ) {
        if (getForceRedirectDimension() == dimension) {
            serverPlayNetworkHandler.sendPacket(packet);
        }
        else {
            serverPlayNetworkHandler.sendPacket(
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
}
