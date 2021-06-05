package qouteall.imm_ptl.core.mixin.client.sync;

import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.imm_ptl.core.network.IPNetworkAdapt;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerPositionLookS2CPacket.class)
public class MixinPlayerPositionLookS2CPacket_C {
    @Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("RETURN"))
    private void onRead(PacketByteBuf buf, CallbackInfo ci) {
        if (buf.isReadable()) {
            RegistryKey<World> playerDimension = DimId.readWorldId(buf, true);
            ((IEPlayerPositionLookS2CPacket) this).setPlayerDimension(playerDimension);
            IPNetworkAdapt.setServerHasIP(true);
        }
        else {
            IPNetworkAdapt.setServerHasIP(false);
        }
    }
    
}
