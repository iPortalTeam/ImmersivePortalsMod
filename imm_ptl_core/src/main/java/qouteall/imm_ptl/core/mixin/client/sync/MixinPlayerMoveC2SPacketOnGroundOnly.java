package qouteall.imm_ptl.core.mixin.client.sync;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;
import qouteall.imm_ptl.core.network.IPNetworkAdapt;

@Mixin(PlayerMoveC2SPacket.OnGroundOnly.class)
public class MixinPlayerMoveC2SPacketOnGroundOnly {
    @Inject(method = "write", at = @At("RETURN"))
    private void onWrite(PacketByteBuf buf, CallbackInfo ci) {
        if (!IPNetworkAdapt.doesServerHasIP()) {return;}
        RegistryKey<World> playerDimension = ((IEPlayerMoveC2SPacket) this).getPlayerDimension();
        Validate.notNull(playerDimension);
        DimId.writeWorldId(buf, playerDimension, true);
    }
}
