package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientboundCustomPayloadPacket.class)
public class MixinCustomPayloadS2CPacket {
    // TODO write own packet handling without custom payload which may be faster
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V",
        constant = @Constant(intValue = 1048576)
    )
    private int modifySizeLimit1(int oldValue){
        return 233333333;
    }
    
    @ModifyConstant(
        method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)V",
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
