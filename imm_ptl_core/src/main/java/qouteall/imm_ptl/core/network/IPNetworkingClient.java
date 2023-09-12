package qouteall.imm_ptl.core.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.imm_ptl.core.teleportation.ClientTeleportationManager;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.my_util.SignalArged;

import java.util.Optional;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class IPNetworkingClient {
    
    public static final SignalArged<Portal> clientPortalSpawnSignal = new SignalArged<>();
    private static final Minecraft client = Minecraft.getInstance();
    
    public static void init() {
    
    }
    
    public static boolean handleImmPtlCorePacketClientSide(
        ResourceLocation packedId,
        FriendlyByteBuf buf
    ) {
        if (packedId.equals(IPNetworking.id_stcSpawnEntity)) {
            processStcSpawnEntity(new FriendlyByteBuf(buf.slice()));
            return true;
        }
        else if (packedId.equals(IPNetworking.id_stcDimensionConfirm)) {
            processStcDimensionConfirm(new FriendlyByteBuf(buf.slice()));
            return true;
        }
        else if (packedId.equals(IPNetworking.id_stcUpdateGlobalPortal)) {
            processGlobalPortalUpdate(new FriendlyByteBuf(buf.slice()));
            return true;
        }
        else {
            return false;
        }
    }
    
    private static void processStcSpawnEntity(FriendlyByteBuf buf) {
        String entityTypeString = buf.readUtf();
        
        int entityId = buf.readInt();
        
        ResourceKey<Level> dim = DimId.readWorldId(buf, true);
        
        CompoundTag compoundTag = buf.readNbt();
        
        processEntitySpawn(entityTypeString, entityId, dim, compoundTag);
    }
    
    private static void processStcDimensionConfirm(FriendlyByteBuf buf) {
        
        ResourceKey<Level> dimension = DimId.readWorldId(buf, true);
        Vec3 pos = new Vec3(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        
        MiscHelper.executeOnRenderThread(() -> {
            ClientTeleportationManager.acceptSynchronizationDataFromServer(
                dimension, pos,
                false
            );
        });
    }
    
    private static void processGlobalPortalUpdate(FriendlyByteBuf buf) {
        ResourceKey<Level> dimension = DimId.readWorldId(buf, true);
        CompoundTag compoundTag = buf.readNbt();
        MiscHelper.executeOnRenderThread(() -> {
            GlobalPortalStorage.receiveGlobalPortalSync(dimension, compoundTag);
        });
    }
    
//    public static Packet createCtsPlayerAction(
//        ResourceKey<Level> dimension,
//        ServerboundPlayerActionPacket packet
//    ) {
//        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//        DimId.writeWorldId(buf, dimension, true);
//        packet.write(buf);
//        return new ServerboundCustomPayloadPacket(IPNetworking.id_ctsPlayerAction, buf);
//    }
//
//    public static Packet createCtsRightClick(
//        ResourceKey<Level> dimension,
//        ServerboundUseItemOnPacket packet
//    ) {
//        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
//        DimId.writeWorldId(buf, dimension, true);
//        packet.write(buf);
//        return new ServerboundCustomPayloadPacket(IPNetworking.id_ctsRightClick, buf);
//    }
    
    public static Packet createCtsTeleport(
        ResourceKey<Level> dimensionBefore,
        Vec3 posBefore,
        UUID portalEntityId
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionBefore, true);
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUUID(portalEntityId);
        return new ServerboundCustomPayloadPacket(IPNetworking.id_ctsTeleport, buf);
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
}
