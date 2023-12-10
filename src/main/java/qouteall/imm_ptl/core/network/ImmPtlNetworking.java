package qouteall.imm_ptl.core.network;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

import java.util.Objects;
import java.util.UUID;

public class ImmPtlNetworking {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // client to server
    public static record TeleportPacket(
        int dimensionId, Vec3 posBefore, UUID portalId
    ) implements FabricPacket {
        public static final PacketType<TeleportPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl:teleport"),
            TeleportPacket::read
        );
        
        public static TeleportPacket read(FriendlyByteBuf buf) {
            int dimId = buf.readVarInt();
            Vec3 pos = new Vec3(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
            );
            UUID portalId = buf.readUUID();
            return new TeleportPacket(dimId, pos, portalId);
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(dimensionId);
            buf.writeDouble(posBefore.x);
            buf.writeDouble(posBefore.y);
            buf.writeDouble(posBefore.z);
            buf.writeUUID(portalId);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        public void handle(ServerPlayer player) {
            ResourceKey<Level> dim = PortalAPI.serverIntToDimKey(
                player.server, dimensionId
            );
            
            IPGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                player, dim, posBefore, portalId
            );
        }
    }
    
    // server to client
    public static record GlobalPortalSyncPacket(
        int dimensionId, CompoundTag data
    ) implements FabricPacket {
        public static final PacketType<GlobalPortalSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl:upd_glb_ptl"),
            GlobalPortalSyncPacket::read
        );
        
        public static GlobalPortalSyncPacket read(FriendlyByteBuf buf) {
            int dimId = buf.readVarInt();
            CompoundTag compoundTag = buf.readNbt();
            return new GlobalPortalSyncPacket(dimId, compoundTag);
        }
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(dimensionId);
            buf.writeNbt(data);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        @Environment(EnvType.CLIENT)
        public void handle() {
            ResourceKey<Level> dim = PortalAPI.clientIntToDimKey(dimensionId);
            
            GlobalPortalStorage.receiveGlobalPortalSync(dim, data);
        }
    }
    
    /**
     * server to client
     * {@link ClientboundAddEntityPacket}
     * This packet is redirected, so there is no need to contain dimension id
     */
    public static record PortalSyncPacket(
        int id,
        UUID uuid,
        EntityType<?> type,
        int dimensionId,
        double x,
        double y,
        double z,
        CompoundTag extraData
    ) implements FabricPacket {
        
        public PortalSyncPacket {
            // debug
//            Helper.LOGGER.info("PortalSyncPacket create {}", MiscHelper.getServer().overworld().getGameTime());
        }
        
        public static final PacketType<PortalSyncPacket> TYPE = PacketType.create(
            new ResourceLocation("imm_ptl:spawn_portal"),
            PortalSyncPacket::read
        );
        
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(id);
            buf.writeUUID(uuid);
            buf.writeId(BuiltInRegistries.ENTITY_TYPE, type);
            buf.writeVarInt(dimensionId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeNbt(extraData);
        }
        
        public static PortalSyncPacket read(FriendlyByteBuf buf) {
            int id = buf.readVarInt();
            UUID uuid = buf.readUUID();
            EntityType<?> type = buf.readById(BuiltInRegistries.ENTITY_TYPE);
            int dimensionId = buf.readVarInt();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            CompoundTag extraData = buf.readNbt();
            return new PortalSyncPacket(id, uuid, type, dimensionId, x, y, z, extraData);
        }
        
        @Override
        public PacketType<?> getType() {
            return TYPE;
        }
        
        /**
         * {@link ClientPacketListener#handleAddEntity(ClientboundAddEntityPacket)}
         */
        @Environment(EnvType.CLIENT)
        public void handle() {
//            Helper.LOGGER.info("PortalSyncPacket handle {}", RenderStates.frameIndex);
            
            ResourceKey<Level> dimension = PortalAPI.clientIntToDimKey(dimensionId);
            ClientLevel world = ClientWorldLoader.getWorld(dimension);
            
            Entity existing = world.getEntity(id);
            
            if (existing instanceof Portal existingPortal) {
                // update existing portal (handles default animation)
                if (!Objects.equals(existingPortal.getUUID(), uuid)) {
                    LOGGER.error("UUID mismatch when syncing portal {} {}", existingPortal, uuid);
                    return;
                }
                
                if (existingPortal.getType() != type) {
                    LOGGER.error("Entity type mismatch when syncing portal {} {}", existingPortal, type);
                    return;
                }
                
                existingPortal.acceptDataSync(new Vec3(x, y, z), extraData);
            }
            else {
                // spawn new portal
                Entity entity = type.create(world);
                Validate.notNull(entity, "Entity type is null");
                
                if (!(entity instanceof Portal portal)) {
                    LOGGER.error("Spawned entity is not a portal. {} {}", entity, type);
                    return;
                }
                
                entity.setId(id);
                entity.setUUID(uuid);
                entity.syncPacketPositionCodec(x, y, z);
                entity.moveTo(x, y, z);
                
                portal.readPortalDataFromNbt(extraData);
                
                world.addEntity(entity);
                
                ClientWorldLoader.getWorld(portal.dimensionTo);
                Portal.CLIENT_PORTAL_SPAWN_EVENT.invoker().accept(portal);
                
                if (IPGlobal.clientPortalLoadDebug) {
                    LOGGER.info("Portal loaded to client {}", portal);
                }
            }
        }
    }
    
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(
            TeleportPacket.TYPE,
            (packet, player, responseSender) -> packet.handle(player)
        );
    }
    
    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(
            GlobalPortalSyncPacket.TYPE,
            (packet, player, responseSender) -> packet.handle()
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            PortalSyncPacket.TYPE,
            (packet, player, responseSender) -> packet.handle()
        );
    }
    
}
