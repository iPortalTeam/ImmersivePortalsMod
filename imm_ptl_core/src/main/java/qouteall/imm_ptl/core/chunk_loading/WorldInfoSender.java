package qouteall.imm_ptl.core.chunk_loading;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.network.PacketRedirection;

import java.util.Set;

public class WorldInfoSender {
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            server.getProfiler().push("portal_send_world_info");
            if (McHelper.getServerGameTime() % 100 == 42) {
                for (ServerPlayer player : McHelper.getCopiedPlayerList()) {
                    Set<ResourceKey<Level>> visibleDimensions = ImmPtlChunkTracking.getVisibleDimensions(player);
                    
                    // sync overworld status when the player is not in overworld
                    if (player.level().dimension() != Level.OVERWORLD) {
                        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                        Validate.notNull(overworld, "missing overworld");
                        sendWorldInfo(player, overworld);
                    }
                    
                    server.getAllLevels().forEach(thisWorld -> {
                        if (isNonOverworldSurfaceDimension(thisWorld)) {
                            if (visibleDimensions.contains(thisWorld.dimension())) {
                                sendWorldInfo(player, thisWorld);
                            }
                        }
                    });
                    
                }
            }
            server.getProfiler().pop();
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
