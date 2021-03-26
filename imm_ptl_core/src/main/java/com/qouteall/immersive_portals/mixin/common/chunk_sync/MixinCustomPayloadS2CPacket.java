package com.qouteall.immersive_portals.mixin.common.chunk_sync;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CustomPayloadS2CPacket.class)
public class MixinCustomPayloadS2CPacket {
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    @Redirect(
        method = "<init>(Lnet/minecraft/util/Identifier;Lnet/minecraft/network/PacketByteBuf;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketByteBuf;writerIndex()I"
        )
    )
    public int redirectWriterIndex(PacketByteBuf packetByteBuf) {
        int writerIndex = packetByteBuf.writerIndex();
        if (writerIndex > 1048576) {
            limitedLogger.invoke(() -> {
                Helper.err("Sending very big packet");
                new Throwable().printStackTrace();
            });
        }
        return 0;
    }
}
