package qouteall.imm_ptl.core.chunk_loading;

import com.google.common.collect.Streams;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.my_util.LimitedLogger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ChunkVisibility {
    private static final LimitedLogger limitedLogger = new LimitedLogger(10);
    
    private static final int portalLoadingRange = 48;
    public static final int secondaryPortalLoadingRange = 16;
    
    public static ChunkLoader playerDirectLoader(ServerPlayer player) {
        return new ChunkLoader(
            new DimensionalChunkPos(
                player.level().dimension(),
                player.chunkPosition()
            ),
            McHelper.getRenderDistanceOnServer(),
            true
        );
    }
    
    private static int getDirectLoadingDistance(int renderDistance, double distanceToPortal) {
        if (distanceToPortal < 5) {
            return renderDistance;
        }
        if (distanceToPortal < 15) {
            return (renderDistance * 2) / 3;
        }
        return renderDistance / 3;
    }
    
    private static int getCappedLoadingDistance(
        Portal portal, ServerPlayer player, int targetLoadingDistance
    ) {
        PerformanceLevel performanceLevel =
            NewChunkTrackingGraph.getPlayerInfo(player).performanceLevel;
        int cap1 = PerformanceLevel.getIndirectLoadingRadiusCap(performanceLevel);
        int cap2 = IPGlobal.indirectLoadingRadiusCap;
        int cap3 = PerformanceLevel.getIndirectLoadingRadiusCap(ServerPerformanceMonitor.getLevel());
        
        int cap = Math.min(cap1, cap2);
        
        // load more for scaling portal
        if (portal.getScale() > 2) {
            cap *= 2;
        }
        
        int cappedLoadingDistance = Math.min(targetLoadingDistance, cap);
        
        return cappedLoadingDistance;
    }
    
    public static List<Portal> getNearbyPortals(
        ServerLevel world, Vec3 pos, Predicate<Portal> predicate,
        int radiusChunks, int radiusChunksForGlobalPortals
    ) {
        List<Portal> result = McHelper.findEntitiesRough(
            Portal.class,
            world,
            pos,
            radiusChunks,
            predicate
        );
        
        for (Portal globalPortal : GlobalPortalStorage.getGlobalPortals(world)) {
            double distance = globalPortal.getDistanceToNearestPointInPortal(pos);
            if (distance < radiusChunksForGlobalPortals * 16) {
                result.add(globalPortal);
            }
        }
        
        if (result.size() > 100) {
            limitedLogger.err("too many portal nearby " + world + pos);
            
            Optional<Portal> nearest =
                result.stream().min(Comparator.comparingDouble(p -> p.getDistanceToNearestPointInPortal(pos)));
            
            return List.of(nearest.get());
        }
        
        return result;
    }
    
    private static ChunkLoader getGeneralDirectPortalLoader(
        ServerPlayer player, Portal portal
    ) {
        if (portal.getIsGlobal()) {
            int renderDistance = Math.min(
                IPGlobal.indirectLoadingRadiusCap * 2,
                //load a little more to make dimension stack more complete
                Math.max(
                    2,
                    McHelper.getRenderDistanceOnServer() -
                        Math.floorDiv((int) portal.getDistanceToNearestPointInPortal(player.position()), 16)
                )
            );
            
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(BlockPos.containing(
                        portal.transformPoint(player.position())
                    ))
                ),
                renderDistance
            );
        }
        else {
            int renderDistance = McHelper.getRenderDistanceOnServer();
            double distance = portal.getDistanceToNearestPointInPortal(player.position());
            
            // load more for up scaling portal
            if (portal.scaling > 2 && distance < 5) {
                renderDistance = (int) ((portal.getDestAreaRadiusEstimation() * 1.4) / 16);
            }
            
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(BlockPos.containing(portal.getDestPos()))
                ),
                getCappedLoadingDistance(
                    portal, player,
                    getDirectLoadingDistance(renderDistance, distance)
                )
            );
        }
    }
    
    private static ChunkLoader getGeneralPortalIndirectLoader(
        ServerPlayer player,
        Vec3 transformedPos,
        Portal portal
    ) {
        int serverLoadingDistance = McHelper.getRenderDistanceOnServer();
        
        if (portal.getIsGlobal()) {
            int renderDistance = Math.min(
                IPGlobal.indirectLoadingRadiusCap,
                serverLoadingDistance / 3
            );
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(BlockPos.containing(transformedPos))
                ),
                renderDistance
            );
        }
        else {
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(BlockPos.containing(portal.getDestPos()))
                ),
                getCappedLoadingDistance(
                    portal, player, serverLoadingDistance / 4
                )
            );
        }
    }
    
    //includes:
    //1.player direct loader
    //2.loaders from the portals that are directly visible
    //3.loaders from the portals that are indirectly visible through portals
    public static Stream<ChunkLoader> getBaseChunkLoaders(
        ServerPlayer player
    ) {
        PerformanceLevel perfLevel = NewChunkTrackingGraph.getPlayerInfo(player).performanceLevel;
        int visiblePortalRangeChunks = PerformanceLevel.getVisiblePortalRangeChunks(perfLevel);
        int indirectVisiblePortalRangeChunks = PerformanceLevel.getIndirectVisiblePortalRangeChunks(perfLevel);
        
        ChunkLoader playerDirectLoader = playerDirectLoader(player);
        
        return Streams.concat(
            Stream.of(playerDirectLoader),
            
            getNearbyPortals(
                ((ServerLevel) player.level()),
                player.position(),
                portal -> portal.broadcastToPlayer(player),
                visiblePortalRangeChunks, 256
            ).stream().flatMap(
                portal -> {
                    Vec3 transformedPlayerPos = portal.transformPoint(player.position());
                    
                    Level destinationWorld = portal.getDestinationWorld();
                    
                    if (destinationWorld == null) {
                        return Stream.empty();
                    }
                    
                    return Stream.concat(
                        Stream.of(getGeneralDirectPortalLoader(player, portal)),
                        isShrinkLoading() ?
                            Stream.empty() :
                            getNearbyPortals(
                                ((ServerLevel) destinationWorld),
                                transformedPlayerPos,
                                p -> p.broadcastToPlayer(player),
                                indirectVisiblePortalRangeChunks, 32
                            ).stream().map(
                                innerPortal -> getGeneralPortalIndirectLoader(
                                    player, transformedPlayerPos, innerPortal
                                )
                            )
                    );
                }
            )
        ).distinct();
    }
    
    public static boolean isShrinkLoading() {
        return ServerPerformanceMonitor.getLevel() != PerformanceLevel.good;
    }
    
}
