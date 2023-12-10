package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.MiscNetworking;

@Mixin(PlayerList.class)
public class MixinPlayerList_Misc {
    @Inject(
        method = "placeNewPlayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundChangeDifficultyPacket;<init>(Lnet/minecraft/world/Difficulty;Z)V"
        )
    )
    private void onConnectionEstablished(
        Connection connection,
        ServerPlayer player,
        CommonListenerCookie commonListenerCookie,
        CallbackInfo ci
    ) {
        player.connection.send(
            MiscNetworking.DimIdSyncPacket.createPacket(player.server)
        );
    }
}
