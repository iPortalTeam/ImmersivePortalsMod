package qouteall.imm_ptl.core.chunk_loading;

import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.platform_specific.IPNetworking;
import qouteall.q_misc_util.MiscHelper;

import java.util.Set;

public class WorldInfoSender {
    public static void init() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MiscHelper.getServer().getProfiler().push("portal_send_world_info");
            if (McHelper.getServerGameTime() % 100 == 42) {
                for (ServerPlayerEntity player : McHelper.getCopiedPlayerList()) {
                    Set<RegistryKey<World>> visibleDimensions = NewChunkTrackingGraph.getVisibleDimensions(player);
                    
                    if (player.world.getRegistryKey() != World.OVERWORLD) {
                        sendWorldInfo(
                            player,
                            MiscHelper.getServer().getWorld(World.OVERWORLD)
                        );
                    }
                    
                    MiscHelper.getServer().getWorlds().forEach(thisWorld -> {
                        if (isNonOverworldSurfaceDimension(thisWorld)) {
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
            MiscHelper.getServer().getProfiler().pop();
        });
    }
    
    //send the daytime and weather info to player when player is in nether
    public static void sendWorldInfo(ServerPlayerEntity player, ServerWorld world) {
        RegistryKey<World> remoteDimension = world.getRegistryKey();
        
        player.networkHandler.sendPacket(
            IPNetworking.createRedirectedMessage(
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
            player.networkHandler.sendPacket(IPNetworking.createRedirectedMessage(
                world.getRegistryKey(),
                new GameStateChangeS2CPacket(
                    GameStateChangeS2CPacket.RAIN_STARTED,
                    0.0F
                )
            ));
        }
        else {
            //if the weather is already not raining when the player logs in then no need to sync
            //if the weather turned to not raining then elsewhere syncs it
        }
        
        player.networkHandler.sendPacket(IPNetworking.createRedirectedMessage(
            world.getRegistryKey(),
            new GameStateChangeS2CPacket(
                GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED,
                world.getRainGradient(1.0F)
            )
        ));
        player.networkHandler.sendPacket(IPNetworking.createRedirectedMessage(
            world.getRegistryKey(),
            new GameStateChangeS2CPacket(
                GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED,
                world.getThunderGradient(1.0F)
            )
        ));
    }
    
    public static boolean isNonOverworldSurfaceDimension(World world) {
        return world.getDimension().hasSkyLight() && world.getRegistryKey() != World.OVERWORLD;
    }
}
