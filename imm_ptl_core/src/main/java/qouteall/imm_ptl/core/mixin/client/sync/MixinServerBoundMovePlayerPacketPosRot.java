package qouteall.imm_ptl.core.mixin.client.sync;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.imm_ptl.core.ducks.IEPlayerMoveC2SPacket;
import qouteall.imm_ptl.core.network.IPNetworkAdapt;

@Environment(EnvType.CLIENT)
@Mixin(ServerboundMovePlayerPacket.PosRot.class)
public class MixinServerBoundMovePlayerPacketPosRot {
    @Inject(method = "Lnet/minecraft/network/protocol/game/ServerboundMovePlayerPacket$PosRot;write(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void onWrite(FriendlyByteBuf buf, CallbackInfo ci) {
        if (!IPNetworkAdapt.doesServerHasIP()) {return;}
        ResourceKey<Level> playerDimension = ((IEPlayerMoveC2SPacket) this).getPlayerDimension();
        Validate.notNull(playerDimension);
        DimId.writeWorldId(buf, playerDimension, true);
    }
}
