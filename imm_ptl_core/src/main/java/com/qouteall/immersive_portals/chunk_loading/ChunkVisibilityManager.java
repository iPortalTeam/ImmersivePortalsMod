package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalExtension;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ChunkVisibilityManager {
    private static final int portalLoadingRange = 48;
    public static final int secondaryPortalLoadingRange = 16;
    
    public static interface ChunkPosConsumer {
        void consume(RegistryKey<World> dimensionType, int x, int z, int distanceToSource);
    }
    
    public static ChunkLoader playerDirectLoader(ServerPlayerEntity player) {
        return new ChunkLoader(
            new DimensionalChunkPos(
                player.world.getRegistryKey(),
                player.chunkX, player.chunkZ
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
    
    private static int getSmoothedLoadingDistance(
        Portal portal, ServerPlayerEntity player, int targetLoadingDistance
    ) {
        int cap = Global.indirectLoadingRadiusCap;
        
        // load more for scaling portal
        if (portal.scaling > 2) {
            cap *= 2;
        }
        
        int cappedLoadingDistance = Math.min(targetLoadingDistance, cap);
        
        if (!Global.serverSmoothLoading) {
            return cappedLoadingDistance;
        }
        
        int maxLoadDistance = PortalExtension.get(portal).refreshAndGetLoadDistanceCap(
            portal, player, cappedLoadingDistance
        );
        return Math.min(maxLoadDistance, cappedLoadingDistance);
    }
    
    @Deprecated
    private static ChunkLoader portalDirectLoader(
        Portal portal,
        ServerPlayerEntity player
    ) {
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
            getSmoothedLoadingDistance(
                portal, player,
                getDirectLoadingDistance(renderDistance, distance)
            )
        );
    }
    
    @Deprecated
    private static ChunkLoader portalIndirectLoader(Portal portal, ServerPlayerEntity player) {
        int renderDistance = McHelper.getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.getDestPos()))
            ),
            getSmoothedLoadingDistance(
                portal, player, renderDistance / 4
            )
        );
    }
    
    @Deprecated
    private static ChunkLoader globalPortalDirectLoader(
        ServerPlayerEntity player,
        Portal portal
    ) {
        Validate.isTrue(portal.getIsGlobal());
        
        int renderDistance = Math.min(
            Global.indirectLoadingRadiusCap + (Global.indirectLoadingRadiusCap / 2),
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
    
    @Deprecated
    private static ChunkLoader globalPortalIndirectLoader(
        ServerPlayerEntity player,
        Portal outerPortal,
        Portal remotePortal
    ) {
        Validate.isTrue(outerPortal.getIsGlobal());
        Validate.isTrue(remotePortal.getIsGlobal());
        
        int renderDistance = Math.min(
            Global.indirectLoadingRadiusCap,
            McHelper.getRenderDistanceOnServer() / 2
        );
        return new ChunkLoader(
            new DimensionalChunkPos(
                remotePortal.dimensionTo,
                new ChunkPos(new BlockPos(
                    remotePortal.transformPoint(
                        outerPortal.transformPoint(player.getPos())
                    )
                ))
            ),
            renderDistance
        );
    }
    
    public static List<Portal> getNearbyPortals(
        ServerWorld world, Vec3d pos, Predicate<Portal> predicate, int range
    ) {
        List<Portal> result = McHelper.findEntitiesRough(
            Portal.class,
            world,
            pos,
            range / 16,
            predicate
        );
        
        for (Portal globalPortal : McHelper.getGlobalPortals(world)) {
            if (globalPortal.getDistanceToNearestPointInPortal(pos) < range * 2) {
                result.add(globalPortal);
            }
        }
        
        return result;
    }
    
    private static ChunkLoader getGeneralDirectPortalLoader(
        ServerPlayerEntity player, Portal portal
    ) {
        if (portal.getIsGlobal()) {
            int renderDistance = Math.min(
                Global.indirectLoadingRadiusCap + (Global.indirectLoadingRadiusCap / 2),
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
                getSmoothedLoadingDistance(
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
                Global.indirectLoadingRadiusCap,
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
                getSmoothedLoadingDistance(
                    portal, player, serverLoadingDistance / 4
                )
            );
        }
    }
    
    //includes:
    //1.player direct loader
    //2.portal direct loader
    //3.portal secondary loader
    //4.global portal direct loader
    //5.global portal secondary loader
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
                shrinkLoading() ? portalLoadingRange / 2 : portalLoadingRange
            ).stream().flatMap(
                portal -> {
                    Vec3d transformedPlayerPos = portal.transformPoint(player.getPos());
    
                    return Stream.concat(
                        Stream.of(getGeneralDirectPortalLoader(player, portal)),
                        shrinkLoading() ?
                            Stream.empty() :
                            getNearbyPortals(
                                ((ServerWorld) portal.getDestinationWorld()),
                                transformedPlayerPos,
                                p -> p.canBeSpectated(player),
                                secondaryPortalLoadingRange
                            ).stream().map(
                                innerPortal -> getGeneralPortalIndirectLoader(
                                    player, transformedPlayerPos, innerPortal
                                )
                            )
                    );
                }
            )
        );

//        return Streams.concat(
//            Stream.of(playerDirectLoader),
//
//            McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
//                player.world,
//                player.getPos(),
//                Portal.class,
//                shrinkLoading() ? portalLoadingRange / 2 : portalLoadingRange
//            ).filter(
//                portal -> portal.canBeSpectated(player)
//            ).flatMap(
//                portal -> Stream.concat(
//                    Stream.of(portalDirectLoader(portal, player)),
//
//                    shrinkLoading() ?
//                        Stream.empty() :
//                        McHelper.getServerEntitiesNearbyWithoutLoadingChunk(
//                            McHelper.getServer().getWorld(portal.dimensionTo),
//                            portal.getDestPos(),
//                            Portal.class,
//                            secondaryPortalLoadingRange
//                        ).filter(
//                            remotePortal -> remotePortal.canBeSpectated(player)
//                        ).map(
//                            remotePortal -> portalIndirectLoader(remotePortal, player)
//                        )
//                )
//            ),
//
//            McHelper.getGlobalPortals(player.world).stream()
//                .flatMap(
//                    portal -> Stream.concat(
//                        Stream.of(globalPortalDirectLoader(
//                            player, portal
//                        )),
//
//                        shrinkLoading() ?
//                            Stream.empty() :
//                            McHelper.getGlobalPortals(portal.getDestinationWorld()).stream()
//                                .filter(
//                                    remotePortal -> remotePortal.getDistanceToNearestPointInPortal(
//                                        portal.transformPoint(player.getPos())
//                                    ) < (shrinkLoading() ? portalLoadingRange / 2 : portalLoadingRange)
//                                ).map(
//                                remotePortal -> globalPortalIndirectLoader(
//                                    player, portal, remotePortal
//                                )
//                            )
//                    )
//                )
//        ).distinct();
    }
    
    public static boolean shrinkLoading() {
        return Global.indirectLoadingRadiusCap < 4;
    }
    
}
