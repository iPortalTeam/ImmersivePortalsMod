package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.q_misc_util.platform_specific.MiscNetworking;

@Mixin(PlayerList.class)
public class MixinPlayerList_Misc {
    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;<init>(IZLnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;Ljava/util/Set;Lnet/minecraft/core/RegistryAccess$Frozen;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceKey;JIIIZZZZLjava/util/Optional;)V"
        )
    )
    private void onConnectionEstablished(
        Connection connection,
        ServerPlayer player,
        CallbackInfo ci
    ) {
        player.connection.send(MiscNetworking.createDimSyncPacket());
    }
}
