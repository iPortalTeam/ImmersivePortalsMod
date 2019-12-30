package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ChunkVisibilityManager {
    //the players and portals are chunk loaders
    public static class ChunkLoader {
        public DimensionalChunkPos center;
        public int radius;
        
        public ChunkLoader(DimensionalChunkPos center, int radius) {
            this.center = center;
            this.radius = radius;
        }
        
        public void foreachChunkPos(Consumer<DimensionalChunkPos> func) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    func.accept(new DimensionalChunkPos(
                        center.dimension,
                        center.x + dx,
                        center.z + dz
                    ));
                }
            }
        }
        
        @Override
        public String toString() {
            return "{" +
                "center=" + center +
                ", radius=" + radius +
                '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkLoader that = (ChunkLoader) o;
            return radius == that.radius &&
                center.equals(that.center);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(center, radius);
        }
    }
    
    private static int getChebyshevDistance(
        Entity a, Entity b
    ) {
        return Math.max(
            Math.abs(a.chunkX - b.chunkX),
            Math.abs(a.chunkZ - b.chunkZ)
        );
    }
    
    private static ChunkLoader playerDirectLoader(ServerPlayerEntity player) {
        return new ChunkLoader(
            new DimensionalChunkPos(
                player.dimension,
                player.chunkX, player.chunkZ
            ),
            getRenderDistanceOnServer()
        );
    }
    
    private static ChunkLoader portalDirectLoader(
        Portal portal,
        ServerPlayerEntity player
    ) {
        int renderDistance = getRenderDistanceOnServer();
        int distanceToPortal = getChebyshevDistance(portal, player);
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.destination))
            ),
            Math.max(
                1,
                (portal.loadFewerChunks ? (renderDistance / 2) : renderDistance) - distanceToPortal
//                renderDistance - getChebyshevDistance(portal, player)
//                    - (portal.loadFewerChunks ? 3 : 0)
            )
        );
    }
    
    private static ChunkLoader portalIndirectLoader(Portal portal) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.destination))
            ),
            (renderDistance / 3)
        );
    }
    
    private static ChunkLoader globalPortalDirectLoader(
        ServerPlayerEntity player,
        GlobalTrackedPortal portal
    ) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(
                    portal.applyTransformationToPoint(player.getPos())
                ))
            ),
            renderDistance
        );
    }
    
    private static ChunkLoader globalPortalIndirectLoader(
        ServerPlayerEntity player,
        GlobalTrackedPortal outerPortal,
        GlobalTrackedPortal remotePortal
    ) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                remotePortal.dimensionTo,
                new ChunkPos(new BlockPos(
                    remotePortal.applyTransformationToPoint(
                        outerPortal.applyTransformationToPoint(player.getPos())
                    )
                ))
            ),
            renderDistance
        );
    }
    
    private static Stream<GlobalTrackedPortal> getGlobalPortals(
        DimensionType dimension
    ) {
        return GlobalPortalStorage.get(
            McHelper.getServer().getWorld(dimension)
        ).data.stream();
    }
    
    //includes:
    //1.player direct loader
    //2.portal direct loader
    //3.portal secondary loader
    //4.global portal direct loader
    //5.global portal secondary loader
    public static Stream<ChunkLoader> getChunkLoaders(
        ServerPlayerEntity player
    ) {
        return Streams.concat(
            Stream.of(playerDirectLoader(player)),
            
            McHelper.getEntitiesNearby(
                player,
                Portal.class,
                ChunkTrackingGraph.portalLoadingRange
            ).filter(
                portal -> portal.canBeSeenByPlayer(player)
            ).flatMap(
                portal -> Streams.concat(
                    Stream.of(portalDirectLoader(portal, player)),
                    
                    McHelper.getEntitiesNearby(
                        McHelper.getServer().getWorld(portal.dimensionTo),
                        portal.destination,
                        Portal.class,
                        ChunkTrackingGraph.secondaryPortalLoadingRange
                    ).filter(
                        remotePortal -> remotePortal.canBeSeenByPlayer(player)
                    ).map(
                        remotePortal -> portalIndirectLoader(remotePortal)
                    )
                )
            ),
            
            getGlobalPortals(player.dimension)
                .filter(portal ->
                    portal.getDistanceToNearestPointInPortal(player.getPos()) < 128
                )
                .flatMap(
                    portal -> Streams.concat(
                        Stream.of(globalPortalDirectLoader(
                            player, portal
                        )),
                        
                        getGlobalPortals(
                            portal.dimensionTo
                        ).filter(
                            remotePortal ->
                                remotePortal.getDistanceToNearestPointInPortal(
                                    portal.applyTransformationToPoint(player.getPos())
                                ) < 64
                        ).map(
                            remotePortal -> globalPortalIndirectLoader(
                                player, portal, remotePortal
                            )
                        )
                    )
                )
        ).distinct();
    }
    
    public static Set<DimensionalChunkPos> getPlayerViewingChunksNew(
        ServerPlayerEntity player
    ) {
        HashSet<DimensionalChunkPos> chunks = new HashSet<>();
        getChunkLoaders(player)
            .forEach(
                loader -> loader.foreachChunkPos(chunks::add)
            );
        return chunks;
    }
    
    public static int getRenderDistanceOnServer() {
        return McHelper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
}
