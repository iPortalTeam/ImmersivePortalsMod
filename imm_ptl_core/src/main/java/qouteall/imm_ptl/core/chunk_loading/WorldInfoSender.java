package qouteall.imm_ptl.core.chunk_loading;

import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.network.PacketRedirection;
import qouteall.q_misc_util.MiscHelper;

import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

public class WorldInfoSender {
    public static void init() {
        IPGlobal.postServerTickSignal.connect(() -> {
            MiscHelper.getServer().getProfiler().push("portal_send_world_info");
            if (McHelper.getServerGameTime() % 100 == 42) {
                for (ServerPlayer player : McHelper.getCopiedPlayerList()) {
                    Set<ResourceKey<Level>> visibleDimensions = NewChunkTrackingGraph.getVisibleDimensions(player);
                    
                    if (player.level().dimension() != Level.OVERWORLD) {
                        sendWorldInfo(
                            player,
                            MiscHelper.getServer().getLevel(Level.OVERWORLD)
                        );
                    }
                    
                    MiscHelper.getServer().getAllLevels().forEach(thisWorld -> {
                        if (isNonOverworldSurfaceDimension(thisWorld)) {
                            if (visibleDimensions.contains(thisWorld.dimension())) {
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
    public static void sendWorldInfo(ServerPlayer player, ServerLevel world) {
        ResourceKey<Level> remoteDimension = world.dimension();
        
        PacketRedirection.sendRedirectedMessage(
            player,
            remoteDimension,
            new ClientboundSetTimePacket(
                world.getGameTime(),
                world.getDayTime(),
                world.getGameRules().getBoolean(
                    GameRules.RULE_DAYLIGHT
                )
            )
        );
        
        /**{@link net.minecraft.client.network.ClientPlayNetworkHandler#onGameStateChange(GameStateChangeS2CPacket)}*/
        
        if (world.isRaining()) {
            PacketRedirection.sendRedirectedMessage(
                player,
                world.dimension(),
                new ClientboundGameEventPacket(
                    ClientboundGameEventPacket.START_RAINING,
                    0.0F
                )
            );
        }
        else {
            //if the weather is already not raining when the player logs in then no need to sync
            //if the weather turned to not raining then elsewhere syncs it
        }
        
        PacketRedirection.sendRedirectedMessage(
            player,
            world.dimension(),
            new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                world.getRainLevel(1.0F)
            )
        );
        PacketRedirection.sendRedirectedMessage(
            player,
            world.dimension(),
            new ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                world.getThunderLevel(1.0F)
            )
        );
    }
    
    public static boolean isNonOverworldSurfaceDimension(Level world) {
        return world.dimensionType().hasSkyLight() && world.dimension() != Level.OVERWORLD;
    }
}
