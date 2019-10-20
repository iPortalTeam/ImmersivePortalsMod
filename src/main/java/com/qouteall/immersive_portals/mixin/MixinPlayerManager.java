package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.SGlobal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
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
    
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onOnPlayerConnect(
        ClientConnection clientConnection_1,
        ServerPlayerEntity serverPlayerEntity_1,
        CallbackInfo ci
    ) {
        GlobalPortalStorage.onPlayerLoggedIn(serverPlayerEntity_1);
    }
}
