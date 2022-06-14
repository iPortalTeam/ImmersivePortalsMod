package qouteall.imm_ptl.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.ducks.IEClientPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEClientWorld;
import qouteall.imm_ptl.core.ducks.IEMinecraftClient;
import qouteall.imm_ptl.core.ducks.IEParticleManager;
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
    
    public static boolean isProcessingRedirectedMessage = false;
    
    
    public static void processRedirectedPacket(ResourceKey<Level> dimension, Packet packet) {
        Runnable func = () -> {
            try {
                client.getProfiler().push("process_redirected_packet");
                
                ClientLevel packetWorld = ClientWorldLoader.getOptionalWorld(dimension);
                
                if (packetWorld != null) {
                    doProcessRedirectedMessage(packetWorld, packet);
                }
                else {
                    Helper.err(
                        "Ignoring packet of invalid dimension %s %s"
                            .formatted(dimension.location(), packet.getClass())
                    );
                }
            }
            finally {
                client.getProfiler().pop();
            }
        };
        
        MiscHelper.executeOnRenderThread(func);
    }
    
    
    public static void doProcessRedirectedMessage(
        ClientLevel packetWorld,
        Packet packet
    ) {
        boolean oldIsProcessing = isProcessingRedirectedMessage;
        
        isProcessingRedirectedMessage = true;
        
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
            
            isProcessingRedirectedMessage = oldIsProcessing;
        }
    }
    
    public static void withSwitchedWorld(ClientLevel newWorld, Runnable runnable) {
        withSwitchedWorld(newWorld, () -> {
            runnable.run();
            return null; // Must return null for "void" supplier
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
                throw new RuntimeException("Respawn packet should not be redirected");
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
            entity.setPacketCoordinates(entity.getX(), entity.getY(), entity.getZ());
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
        return isProcessingRedirectedMessage;
    }
}
