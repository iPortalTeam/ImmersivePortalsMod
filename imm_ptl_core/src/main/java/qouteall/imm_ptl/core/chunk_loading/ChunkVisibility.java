package qouteall.imm_ptl.core.chunk_loading;

import com.google.common.collect.Streams;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.my_util.LimitedLogger;
import qouteall.imm_ptl.core.portal.Portal;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ChunkVisibility {
    private static final LimitedLogger limitedLogger = new LimitedLogger(50);
    
    private static final int portalLoadingRange = 48;
    public static final int secondaryPortalLoadingRange = 16;
    
    public static ChunkLoader playerDirectLoader(ServerPlayerEntity player) {
        return new ChunkLoader(
            new DimensionalChunkPos(
                player.world.getRegistryKey(),
                player.getChunkPos()
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
        Portal portal, ServerPlayerEntity player, int targetLoadingDistance
    ) {
        int cap = IPGlobal.indirectLoadingRadiusCap;
        
        // load more for scaling portal
        if (portal.getScale() > 2) {
            cap *= 2;
        }
        
        int cappedLoadingDistance = Math.min(targetLoadingDistance, cap);

//        if (!IPGlobal.serverSmoothLoading) {
//            return cappedLoadingDistance;
//        }

//        int maxLoadDistance = PortalExtension.get(portal).refreshAndGetLoadDistanceCap(
//            portal, player, cappedLoadingDistance
//        );
        return cappedLoadingDistance;
    }
    
    public static List<Portal> getNearbyPortals(
        ServerWorld world, Vec3d pos, Predicate<Portal> predicate, boolean isDirect
    ) {
        List<Portal> result = McHelper.findEntitiesRough(
            Portal.class,
            world,
            pos,
            (isDirect ?
                (isShrinkLoading() ? portalLoadingRange / 2 : portalLoadingRange) :
                secondaryPortalLoadingRange)
                / 16,
            predicate
        );
        
        for (Portal globalPortal : GlobalPortalStorage.getGlobalPortals(world)) {
            if (globalPortal.getDistanceToNearestPointInPortal(pos) < (isDirect ? 256 : 32)) {
                result.add(globalPortal);
            }
        }
        
        if (result.size() > 50) {
            limitedLogger.err("too many portal nearby " + world + pos);
            
            Optional<Portal> nearest =
                result.stream().min(Comparator.comparingDouble(p -> p.getDistanceToNearestPointInPortal(pos)));
            
            return List.of(nearest.get());
        }
        
        return result;
    }
    
    private static ChunkLoader getGeneralDirectPortalLoader(
        ServerPlayerEntity player, Portal portal
    ) {
        if (portal.getIsGlobal()) {
            int renderDistance = Math.min(
                IPGlobal.indirectLoadingRadiusCap * 2,
                //load a little more to make dimension stack more complete
                Math.max(
                    2,
                    McHelper.getRenderDistanceOnServer() -
                        Math.floorDiv((int) portal.getDistanceToNearestPointInPortal(player.getPos()), 16)
                )
            );
            
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(new BlockPos(
                        portal.transformPoint(player.getPos())
                    ))
                ),
                renderDistance
            );
        }
        else {
            int renderDistance = McHelper.getRenderDistanceOnServer();
            double distance = portal.getDistanceToNearestPointInPortal(player.getPos());
            
            // load more for up scaling portal
            if (portal.scaling > 2 && distance < 5) {
                renderDistance = (int) ((portal.getDestAreaRadiusEstimation() * 1.4) / 16);
            }
            
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(new BlockPos(portal.getDestPos()))
                ),
                getCappedLoadingDistance(
                    portal, player,
                    getDirectLoadingDistance(renderDistance, distance)
                )
            );
        }
    }
    
    private static ChunkLoader getGeneralPortalIndirectLoader(
        ServerPlayerEntity player,
        Vec3d transformedPos,
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
                    new ChunkPos(new BlockPos(transformedPos))
                ),
                renderDistance
            );
        }
        else {
            return new ChunkLoader(
                new DimensionalChunkPos(
                    portal.dimensionTo,
                    new ChunkPos(new BlockPos(portal.getDestPos()))
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
        ServerPlayerEntity player
    ) {
        ChunkLoader playerDirectLoader = playerDirectLoader(player);
        
        return Streams.concat(
            Stream.of(playerDirectLoader),
            
            getNearbyPortals(
                ((ServerWorld) player.world),
                player.getPos(),
                portal -> portal.canBeSpectated(player),
                true
            ).stream().flatMap(
                portal -> {
                    Vec3d transformedPlayerPos = portal.transformPoint(player.getPos());
                    
                    World destinationWorld = portal.getDestinationWorld();
                    
                    if (destinationWorld == null) {
                        return Stream.empty();
                    }
                    
                    return Stream.concat(
                        Stream.of(getGeneralDirectPortalLoader(player, portal)),
                        isShrinkLoading() ?
                            Stream.empty() :
                            getNearbyPortals(
                                ((ServerWorld) destinationWorld),
                                transformedPlayerPos,
                                p -> p.canBeSpectated(player),
                                false
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
        return IPGlobal.indirectLoadingRadiusCap < 4;
    }
    
}
