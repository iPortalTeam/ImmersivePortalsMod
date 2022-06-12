package qouteall.imm_ptl.core.platform_specific.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qouteall.imm_ptl.core.IPGlobal;

@Mixin(PlayerList.class)
public class MixinPlayerManager_MA {
    @Inject(
        method = "Lnet/minecraft/server/players/PlayerList;respawn(Lnet/minecraft/server/level/ServerPlayer;Z)Lnet/minecraft/server/level/ServerPlayer;",
        at = @At("HEAD")
    )
    private void onPlayerRespawn(
        ServerPlayer oldPlayer,
        boolean bl,
        CallbackInfoReturnable<ServerPlayer> cir
    ) {
        IPGlobal.chunkDataSyncManager.removePlayerFromChunkTrackersAndEntityTrackers(oldPlayer);
    }
    
    @Inject(
        method = "remove",
        at = @At("HEAD")
    )
    private void onPlayerDisconnect(ServerPlayer player, CallbackInfo ci) {
        IPGlobal.chunkDataSyncManager.removePlayerFromChunkTrackersAndEntityTrackers(player);
    }
}
