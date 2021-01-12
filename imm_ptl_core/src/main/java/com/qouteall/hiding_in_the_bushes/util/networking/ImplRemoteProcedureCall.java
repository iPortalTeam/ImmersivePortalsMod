package com.qouteall.hiding_in_the_bushes.util.networking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.qouteall.hiding_in_the_bushes.MyNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class ImplRemoteProcedureCall {
    public static final Gson gson;
    
    static {
        gson = new GsonBuilder().create();
    }
    
    public static CustomPayloadC2SPacket createC2SPacket(
        String methodPath,
        Object... arguments
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        
        serializeStringWithArguments(methodPath, arguments, buf);
        
        return new CustomPayloadC2SPacket(MyNetwork.id_ctsRemote, buf);
    }
    
    public static CustomPayloadS2CPacket createS2CPacket(
        String methodPath,
        Object... arguments
    ) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
    
        serializeStringWithArguments(methodPath, arguments, buf);
    
        return new CustomPayloadS2CPacket(MyNetwork.id_stcRemote, buf);
    }
    
    public static void clientHandlePacket(PacketByteBuf buf) {
    
    }
    
    public static void serverHandlePacket(ServerPlayerEntity player, PacketByteBuf buf) {
    
    }
    
    public static void serializeStringWithArguments(String methodPath, Object[] arguments, PacketByteBuf buf) {
        buf.writeString(methodPath);
        
        Object[] argumentArray = arguments;
        
        String jsonInfo = gson.toJson(argumentArray);
        buf.writeString(jsonInfo);
    }
    
}
