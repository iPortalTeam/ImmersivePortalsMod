package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
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
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class MyNetworkClient {
    
    public static void init() {
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcCustom,
            MyNetworkClient::handleCustomPacketStc
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
            MyNetwork.id_stcRedirected,
            MyNetworkClient::processRedirectedMessage
        );
        
        ClientSidePacketRegistry.INSTANCE.register(
            MyNetwork.id_stcUpdateGlobalPortal,
            MyNetworkClient::processGlobalPortalUpdate
        );
    }
    
    @Deprecated
    private static void handleCustomPacketStc(PacketContext context, PacketByteBuf buf) {
        ByteBuffer byteBuffer = buf.nioBuffer();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteBuffer.array());
        ObjectInputStream objectInputStream;
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object obj = objectInputStream.readObject();
            ICustomStcPacket customStcPacket = (ICustomStcPacket) obj;
            customStcPacket.handle();
        }
        catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    private static void processStcSpawnEntity(PacketContext context, PacketByteBuf buf) {
        String entityTypeString = buf.readString();
        int entityId = buf.readInt();
        int dimId = buf.readInt();
        DimensionType dimensionType = DimensionType.byRawId(dimId);
        CompoundTag compoundTag = buf.readCompoundTag();
        
        if (dimensionType == null) {
            Helper.err(String.format(
                "Invalid dimension for spawning entity %s %s %s",
                dimId, entityTypeString, compoundTag
            ));
        }
        
        Optional<EntityType<?>> entityType = EntityType.get(entityTypeString);
        if (!entityType.isPresent()) {
            Helper.err("unknown entity type " + entityTypeString);
            return;
        }
        
        //without this delay it will flash? or it's random?
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld world = CGlobal.clientWorldLoader.getWorld(dimensionType);
            
            if (world.getEntityById(entityId) != null) {
                Helper.err(String.format(
                    "duplicate entity %s %s %s",
                    ((Integer) entityId).toString(),
                    entityType.get().getTranslationKey(),
                    compoundTag
                ));
                return;
            }
            
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
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
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
    
    private static void processSpawnLoadingIndicator(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        Vec3d pos = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MinecraftClient.getInstance().execute(() -> {
            ClientWorld world = CGlobal.clientWorldLoader.getWorld(dimension);
            if (world == null) {
                return;
            }
            
            LoadingIndicatorEntity indicator = new LoadingIndicatorEntity(
                LoadingIndicatorEntity.entityType, world
            );
            indicator.updatePosition(pos.x, pos.y, pos.z);
            
            world.addEntity(233333333, indicator);
        });
    }
    
    public static void processRedirectedMessage(
        PacketContext context,
        PacketByteBuf buf
    ) {
        int dimensionId = buf.readInt();
        int messageType = buf.readInt();
        DimensionType dimension = DimensionType.byRawId(dimensionId);
        Packet packet = MyNetwork.createEmptyPacketByType(messageType);
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        if (dimension == null) {
            Helper.err(String.format(
                "Invalid redirected packet %s %s \nRegistered dimensions %s",
                dimensionId, packet,
                Registry.DIMENSION_TYPE.stream().map(
                    dim -> dim.toString() + " " + dim.getRawId()
                ).collect(Collectors.joining("\n"))
            ));
            return;
        }
        
        processRedirectedPacket(dimension, packet);
    }
    
    private static int reportedError = 0;
    
    private static void processRedirectedPacket(DimensionType dimension, Packet packet) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            ClientWorld packetWorld = CGlobal.clientWorldLoader.getWorld(dimension);
            
            assert packetWorld != null;
            
            assert packetWorld.getChunkManager() instanceof MyClientChunkManager;
            
            ClientPlayNetworkHandler netHandler = ((IEClientWorld) packetWorld).getNetHandler();
            
            if ((netHandler).getWorld() != packetWorld) {
                ((IEClientPlayNetworkHandler) netHandler).setWorld(packetWorld);
                Helper.err("The world field of client net handler is wrong");
            }
            
            ClientWorld originalWorld = mc.world;
            //some packet handling may use mc.world so switch it
            mc.world = packetWorld;
            ((IEParticleManager) mc.particleManager).mySetWorld(packetWorld);
            
            try {
                packet.apply(netHandler);
            }
            catch (Throwable e) {
                if (reportedError < 200) {
                    reportedError += 1;
                    throw new IllegalStateException(
                        "handling packet in " + dimension, e
                    );
                }
            }
            finally {
                mc.world = originalWorld;
                ((IEParticleManager) mc.particleManager).mySetWorld(originalWorld);
            }
        });
    }
    
    private static void processGlobalPortalUpdate(PacketContext context, PacketByteBuf buf) {
        DimensionType dimensionType = DimensionType.byRawId(buf.readInt());
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
        DimensionType dimension,
        PlayerActionC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimension.getRawId());
        try {
            packet.write(buf);
        }
        
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsPlayerAction, buf);
    }
    
    public static Packet createCtsRightClick(
        DimensionType dimension,
        PlayerInteractBlockC2SPacket packet
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimension.getRawId());
        try {
            packet.write(buf);
        }
        
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsRightClick, buf);
    }
    
    public static Packet createCtsTeleport(
        DimensionType dimensionBefore,
        Vec3d posBefore,
        UUID portalEntityId
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimensionBefore.getRawId());
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUuid(portalEntityId);
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsTeleport, buf);
    }
}
