package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.dimension_sync.DimId;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;

@Mixin(ServerboundMovePlayerPacket.PosRot.class)
public class MixinPlayerMoveC2SPacketFull {
    @Inject(method = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;read(Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;", at = @At("RETURN"), cancellable = true)
    private static void onRead(
        FriendlyByteBuf buf, CallbackInfoReturnable<ServerboundMovePlayerPacket.PosRot> cir
    ) {
        ResourceKey<Level> playerDim = DimId.readWorldId(buf, false);
        ((IEPlayerMoveC2SPacket) cir.getReturnValue()).setPlayerDimension(playerDim);
    }
}
