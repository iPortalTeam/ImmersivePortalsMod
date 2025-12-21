package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.ducks.IEWorld;
import qouteall.imm_ptl.core.mixin.common.entity_sync.MixinServerGamePacketListenerImpl_Redirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PacketRedirection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketRedirection.class);
    
    // most game packets sent are redirected, so that payload id will be used very frequently
    // use a short id to reduce packet size
    public static final ResourceLocation payloadId =
        McHelper.newIdentifier("i:r");
    
    private static final ThreadLocal<ResourceKey<Level>> serverPacketRedirection =
        ThreadLocal.withInitial(() -> null);
    
    public static interface ForceBundleCallback {
        void accept(
            ServerCommonPacketListenerImpl listener,
            Packet<ClientGamePacketListener> packet
        );
    }
    
    private static final ThreadLocal<ForceBundleCallback> forceBundle =
        ThreadLocal.withInitial(() -> null);
    
    public static void init() {
        PayloadTypeRegistry.playS2C().register(Payload.TYPE, Payload.CODEC);
    }
    
    public static void withForceRedirect(ServerLevel world, Runnable func) {
        withForceRedirectAndGet(world, () -> {
            func.run();
            return null;
        });
    }
    
    @SuppressWarnings("UnusedReturnValue")
    public static <T> T withForceRedirectAndGet(ServerLevel world, Supplier<T> func) {
        if (((IEWorld) world).portal_getThread() != Thread.currentThread()) {
            LOGGER.error(
                "It's possible that a mod is trying to handle packet in networking thread instead of server thread. This is not thread safe and can cause rare bugs! (ImmPtl is just doing checking, it's not an issue of iPortal)",
                new Throwable()
            );
        }
        
        ResourceKey<Level> redirectDim = world.dimension();
        
        ResourceKey<Level> oldRedirection = serverPacketRedirection.get();
        
        if (oldRedirection != redirectDim) {
            serverPacketRedirection.set(redirectDim);
        }
        
        try {
            return func.get();
        }
        finally {
            if (oldRedirection != redirectDim) {
                serverPacketRedirection.set(oldRedirection);
            }
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
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Packet<ClientGamePacketListener> createRedirectedMessage(
        MinecraftServer server,
        ResourceKey<Level> dimension,
        Packet<ClientGamePacketListener> packet
    ) {
        if (isRedirectPacket(packet)) {
            // avoid duplicate redirect nesting
            return packet;
        }
        
        Validate.isTrue(!(packet instanceof BundleDelimiterPacket));
        if (packet instanceof ClientboundBundlePacket bundlePacket) {
            // vanilla has special handling to bundle packet
            // don't wrap a bundle packet into a normal packet
            List<Packet<ClientGamePacketListener>> newSubPackets = new ArrayList<>();
            for (var subPacket : bundlePacket.subPackets()) {
                newSubPackets.add(createRedirectedMessage(
                    server, dimension, (Packet<ClientGamePacketListener>) subPacket
                ));
            }
            
            return new ClientboundBundlePacket(
                (List<Packet<? super ClientGamePacketListener>>) (List) newSubPackets
            );
        }
        else {
            // will use the server argument in the future
            int intDimId = PortalAPI.serverDimKeyToInt(server, dimension);
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
    
    // Note this doesn't consider bundle packet
    public static boolean isRedirectPacket(Packet<?> packet) {
        return packet instanceof ClientboundCustomPayloadPacket customPayloadPacket &&
            customPayloadPacket.payload() instanceof Payload;
    }
    
    @SuppressWarnings({"unchecked", "ThreadLocalSetWithNull", "rawtypes"})
    public static <R> R withForceBundle(Supplier<R> func) {
        ForceBundleCallback forceBundleCallback = getForceBundleCallback();
        if (forceBundleCallback != null) {
            // already in force-bundle mode. directly invoke the function
            return func.get();
        }
        
        Map<ServerCommonPacketListenerImpl, List<Packet<ClientGamePacketListener>>>
            map = new HashMap<>();
        forceBundle.set((listener, packet) -> {
            List<Packet<ClientGamePacketListener>> packetsToBundle =
                map.computeIfAbsent(listener, k -> new ArrayList<>());
            if (packet instanceof BundlePacket<?> bundlePacket) {
                Iterable<? extends Packet<?>> subPackets = bundlePacket.subPackets();
                for (Packet<?> subPacket : subPackets) {
                    packetsToBundle.add((Packet<ClientGamePacketListener>) subPacket);
                }
            }
            else {
                packetsToBundle.add(packet);
            }
        });
        
        try {
            return func.get();
        }
        finally {
            forceBundle.set(null);
            for (var e : map.entrySet()) {
                ServerCommonPacketListenerImpl listener = e.getKey();
                List<Packet<ClientGamePacketListener>> packets = e.getValue();
                listener.send(new ClientboundBundlePacket(
                    (List<Packet<? super ClientGamePacketListener>>) (List) packets
                ));
            }
        }
    }
    
    public static @Nullable ForceBundleCallback getForceBundleCallback() {
        return forceBundle.get();
    }
    
    // Mojang's new networking abstraction made packet redirection more convoluted...
    private static final ProtocolInfo<ClientGamePacketListener> PLACEHOLDER_PROTOCOL_INFO =
        // the function passed into bind is used for converting ByteBuf to RegistryFriendlyByteBuf
        // it's a pre-processor
        // that ProtocolInfo will be used by passing RegistryFriendlyByteBuf
        // so only a casting is needed
        GameProtocols.CLIENTBOUND_TEMPLATE.bind(
            argBuf -> ((RegistryFriendlyByteBuf) argBuf)
        );
    
    /**
     * @param dimensionIntId use integer here because the mapping between dimension id and integer id is per-server the deserialization context does not give access to MinecraftServer object (going to handle the case of multiple servers per JVM)
     */
    public record Payload(
        int dimensionIntId, Packet<? extends ClientGamePacketListener> packet
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Payload> TYPE =
            new CustomPacketPayload.Type<>(payloadId);
        
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC =
            StreamCodec.of(
                (b, p) -> p.write(b), Payload::read
            );
        
        @SuppressWarnings("unchecked")
        public void write(RegistryFriendlyByteBuf buf) {
            Validate.notNull(packet, "packet is null");
            
            buf.writeVarInt(dimensionIntId);
            
            PLACEHOLDER_PROTOCOL_INFO.codec()
                .encode(buf, (Packet<? super ClientGamePacketListener>) packet);
        }
        
        @SuppressWarnings("unchecked")
        public static Payload read(FriendlyByteBuf buf) {
            int dimensionIntId = buf.readVarInt();
            
            var packet = (Packet<ClientGamePacketListener>)
                PLACEHOLDER_PROTOCOL_INFO.codec().decode(buf);
            
            return new Payload(dimensionIntId, packet);
        }
        
        @SuppressWarnings({"unchecked", "rawtypes"})
        @Environment(EnvType.CLIENT)
        public void handle(ClientGamePacketListener listener) {
            PacketRedirectionClient.handleRedirectedPacket(
                dimensionIntId, (Packet) packet, listener
            );
        }
        
        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
