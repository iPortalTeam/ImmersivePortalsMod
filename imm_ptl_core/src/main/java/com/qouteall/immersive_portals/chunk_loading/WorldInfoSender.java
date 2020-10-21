package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorldInfoSender {
    public static void init() {
        ModMain.postServerTickSignal.connect(() -> {
            if (McHelper.getServerGameTime() % 100 == 42) {
                for (ServerPlayerEntity player : McHelper.getCopiedPlayerList()) {
                    Set<RegistryKey<World>> visibleDimensions = getVisibleDimensions(player);
                    
                    if (player.world.getRegistryKey() != World.OVERWORLD) {
                        sendWorldInfo(
                            player,
                            McHelper.getServer().getWorld(World.OVERWORLD)
                        );
                    }
                    
                    McHelper.getServer().getWorlds().forEach(thisWorld -> {
                        if (ModMain.isAlternateDimension(thisWorld)) {
                            if (visibleDimensions.contains(thisWorld.getRegistryKey())) {
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
        RegistryKey<World> remoteDimension = world.getRegistryKey();
        
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
            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
                world.getRegistryKey(),
                new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_STARTED,
                    0.0F
                )
            ));
            
            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
                world.getRegistryKey(),
                new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED,
                    world.getRainGradient(1.0F)
                )
            ));
            player.networkHandler.sendPacket(MyNetwork.createRedirectedMessage(
                world.getRegistryKey(),
                new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED,
                    world.getThunderGradient(1.0F)
                )
            ));
        }
        else {
            //if the weather is already not raining when the player logs in then no need to sync
            //if the weather turned to not raining then elsewhere syncs it
        }
    }
    
    public static Set<RegistryKey<World>> getVisibleDimensions(ServerPlayerEntity player) {
        return Stream.concat(
            ChunkVisibilityManager.getChunkLoaders(player)
                .map(chunkLoader -> chunkLoader.center.dimension),
            Optional.of(McHelper.getGlobalPortals(player.world))
                .map(p ->
                    p.stream().map(
                        p1 -> p1.dimensionTo
                    )
                ).orElse(Stream.empty())
        ).collect(Collectors.toSet());
    }
}
