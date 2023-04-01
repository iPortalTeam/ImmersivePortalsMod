package qouteall.imm_ptl.core.network;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.MiscHelper;

import java.util.UUID;

public class IPNetworking {
    
    public static final ResourceLocation id_ctsTeleport =
        new ResourceLocation("imm_ptl", "teleport");
    public static final ResourceLocation id_stcSpawnEntity =
        new ResourceLocation("imm_ptl", "spawn_entity");
    public static final ResourceLocation id_stcDimensionConfirm =
        new ResourceLocation("imm_ptl", "dim_confirm");
    public static final ResourceLocation id_stcUpdateGlobalPortal =
        new ResourceLocation("imm_ptl", "upd_glb_ptl");
    public static final ResourceLocation id_ctsPlayerAction =
        new ResourceLocation("imm_ptl", "player_action");
    public static final ResourceLocation id_ctsRightClick =
        new ResourceLocation("imm_ptl", "right_click");
    
    public static void init() {
        
    }
    
    // return true for handled
    public static boolean handleImmPtlCorePacketServerSide(
        ResourceLocation packedId,
        ServerPlayer player, FriendlyByteBuf buf
    ) {
        if (id_ctsTeleport.equals(packedId)) {
            processCtsTeleport(player, buf);
            return true;
        }
        else if (id_ctsPlayerAction.equals(packedId)) {
            processCtsPlayerAction(player, buf);
            return true;
        }
        else if (id_ctsRightClick.equals(packedId)) {
            processCtsRightClick(player, buf);
            return true;
        }
        else {
            return false;
        }
    }
    
    // invoking client-only method. avoid dedicated server crash
    public static boolean handleImmPtlCorePacketClientSide(
        ResourceLocation packedId,
        FriendlyByteBuf buf
    ) {
        return IPNetworkingClient.handleImmPtlCorePacketClientSide(packedId, buf);
    }
    
    public static Packet createStcDimensionConfirm(
        ResourceKey<Level> dimensionType,
        Vec3 pos
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        DimId.writeWorldId(buf, dimensionType, false);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new ClientboundCustomPayloadPacket(id_stcDimensionConfirm, buf);
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static Packet createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(EntityType.getKey(entityType).toString());
        buf.writeInt(entity.getId());
        DimId.writeWorldId(
            buf, entity.level.dimension(),
            entity.level.isClientSide
        );
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        buf.writeNbt(tag);
        return new ClientboundCustomPayloadPacket(id_stcSpawnEntity, buf);
    }
    
    public static Packet createGlobalPortalUpdate(
        GlobalPortalStorage storage
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        
        DimId.writeWorldId(buf, storage.world.get().dimension(), false);
        buf.writeNbt(storage.save(new CompoundTag()));
        
        return new ClientboundCustomPayloadPacket(id_stcUpdateGlobalPortal, buf);
    }
    
    private static void processCtsTeleport(ServerPlayer player, FriendlyByteBuf buf) {
        ResourceKey<Level> dim = DimId.readWorldId(buf, false);
        Vec3 posBefore = new Vec3(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        UUID portalEntityId = buf.readUUID();
        
        MiscHelper.executeOnServerThread(() -> {
            IPGlobal.serverTeleportationManager.onPlayerTeleportedInClient(
                player,
                dim,
                posBefore,
                portalEntityId
            );
        });
    }
    
    private static void processCtsPlayerAction(ServerPlayer player, FriendlyByteBuf buf) {
        ResourceKey<Level> dim = DimId.readWorldId(buf, false);
        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(buf);
        IPGlobal.serverTaskList.addTask(() -> {
            BlockManipulationServer.processBreakBlock(
                dim, packet,
                player
            );
            return true;
        });
    }
    
    private static void processCtsRightClick(ServerPlayer player, FriendlyByteBuf buf) {
        ResourceKey<Level> dim = DimId.readWorldId(buf, false);
        ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(buf);
        IPGlobal.serverTaskList.addTask(() -> {
            BlockManipulationServer.processRightClickBlock(
                dim, packet, player, packet.getSequence()
            );
            return true;
        });
    }
    
    public static class RemoteCallables {
        public static void onClientPlayerUpdatePose(
            ServerPlayer player, Pose pose
        ) {
            player.setPose(pose);
        }
    }
    
}
