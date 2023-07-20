package qouteall.imm_ptl.core.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.ducks.IECustomPayloadPacket;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.entity_sync.MixinServerGamePacketListenerImpl_E;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketRedirection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketRedirection.class);
    
    public static final ResourceLocation id_stcRedirected =
        new ResourceLocation("imm_ptl", "rd");
    
    public static boolean isPacketIdOfRedirection(ResourceLocation packetTypeId) {
        return packetTypeId.getNamespace().equals("imm_ptl") && packetTypeId.getPath().equals("rd");
    }
    
    private static final ThreadLocal<ResourceKey<Level>> serverPacketRedirection =
        ThreadLocal.withInitial(() -> null);
    
    // Mixin does not allow cancelling in constructor
    // so use a dummy argument instead of null
    private static final FriendlyByteBuf dummyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
    
    public static void withForceRedirect(ServerLevel world, Runnable func) {
        withForceRedirectAndGet(world, () -> {
            func.run();
            return null;
        });
    }
    
    public static <T> T withForceRedirectAndGet(ServerLevel world, Supplier<T> func) {
        if (((IEWorld) world).portal_getThread() != Thread.currentThread()) {
            LOGGER.error(
                "It's possible that a mod is trying to handle packet in networking thread instead of server thread. This is not thread safe and can cause rare bugs! (ImmPtl is just doing checking, it's not an issue of ImmPtl)",
                new Throwable()
            );
        }
        
        ResourceKey<Level> oldRedirection = serverPacketRedirection.get();
        serverPacketRedirection.set(world.dimension());
        try {
            return func.get();
        }
        finally {
            serverPacketRedirection.set(oldRedirection);
        }
    }
    
    /**
     * If it's not null, all sent packets will be wrapped into redirected packet
     * {@link MixinServerGamePacketListenerImpl_E}
     */
    @Nullable
    public static ResourceKey<Level> getForceRedirectDimension() {
        return serverPacketRedirection.get();
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
                createRedirectedMessage(
                    dimension,
                    packet
                )
            );
        }
    }
    
    public static void validateForceRedirecting() {
        Validate.isTrue(getForceRedirectDimension() != null);
    }
    
    /**
     * This can be called both in networking thread (for normal packets) or in render thread (for bundle packet).
     * avoid ClassNotFound in dedicated server
     */
    public static void do_handleRedirectedPacket(
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet,
        ClientGamePacketListener handler
    ) {
        PacketRedirectionClient.handleRedirectedPacket(dimension, packet, handler);
    }
    
    public static Packet<ClientGamePacketListener> createRedirectedMessage(
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet
    ) {
        Validate.isTrue(!(packet instanceof BundleDelimiterPacket<ClientGamePacketListener>));
        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            // vanilla has special handling to bundle packet
            // don't wrap a bundle packet into a normal packet
            List<Packet<ClientGamePacketListener>> newSubPackets = new ArrayList<>();
            for (Packet<ClientGamePacketListener> subPacket : bundlePacket.subPackets()) {
                newSubPackets.add(createRedirectedMessage(dimension, subPacket));
            }
            return new ClientboundBundlePacket(newSubPackets);
        }
        else {
            ClientboundCustomPayloadPacket result =
                new ClientboundCustomPayloadPacket(id_stcRedirected, dummyByteBuf);
            
            ((IECustomPayloadPacket) result).ip_setRedirectedDimension(dimension);
            ((IECustomPayloadPacket) result).ip_setRedirectedPacket(packet);
            
            return result;
        }
    }
    
    public static void sendRedirectedMessage(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        Packet packet
    ) {
        player.connection.send(createRedirectedMessage(dimension, packet));
    }
    
    public static int getPacketId(Packet packet) {
        try {
            return ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static Packet createPacketById(
        int messageType, FriendlyByteBuf buf
    ) {
        return ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, messageType, buf);
    }
}
