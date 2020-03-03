package com.qouteall.modloader_agnostic_api;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationServer;
import com.qouteall.immersive_portals.my_util.ICustomStcPacket;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.PacketContext;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.UUID;

public class MyNetwork {
    public static final Identifier id_ctsTeleport =
        new Identifier("imm_ptl", "teleport");
    public static final Identifier id_stcCustom =
        new Identifier("imm_ptl", "stc_custom");
    public static final Identifier id_stcSpawnEntity =
        new Identifier("imm_ptl", "spawn_entity");
    public static final Identifier id_stcDimensionConfirm =
        new Identifier("imm_ptl", "dim_confirm");
    public static final Identifier id_stcRedirected =
        new Identifier("imm_ptl", "rd");
    public static final Identifier id_stcUpdateGlobalPortal =
        new Identifier("imm_ptl", "upd_glb_ptl");
    public static final Identifier id_ctsPlayerAction =
        new Identifier("imm_ptl", "player_action");
    public static final Identifier id_ctsRightClick =
        new Identifier("imm_ptl", "right_click");
    
    public static void init() {
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsTeleport,
            MyNetwork::processCtsTeleport
        );
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsPlayerAction,
            MyNetwork::processCtsPlayerAction
        );
        ServerSidePacketRegistry.INSTANCE.register(
            id_ctsRightClick,
            MyNetwork::processCtsRightClick
        );
    
    }
    
    public static Packet createRedirectedMessage(
        DimensionType dimension,
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
        buf.writeInt(dimension.getRawId());
        buf.writeInt(messageType);
        
        try {
            packet.write(buf);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        return new CustomPayloadS2CPacket(id_stcRedirected, buf);
    }
    
    public static Packet createEmptyPacketByType(
        int messageType
    ) {
        return NetworkState.PLAY.getPacketHandler(NetworkSide.CLIENTBOUND, messageType);
    }
    
    public static void sendRedirectedMessage(
        ServerPlayerEntity player,
        DimensionType dimension,
        Packet packet
    ) {
        player.networkHandler.sendPacket(createRedirectedMessage(dimension, packet));
    }
    
    public static Packet createStcDimensionConfirm(
        DimensionType dimensionType,
        Vec3d pos
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(dimensionType.getRawId());
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        return new CustomPayloadS2CPacket(id_stcDimensionConfirm, buf);
    }
    
    //you can input a lambda expression and it will be invoked remotely
    //but java serialization is not stable
    @Deprecated
    public static Packet createCustomPacketStc(
        ICustomStcPacket serializable
    ) {
        //it copies the data twice but as the packet is small it's of no problem
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(byteArrayOutputStream);
            stream.writeObject(serializable);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(byteArrayOutputStream.toByteArray());
        
        PacketByteBuf buf = new PacketByteBuf(buffer);
        
        return new CustomPayloadS2CPacket(id_stcCustom, buf);
    }
    
    //NOTE my packet is redirected but I cannot get the packet handler info here
    public static Packet createStcSpawnEntity(
        Entity entity
    ) {
        EntityType entityType = entity.getType();
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeString(EntityType.getId(entityType).toString());
        buf.writeInt(entity.getEntityId());
        buf.writeInt(entity.dimension.getRawId());
        CompoundTag tag = new CompoundTag();
        entity.toTag(tag);
        buf.writeCompoundTag(tag);
        return new CustomPayloadS2CPacket(id_stcSpawnEntity, buf);
    }
    
    public static Packet createGlobalPortalUpdate(
        GlobalPortalStorage storage
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        buf.writeInt(storage.world.get().dimension.getType().getRawId());
        buf.writeCompoundTag(storage.toTag(new CompoundTag()));
        
        return new CustomPayloadS2CPacket(id_stcUpdateGlobalPortal, buf);
    }
    
    private static void processCtsTeleport(PacketContext context, PacketByteBuf buf) {
        DimensionType dimensionBefore = DimensionType.byRawId(buf.readInt());
        Vec3d posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        UUID portalEntityId = buf.readUuid();
        
        McHelper.getServer().execute(() -> {
            Global.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.getPlayer(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
        });
    }
    
    private static void processCtsPlayerAction(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket();
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ModMain.serverTaskList.addTask(() -> {
            BlockManipulationServer.processBreakBlock(
                dimension, packet,
                ((ServerPlayerEntity) context.getPlayer())
            );
            return true;
        });
    }
    
    private static void processCtsRightClick(PacketContext context, PacketByteBuf buf) {
        DimensionType dimension = DimensionType.byRawId(buf.readInt());
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket();
        try {
            packet.read(buf);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
        ModMain.serverTaskList.addTask(() -> {
            BlockManipulationServer.processRightClickBlock(
                dimension, packet,
                ((ServerPlayerEntity) context.getPlayer())
            );
            return true;
        });
    }
}
