package qouteall.imm_ptl.core.network;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.entity_sync.MixinServerGamePacketListenerImpl_Redirect;
import qouteall.q_misc_util.dimension.DimensionIdRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketRedirection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketRedirection.class);
    
    // most game packets sent are redirected, so that payload id will be used very frequently
    // use a short id to reduce packet size
    public static final ResourceLocation payloadId =
        new ResourceLocation("i:r");
    
    private static final ThreadLocal<ResourceKey<Level>> serverPacketRedirection =
        ThreadLocal.withInitial(() -> null);
    
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
     * {@link MixinServerGamePacketListenerImpl_Redirect}
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
                    serverPlayNetworkHandler.player.server,
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
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Packet<ClientGamePacketListener> createRedirectedMessage(
        MinecraftServer server,
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet
    ) {
        Validate.isTrue(!(packet instanceof BundleDelimiterPacket));
        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            // vanilla has special handling to bundle packet
            // don't wrap a bundle packet into a normal packet
            List<Packet<ClientGamePacketListener>> newSubPackets = new ArrayList<>();
            for (var subPacket : bundlePacket.subPackets()) {
                newSubPackets.add(createRedirectedMessage(server, dimension, subPacket));
            }
            
            return new ClientboundBundlePacket(newSubPackets);
        }
        else {
            // will use the server argument in the future
            int intDimId = DimensionIdRecord.serverRecord.getIntId(dimension);
            Payload payload = new Payload(intDimId, packet);
            
            // the custom payload packet should be able to be bundled
            // the bundle accepts Packet<ClientGamePacketListener>
            // but the custom payload packet is Packet<ClientCommonPacketListener>
            // the generic parameter is contravariant (it's used as argument),
            // which means changing it to subtype is fine
            
            return (Packet<ClientGamePacketListener>) (Packet)
                new ClientboundCustomPayloadPacket(payload);
        }
    }
    
    public static void sendRedirectedMessage(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet
    ) {
        player.connection.send(createRedirectedMessage(player.server, dimension, packet));
    }
    
    private static final ConnectionProtocol.CodecData<?> clientPlayCodecData =
        ConnectionProtocol.PLAY.codec(PacketFlow.CLIENTBOUND);
    
    public static int getPacketId(Packet<?> packet) {
        try {
            return clientPlayCodecData.packetId(packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static Packet<?> readPacketById(int messageType, FriendlyByteBuf buf) {
        return clientPlayCodecData.createPacket(messageType, buf);
    }
    
    /**
     * @param dimensionIntId use integer here because the mapping between dimension id and integer id is per-server the deserialization context does not give access to MinecraftServer object (going to handle the case of multiple servers per JVM)
     */
    public record Payload(
        int dimensionIntId, Packet<? extends ClientCommonPacketListener> packet
    ) implements CustomPacketPayload {
        
        @Override
        public void write(FriendlyByteBuf buf) {
            Validate.notNull(packet, "packet is null");
            
            buf.writeVarInt(dimensionIntId);
            
            int packetId = getPacketId(packet);
            buf.writeVarInt(packetId);
            
            packet.write(buf);
        }
        
        @SuppressWarnings("unchecked")
        public static Payload read(FriendlyByteBuf buf) {
            int dimensionIntId = buf.readVarInt();
            
            int packetId = buf.readVarInt();
            Packet<ClientGamePacketListener> packet =
                (Packet<ClientGamePacketListener>) readPacketById(packetId, buf);
            
            return new Payload(dimensionIntId, packet);
        }
        
        @Override
        public @NotNull ResourceLocation id() {
            return payloadId;
        }
        
        @SuppressWarnings("unchecked")
        public void handle(ClientGamePacketListener listener) {
            ResourceKey<Level> dim = DimensionIdRecord.clientRecord.getDim(dimensionIntId);
            PacketRedirectionClient.handleRedirectedPacket(
                dim, (Packet) packet, listener
            );
        }
    }
}
