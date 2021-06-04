package com.qouteall.immersive_portals.mixin.common.chunk_sync;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CustomPayloadS2CPacket.class)
public class MixinCustomPayloadS2CPacket {
    // TODO write own packet handling without custom payload which may be faster
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V",
        constant = @Constant(intValue = 1048576)
    )
    private int modifySizeLimit1(int oldValue){
        return 233333333;
    }
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/network/PacketByteBuf;)V",
        constant = @Constant(intValue = 1048576)
    )
    private int modifySizeLimit2(int oldValue){
        return 233333333;
    }
    
    
    
//    @Shadow
//    private Identifier channel;
//    @Shadow
//    private PacketByteBuf data;
//    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
//
//    @Redirect(
//        method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/network/PacketByteBuf;)V",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/network/PacketByteBuf;writerIndex()I"
//        )
//    )
//    public int redirectWriterIndex(PacketByteBuf packetByteBuf) {
//        int writerIndex = packetByteBuf.writerIndex();
//        if (writerIndex > 1048576) {
//            limitedLogger.invoke(() -> {
//                Helper.err("Sending very big packet " + writerIndex);
//                new Throwable().printStackTrace();
//            });
//        }
//        return 0;
//    }
//
//    @Redirect(
//        method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/network/PacketByteBuf;readableBytes()I"
//        )
//    )
//    public int redirectReadableBytes(PacketByteBuf buf) {
//
//    }
//
//    /**
//     * @author cozyGalvinism
//     * @reason Allow for bigger packets to be sent
//     */
//    @Overwrite
//    public void read(PacketByteBuf packetByteBuf) {
//        this.channel = packetByteBuf.readIdentifier();
//        int i = packetByteBuf.readableBytes();
//        if (i < 0 || i > 1048576) {
//            limitedLogger.invoke(() -> {
//                Helper.err("Received very big packet " + i);
//                new Throwable().printStackTrace();
//            });
//        }
//        this.data = new PacketByteBuf(packetByteBuf.readBytes(i));
//    }
}
