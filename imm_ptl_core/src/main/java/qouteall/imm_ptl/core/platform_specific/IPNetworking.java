package qouteall.imm_ptl.core.platform_specific;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.dimension_sync.DimensionIdRecord;
import qouteall.imm_ptl.core.dimension_sync.DimensionTypeSync;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.q_misc_util.MiscHelper;

import java.util.UUID;

public class IPNetworking {
    public static final Identifier id_stcRedirected =
        new Identifier("imm_ptl", "rd");
    public static final Identifier id_stcDimSync =
        new Identifier("imm_ptl", "dim_sync");
    public static final Identifier id_ctsTeleport =
        new Identifier("imm_ptl", "teleport");
    public static final Identifier id_stcCustom =
        new Identifier("imm_ptl", "stc_custom");
    public static final Identifier id_stcSpawnEntity =
        new Identifier("imm_ptl", "spawn_entity");
    public static final Identifier id_stcDimensionConfirm =
        new Identifier("imm_ptl", "dim_confirm");
    public static final Identifier id_stcUpdateGlobalPortal =
        new Identifier("imm_ptl", "upd_glb_ptl");
    public static final Identifier id_ctsPlayerAction =
        new Identifier("imm_ptl", "player_action");
    public static final Identifier id_ctsRightClick =
        new Identifier("imm_ptl", "right_click");
    
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsTeleport,
            (server, player, handler, buf, responseSender) -> {
                processCtsTeleport(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsPlayerAction,
            (server, player, handler, buf, responseSender) -> {
                processCtsPlayerAction(player, buf);
            }
        );
        
        ServerPlayNetworking.registerGlobalReceiver(
            id_ctsRightClick,
            (server, player, handler, buf, responseSender) -> {
                processCtsRightClick(player, buf);
            }
        );
        
        
        
    }
    
    public static Packet createRedirectedMessage(
        RegistryKey<World> dimension,
        Packet packet
    ) {
        int messageType = 0;
        try {
            messageType = NetworkState.PLAY.getPacketId(NetworkSide.CLIENTBOUND, packet);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, dimension, false);
        
        buf.writeInt(messageType);
    
        packet.write(buf);
    
        return new CustomPayloadS2CPacket(id_stcRedirected, buf);
    }
    
    public static Packet createDimSync() {
        Validate.notNull(DimensionIdRecord.serverRecord);
        
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        NbtCompound idMapTag = DimensionIdRecord.recordToTag(DimensionIdRecord.serverRecord);
        buf.writeNbt(idMapTag);
        
        NbtCompound typeMapTag = DimensionTypeSync.createTagFromServerWorldInfo();
        buf.writeNbt(typeMapTag);
        
        return new CustomPayloadS2CPacket(id_stcDimSync, buf);
    }
    
    public static void sendRedirectedMessage(
        ServerPlayerEntity player,
        RegistryKey<World> dimension,
        Packet packet
    ) {
        player.networkHandler.sendPacket(createRedirectedMessage(dimension, packet));
    }
    
    public static Packet createStcDimensionConfirm(
        RegistryKey<World> dimensionType,
        Vec3d pos
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionType, false);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new CustomPayloadS2CPacket(id_stcDimensionConfirm, buf);
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static Packet createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(EntityType.getId(entityType).toString());
        buf.writeInt(entity.getId());
        DimId.writeWorldId(
            buf, entity.world.getRegistryKey(),
            entity.world.isClient
        );
        NbtCompound tag = new NbtCompound();
        entity.writeNbt(tag);
        buf.writeNbt(tag);
        return new CustomPayloadS2CPacket(id_stcSpawnEntity, buf);
    }
    
    public static Packet createGlobalPortalUpdate(
        GlobalPortalStorage storage
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, storage.world.get().getRegistryKey(), false);
        buf.writeNbt(storage.writeNbt(new NbtCompound()));
        
        return new CustomPayloadS2CPacket(id_stcUpdateGlobalPortal, buf);
    }
    
    private static void processCtsTeleport(ServerPlayerEntity player, PacketByteBuf buf) {
        RegistryKey<World> dim = DimId.readWorldId(buf, false);
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        UUID portalEntityId = buf.readUuid();
        
        MiscHelper.executeOnServerThread(() -> {
            IPGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                player,
                dim,
                posBefore,
                portalEntityId
            );
        });
    }
    
    private static void processCtsPlayerAction(ServerPlayerEntity player, PacketByteBuf buf) {
        RegistryKey<World> dim = DimId.readWorldId(buf, false);
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket(buf);
        IPGlobal.serverTaskList.addTask(() -> {
            BlockManipulationServer.processBreakBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
    private static void processCtsRightClick(ServerPlayerEntity player, PacketByteBuf buf) {
        RegistryKey<World> dim = DimId.readWorldId(buf, false);
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(buf);
        IPGlobal.serverTaskList.addTask(() -> {
            BlockManipulationServer.processRightClickBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
}
