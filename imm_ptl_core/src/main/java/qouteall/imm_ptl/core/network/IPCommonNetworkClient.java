package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
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
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
import qouteall.imm_ptl.core.mixin.client.sync.MixinMinecraft_RedirectedPacket;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Optional;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class IPCommonNetworkClient {
    
    public static final SignalArged<Portal> clientPortalSpawnSignal = new SignalArged<>();
    
    public static final Minecraft client = Minecraft.getInstance();
    private static final LimitedLogger limitedLogger = new LimitedLogger(100);
    
    /**
     * This ensures that when it calls client.execute(...)
     * the task will be executed with redirected dimension.
     * {@link MixinMinecraft_RedirectedPacket}
     * This is also used in networking threads.
     */
    public static final ThreadLocal<ResourceKey<Level>> taskRedirection =
        ThreadLocal.withInitial(() -> null);
    
    
    public static void doProcessRedirectedMessage(
        ClientLevel packetWorld,
        Packet packet
    ) {
        ResourceKey<Level> oldTaskRedirection = taskRedirection.get();
        taskRedirection.set(packetWorld.dimension());
        
        ClientPacketListener netHandler = ((IEClientWorld) packetWorld).getNetHandler();
        
        if (netHandler.getLevel() != client.level) {
            Helper.err("Net handler world state inconsistent");
        }
        
        client.getProfiler().push(() -> {
            return "handle_redirected_packet" + packetWorld.dimension() + packet;
        });
        
        try {
            withSwitchedWorld(packetWorld, () -> packet.handle(netHandler));
        }
        catch (Throwable e) {
            limitedLogger.throwException(() -> new IllegalStateException(
                "handling packet in %s %s".formatted(packetWorld.dimension(), packet.getClass()), e
            ));
        }
        finally {
            client.getProfiler().pop();
            
            taskRedirection.set(oldTaskRedirection);
        }
    }
    
    public static void withSwitchedWorld(ClientLevel newWorld, Runnable runnable) {
        withSwitchedWorld(newWorld, () -> {
            runnable.run();
            return null;
        });
    }
    
    public static <T> T withSwitchedWorld(ClientLevel newWorld, Supplier<T> supplier) {
        Validate.isTrue(client.isSameThread());
        Validate.isTrue(client.player != null);
        
        ClientPacketListener networkHandler = client.getConnection();
        
        ClientLevel originalWorld = client.level;
        LevelRenderer originalWorldRenderer = client.levelRenderer;
        ClientLevel originalNetHandlerWorld = networkHandler.getLevel();
        
        LevelRenderer newWorldRenderer = ClientWorldLoader.getWorldRenderer(newWorld.dimension());
        
        Validate.notNull(newWorldRenderer);
        
        client.level = newWorld;
        ((IEParticleManager) client.particleEngine).ip_setWorld(newWorld);
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        ((IEClientPlayNetworkHandler) networkHandler).ip_setWorld(newWorld);
        
        try {
            return supplier.get();
        }
        finally {
            if (client.level != newWorld) {
                Helper.err("Respawn packet should not be redirected");
                originalWorld = client.level;
                originalWorldRenderer = client.levelRenderer;
            }
            
            client.level = originalWorld;
            ((IEMinecraftClient) client).setWorldRenderer(originalWorldRenderer);
            ((IEParticleManager) client.particleEngine).ip_setWorld(originalWorld);
            ((IEClientPlayNetworkHandler) networkHandler).ip_setWorld(originalNetHandlerWorld);
        }
    }
    
    public static void processEntitySpawn(String entityTypeString, int entityId, ResourceKey<Level> dim, CompoundTag compoundTag) {
        Optional<EntityType<?>> entityType = EntityType.byString(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
        
        MiscHelper.executeOnRenderThread(() -> {
            client.getProfiler().push("ip_spawn_entity");
            
            ClientLevel world = ClientWorldLoader.getWorld(dim);
            
            Entity entity = entityType.get().create(
                world
            );
            entity.load(compoundTag);
            entity.setId(entityId);
            entity.syncPacketPositionCodec(entity.getX(), entity.getY(), entity.getZ());
            world.putNonPlayerEntity(entityId, entity);
            
            //do not create client world while rendering or gl states will be disturbed
            if (entity instanceof Portal) {
                ClientWorldLoader.getWorld(((Portal) entity).dimensionTo);
                clientPortalSpawnSignal.emit(((Portal) entity));
            }
            
            client.getProfiler().pop();
        });
    }
    
    public static boolean getIsProcessingRedirectedMessage() {
        return taskRedirection.get() != null;
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
        ResourceKey<Level> oldTaskRedirection = taskRedirection.get();
        taskRedirection.set(dimension);
    
        try {
            packet.handle(handler);
        }
        finally {
            taskRedirection.set(oldTaskRedirection);
        }
    }
}
