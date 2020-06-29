package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.dimension_sync.DimensionIdRecord;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.render.lag_spike_fix.SmoothLoading;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class MyNetworkClient {
    
    private static MinecraftClient client = MinecraftClient.getInstance();
    
    public static void init() {
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcRedirected,
            MyNetworkClient::processRedirectedMessage
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcDimSync,
            MyNetworkClient::processDimSync
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcSpawnEntity,
            MyNetworkClient::processStcSpawnEntity
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcDimensionConfirm,
            MyNetworkClient::processStcDimensionConfirm
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcUpdateGlobalPortal,
            MyNetworkClient::processGlobalPortalUpdate
        );
        
    }
    
    private static void processStcSpawnEntity(PacketContext context, PacketByteBuf buf) {
        String entityTypeString = buf.readString();
    
        int entityId = buf.readInt();
        
        RegistryKey<World> dim = DimId.readWorldId(buf, EnvType.CLIENT);
        
        CompoundTag compoundTag = buf.readCompoundTag();
        
        Optional<EntityType<?>> entityType = EntityType.get(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
        
        //without this delay it will flash? or it's random?
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld world = CGlobal.clientWorldLoader.getWorld(dim);

//            if (world.getEntityById(entityId) != null) {
//                Helper.err(String.format(
//                    "duplicate entity %s %s %s",
//                    ((Integer) entityId).toString(),
//                    entityType.get().getTranslationKey(),
//                    compoundTag
//                ));
//            }
            
            Entity entity = entityType.get().create(
                world
            );
            entity.fromTag(compoundTag);
            entity.setEntityId(entityId);
            entity.updateTrackedPosition(entity.getX(), entity.getY(), entity.getZ());
            world.addEntity(entityId, entity);
            
            //do not create client world while rendering or gl states will be disturbed
            if (entity instanceof Portal) {
                CGlobal.clientWorldLoader.getWorld(
                    ((Portal) entity).dimensionTo
                );
            }
            
            return;
        });
    }
    
    private static void processStcDimensionConfirm(PacketContext context, PacketByteBuf buf) {
        
        RegistryKey<World> dimension = DimId.readWorldId(buf, EnvType.CLIENT);
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MinecraftClient.getInstance().execute(() -> {
            CGlobal.clientTeleportationManager.acceptSynchronizationDataFromServer(
                dimension, pos,
                false
            );
        });
    }
    
    public static void processRedirectedMessage(
        PacketContext context,
        PacketByteBuf buf
    ) {
        RegistryKey<World> dimension = DimId.readWorldId(buf, EnvType.CLIENT);
        int messageType = buf.readInt();
        Packet packet = MyNetwork.createEmptyPacketByType(messageType);
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        processRedirectedPacket(dimension, packet);
    }
    
    public static void processDimSync(
        PacketContext context, PacketByteBuf buf
    ) {
        CompoundTag idMap = buf.readCompoundTag();
        
        DimensionIdRecord.clientRecord = DimensionIdRecord.tagToRecord(idMap);
        
        CompoundTag typeMap = buf.readCompoundTag();
        
        DimensionTypeSync.acceptTypeMapData(typeMap);
        
        Helper.log("Received Dimension Int Id Sync");
        Helper.log("\n" + DimensionIdRecord.clientRecord);
    }
    
    private static int reportedError = 0;
    
    private static void processRedirectedPacket(RegistryKey<World> dimension, Packet packet) {
        client.execute(() -> {
            ClientWorld packetWorld = CGlobal.clientWorldLoader.getWorld(dimension);
            
            if (SmoothLoading.filterPacket(packetWorld, packet)) {
                return;
            }
            
            assert packetWorld != null;
            
            assert packetWorld.getChunkManager() instanceof MyClientChunkManager;
            
            doProcessRedirectedMessage(packetWorld, packet);
        });
    }
    
    public static void doProcessRedirectedMessage(
        ClientWorld packetWorld,
        Packet packet
    ) {
        ClientPlayNetworkHandler netHandler = ((IEClientWorld) packetWorld).getNetHandler();
        
        if ((netHandler).getWorld() != packetWorld) {
            ((IEClientPlayNetworkHandler) netHandler).setWorld(packetWorld);
            Helper.err("The world field of client net handler is wrong");
        }
        
        ClientWorld originalWorld = client.world;
        //some packet handling may use mc.world so switch it
        client.world = packetWorld;
        ((IEParticleManager) client.particleManager).mySetWorld(packetWorld);
        
        try {
            packet.apply(netHandler);
        }
        catch (Throwable e) {
            if (reportedError < 200) {
                reportedError += 1;
                throw new IllegalStateException(
                    "handling packet in " + packetWorld.getRegistryKey(), e
                );
            }
        }
        finally {
            client.world = originalWorld;
            ((IEParticleManager) client.particleManager).mySetWorld(originalWorld);
        }
    }
    
    private static void processGlobalPortalUpdate(PacketContext context, PacketByteBuf buf) {
        RegistryKey<World> dimensionType = DimId.readWorldId(buf, EnvType.CLIENT);
        CompoundTag compoundTag = buf.readCompoundTag();
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld world =
                CGlobal.clientWorldLoader.getWorld(dimensionType);
            
            List<GlobalTrackedPortal> portals =
                GlobalPortalStorage.getPortalsFromTag(compoundTag, world);
            
            ((IEClientWorld) world).setGlobalPortals(portals);
        });
    }
    
    public static Packet createCtsPlayerAction(
        RegistryKey<World> dimension,
        PlayerActionC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, EnvType.CLIENT);
        try {
            packet.write(buf);
        }
        
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsPlayerAction, buf);
    }
    
    public static Packet createCtsRightClick(
        RegistryKey<World> dimension,
        PlayerInteractBlockC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimension, EnvType.CLIENT);
        try {
            packet.write(buf);
        }
        
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsRightClick, buf);
    }
    
    public static Packet createCtsTeleport(
        RegistryKey<World> dimensionBefore,
        Vec3d posBefore,
        UUID portalEntityId
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionBefore, EnvType.CLIENT);
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUuid(portalEntityId);
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsTeleport, buf);
    }
}
