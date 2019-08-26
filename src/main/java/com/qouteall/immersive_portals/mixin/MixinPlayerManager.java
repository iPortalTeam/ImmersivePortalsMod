package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.network.packet.GameStateChangeS2CPacket;
import net.minecraft.client.network.packet.WorldTimeUpdateS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(
        method = "respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/world/dimension/DimensionType;Z)Lnet/minecraft/server/network/ServerPlayerEntity;",
        at = @At("HEAD")
    )
    private void onPlayerRespawn(
        ServerPlayerEntity oldPlayer,
        DimensionType dimensionType_1,
        boolean boolean_1,
        CallbackInfoReturnable<ServerPlayerEntity> cir
    ) {
        SGlobal.chunkDataSyncManager.onPlayerRespawn(oldPlayer);
    }
    
    //send the daytime to player when player is in nether
    @Inject(method = "sendWorldInfo", at = @At("TAIL"))
    private void onSendWorldInfo(
        ServerPlayerEntity player,
        ServerWorld playerWorld,
        CallbackInfo ci
    ) {
        Helper.getServer().getWorlds().forEach(remoteWorld -> {
            if (remoteWorld != playerWorld) {
                sendRemoveWorldInfo(player, remoteWorld);
            }
        });
    }
    
    private void sendRemoveWorldInfo(ServerPlayerEntity player, ServerWorld remoteWorld) {
        DimensionType remoteDimension = remoteWorld.dimension.getType();
        player.networkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                remoteDimension,
                new WorldTimeUpdateS2CPacket(
                    remoteWorld.getTime(),
                    remoteWorld.getTimeOfDay(),
                    remoteWorld.getGameRules().getBoolean(
                        GameRules.DO_DAYLIGHT_CYCLE
                    )
                )
            )
        );
        if (remoteWorld.isRaining()) {
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(1, 0.0F)
                )
            );
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(7, remoteWorld.getRainGradient(1.0F))
                )
            );
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(8, remoteWorld.getThunderGradient(1.0F))
                )
            );
        }
    }
}
