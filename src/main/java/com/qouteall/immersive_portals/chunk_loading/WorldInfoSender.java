package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.network.packet.GameStateChangeS2CPacket;
import net.minecraft.client.network.packet.WorldTimeUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;

public class WorldInfoSender {
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            if (Helper.getServerGameTime() % 100 == 42) {
                for (ServerPlayerEntity player : Helper.getCopiedPlayerList()) {
                    for (ServerWorld world : Helper.getServer().getWorlds()) {
                        sendWorldInfo(player, world);
                    }
                }
            }
        });
    }
    
    //send the daytime and weather info to player when player is in nether
    public static void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
        DimensionType remoteDimension = world.dimension.getType();
        if (remoteDimension == DimensionType.THE_NETHER || remoteDimension == DimensionType.THE_END) {
            return;
        }
        
        player.networkHandler.sendPacket(
            MyNetwork.createRedirectedMessage(
                remoteDimension,
                new WorldTimeUpdateS2CPacket(
                    world.getTime(),
                    world.getTimeOfDay(),
                    world.getGameRules().getBoolean(
                        GameRules.DO_DAYLIGHT_CYCLE
                    )
                )
            )
        );
    
        /**{@link net.minecraft.client.network.ClientPlayNetworkHandler#onGameStateChange(GameStateChangeS2CPacket)}*/
        if (world.isRaining()) {
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(1, 0.0F)
                )
            );
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(7, world.getRainGradient(1.0F))
                )
            );
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(8, world.getThunderGradient(1.0F))
                )
            );
        }
        else {
            player.networkHandler.sendPacket(
                MyNetwork.createRedirectedMessage(
                    remoteDimension,
                    new GameStateChangeS2CPacket(2, 0.0F)
                )
            );
        }
    }
}
