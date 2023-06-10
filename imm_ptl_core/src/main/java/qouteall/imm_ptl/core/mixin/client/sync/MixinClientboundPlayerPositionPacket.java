package qouteall.imm_ptl.core.mixin.client.sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.network.IPNetworkAdapt;
import qouteall.q_misc_util.dimension.DimId;

@Mixin(ClientboundPlayerPositionPacket.class)
public class MixinClientboundPlayerPositionPacket {
    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void onRead(FriendlyByteBuf buf, CallbackInfo ci) {
        if (buf.isReadable()) {
            ResourceKey<Level> playerDimension = DimId.readWorldId(buf, true);
            ((IEPlayerPositionLookS2CPacket) this).setPlayerDimension(playerDimension);
            IPNetworkAdapt.setServerHasIP(true);
        }
        else {
            IPNetworkAdapt.setServerHasIP(false);
        }
    }
    
}
