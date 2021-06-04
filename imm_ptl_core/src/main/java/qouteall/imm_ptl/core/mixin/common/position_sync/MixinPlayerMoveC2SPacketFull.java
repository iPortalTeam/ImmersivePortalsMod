package qouteall.imm_ptl.core.mixin.common.position_sync;

import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerMoveC2SPacket.Full.class)
public class MixinPlayerMoveC2SPacketFull {
    @Inject(method = "read", at = @At("RETURN"), cancellable = true)
    private static void onRead(
        PacketByteBuf buf, CallbackInfoReturnable<PlayerMoveC2SPacket.Full> cir
    ) {
        RegistryKey<World> playerDim = DimId.readWorldId(buf, false);
        ((IEPlayerMoveC2SPacket) cir.getReturnValue()).setPlayerDimension(playerDim);
    }
}
