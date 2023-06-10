package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;
import qouteall.q_misc_util.dimension.DimId;

@Mixin(ClientboundPlayerPositionPacket.class)
public class MixinPlayerPositionLookS2CPacket implements IEPlayerPositionLookS2CPacket {
    private ResourceKey<Level> playerDimension;
    
    @Override
    public ResourceKey<Level> getPlayerDimension() {
        return playerDimension;
    }
    
    @Override
    public void setPlayerDimension(ResourceKey<Level> dimension) {
        playerDimension = dimension;
    }
    
    @Inject(method = "Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;write(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void onWrite(FriendlyByteBuf buf, CallbackInfo ci) {
        DimId.writeWorldId(buf, playerDimension, false);
    }
}
