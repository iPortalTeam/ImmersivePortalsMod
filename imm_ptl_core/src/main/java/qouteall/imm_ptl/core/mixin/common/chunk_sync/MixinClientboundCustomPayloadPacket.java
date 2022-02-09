package qouteall.imm_ptl.core.mixin.common.chunk_sync;

import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientboundCustomPayloadPacket.class)
public class MixinClientboundCustomPayloadPacket {
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
}
