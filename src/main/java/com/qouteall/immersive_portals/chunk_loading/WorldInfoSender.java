package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldInfoSender {
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            if (McHelper.getServerGameTime() % 100 == 42) {
                for (ServerPlayerEntity player : McHelper.getCopiedPlayerList()) {
                    Set<DimensionType> visibleDimensions = getVisibleDimensions(player);
                    
                    if (player.dimension != DimensionType.OVERWORLD) {
                        sendWorldInfo(
                            player,
                            McHelper.getServer().getWorld(DimensionType.OVERWORLD)
                        );
                    }
                    
                    McHelper.getServer().getWorlds().forEach(thisWorld -> {
                        if (thisWorld.dimension instanceof AlternateDimension) {
                            if (visibleDimensions.contains(thisWorld.dimension.getType())) {
                                sendWorldInfo(
                                    player,
                                    thisWorld
                                );
                            }
                        }
                    });
                    
                }
            }
        });
    }
    
    //send the daytime and weather info to player when player is in nether
    public static void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
        DimensionType remoteDimension = world.dimension.getType();
        
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
    }
    
    public static Set<DimensionType> getVisibleDimensions(ServerPlayerEntity player) {
        return Stream.concat(
            ChunkVisibilityManager.getChunkLoaders(player)
                .map(chunkLoader -> chunkLoader.center.dimension),
            Optional.ofNullable(McHelper.getGlobalPortals(player.world))
                .map(p ->
                    p.stream().map(
                        p1 -> p1.dimensionTo
                    )
                ).orElse(Stream.empty())
        ).collect(Collectors.toSet());
    }
}
