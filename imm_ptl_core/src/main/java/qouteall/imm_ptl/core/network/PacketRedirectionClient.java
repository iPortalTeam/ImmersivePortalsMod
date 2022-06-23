package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.mixin.client.sync.MixinMinecraft_RedirectedPacket;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class PacketRedirectionClient {
    
    public static final Minecraft client = Minecraft.getInstance();
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    /**
     * This ensures that when it calls client.execute(...)
     * the task will be executed with redirected dimension.
     * {@link MixinMinecraft_RedirectedPacket}
     * This is also used in networking threads.
     */
    public static final ThreadLocal<ResourceKey<Level>> clientTaskRedirection =
        ThreadLocal.withInitial(() -> null);
    
    public static boolean getIsProcessingRedirectedMessage() {
        return clientTaskRedirection.get() != null;
    }
    
    /**
     * For vanilla packets, in {@link PacketUtils#ensureRunningOnSameThread(Packet, PacketListener, BlockableEventLoop)}
     * it will resubmit the task
     * and the task will be redirected in {@link MixinMinecraft_RedirectedPacket}
     *
     * For mod packets ({@link ClientboundCustomPayloadPacket}),
     * handled in {@link net.fabricmc.fabric.mixin.networking.client.ClientPlayNetworkHandlerMixin},
     * the mod will also handle the packet using {@link Minecraft#execute(Runnable)} (If not, that mod has the bug)
     */
    public static void handleRedirectedPacketFromNetworkingThread(
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet,
        ClientGamePacketListener handler
    ) {
        ResourceKey<Level> oldTaskRedirection = clientTaskRedirection.get();
        clientTaskRedirection.set(dimension);
    
        try {
            packet.handle(handler);
        }
        finally {
            clientTaskRedirection.set(oldTaskRedirection);
        }
    }
}
