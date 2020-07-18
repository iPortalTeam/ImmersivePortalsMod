package com.qouteall.immersive_portals.mixin;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Shadow
    @Final
    private List<ServerPlayerEntity> players;
    
    @Shadow
    @Final
    private MinecraftServer server;
    
    @Inject(
        method = "onPlayerConnect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;<init>(ILnet/minecraft/world/GameMode;Lnet/minecraft/world/GameMode;JZLjava/util/Set;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Lnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/util/registry/RegistryKey;IIZZZZ)V"
        )
    )
    private void onConnectionEstablished(
        ClientConnection connection,
        ServerPlayerEntity player,
        CallbackInfo ci
    ) {
        player.networkHandler.sendPacket(MyNetwork.createDimSync());
    }
    
    @Inject(method = "sendWorldInfo", at = @At("RETURN"))
    private void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo ci) {
        if (!Global.serverTeleportationManager.isFiringMyChangeDimensionEvent) {
            GlobalPortalStorage.onPlayerLoggedIn(player);
        }
    }
    
    //sometimes the server side player dimension is not same as client
    //so redirect it
    @Inject(
        method = "sendToDimension",
        at = @At("HEAD"),
        cancellable = true
    )
    public void sendToDimension(Packet<?> packet, RegistryKey<World> dimension, CallbackInfo ci) {
        players.stream()
            .filter(player -> player.world.getRegistryKey() == dimension)
            .forEach(player -> player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    dimension,
                    packet
                )
            ));
        ci.cancel();
    }
    
//    /**
//     * @author qouteall
//     */
//    @Overwrite
//    public void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
//        WorldBorder worldBorder = this.server.getOverworld().getWorldBorder();
//        RegistryKey<World> dimension = world.getRegistryKey();
//        player.networkHandler.sendPacket(
//            MyNetwork.createRedirectedMessage(
//                dimension,
//                new WorldBorderS2CPacket(worldBorder, WorldBorderS2CPacket.Type.INITIALIZE)
//            )
//        );
//        player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//            dimension, new WorldTimeUpdateS2CPacket(
//                world.getTime(),
//                world.getTimeOfDay(),
//                world.getGameRules().getBoolean(
//                    GameRules.DO_DAYLIGHT_CYCLE)
//            ))
//        );
//        player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//            dimension, new PlayerSpawnPositionS2CPacket(world.getSpawnPos())
//        ));
//        if (world.isRaining()) {
//            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//                dimension, new GameStateChangeS2CPacket(
//                    GameStateChangeS2CPacket.RAIN_STARTED,
//                    0.0F
//                )
//            ));
//            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//                dimension, new GameStateChangeS2CPacket(
//                    GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED,
//                    world.getRainGradient(1.0F)
//                )
//            ));
//            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
//                dimension, new GameStateChangeS2CPacket(
//                    GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED,
//                    world.getThunderGradient(1.0F)
//                )
//            ));
//        }
//
//    }
}
