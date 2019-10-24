package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.DimensionType;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class NewNetherPortalGenerator {
    public static class Info {
        DimensionType from;
        DimensionType to;
        NetherPortalShape fromShape;
        NetherPortalShape toShape;
        
        public Info(
            DimensionType from,
            DimensionType to,
            NetherPortalShape fromShape,
            NetherPortalShape toShape
        ) {
            this.from = from;
            this.to = to;
            this.fromShape = fromShape;
            this.toShape = toShape;
        }
    }
    
    //only one nether portal should be generating
    public static final Object lock = new Object();
    
    //return null for not found
    //executed on main server thread
    public static Pair<NetherPortalShape, CompletableFuture<Info>> onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        DimensionType toDimension = NetherPortalGenerator.getDestinationDimension(fromDimension);
        
        if (toDimension == null) return null;
        
        NetherPortalShape foundShape = Arrays.stream(Direction.Axis.values())
            .map(
                axis -> NetherPortalShape.findArea(
                    firePos,
                    axis,
                    blockPos -> NetherPortalMatcher.isAirOrFire(
                        fromWorld, blockPos
                    ),
                    blockPos -> NetherPortalMatcher.isObsidian(
                        fromWorld, blockPos
                    )
                )
            ).filter(
                Objects::nonNull
            ).findFirst().orElse(null);
        
        if (foundShape == null) {
            return null;
        }
        
        //avoid lighting portal again when generating portal
        fillInPlaceHolderBlock(fromWorld, foundShape);
        
        //spawn loading indicator
        
        CompletableFuture<Info> future = startGeneratingPortal(
            fromWorld,
            Helper.getServer().getWorld(toDimension),
            foundShape
        );
        
        return new Pair<>(foundShape, future);
    }
    
    private static void fillInPlaceHolderBlock(
        ServerWorld fromWorld,
        NetherPortalShape netherPortalShape
    ) {
        assert false;
    }
    
    private static CompletableFuture<Info> startGeneratingPortal(
        ServerWorld fromWorld,
        ServerWorld toWorld,
        NetherPortalShape foundShape
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                synchronized (lock) {
                    NetherPortalShape portalPlacement =
                        getPortalPlacementAsync(fromWorld, toWorld, foundShape);
                    Info info = new Info(
                        fromWorld.dimension.getType(),
                        toWorld.dimension.getType(),
                        foundShape,
                        portalPlacement
                    );
                    if (portalPlacement != null) {
                        Helper.getServer().execute(() -> {
                            finishGeneratingPortal(info);
                        });
                    }
                    return info;
                }
            },
            Helper.getServer().getWorkerExecutor()
        );
    }
    
    private static boolean recheckTheFrameThatIsBeingLighted(
        ServerWorld fromWorld,
        NetherPortalShape foundShape
    ) {
        return foundShape.isPortalIntact(
            blockPos -> {
                Block block = fromWorld.getBlockState(blockPos).getBlock();
                return block == Blocks.AIR ||
                    block == Blocks.FIRE ||
                    block == PortalPlaceholderBlock.instance;
            },
            blockPos -> NetherPortalMatcher.isObsidian(fromWorld, blockPos)
        );
    }
    
    //executed on server worker thread
    //return null for failed
    private static NetherPortalShape getPortalPlacementAsync(
        ServerWorld fromWorld,
        ServerWorld toWorld,
        NetherPortalShape fromShape
    ) {
        if (!recheckTheFrameThatIsBeingLighted(fromWorld, fromShape)) {
            Helper.log(
                "Nether Portal Generation Aborted." +
                    "This Could Be Caused By Breaking The Portal After Generation Started"
            );
            return null;
        }
        
        BlockPos fromPos = fromShape.areaBox.getCenter();
        
        BlockPos toPos = NetherPortalGenerator.getPosInOtherDimension(
            fromPos,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
        
        IntegerAABBInclusive toWorldHeightLimit =
            NetherPortalMatcher.getHeightLimit(toWorld.dimension.getType());
        
        NetherPortalShape toFrame = NetherPortalMatcher.fromNearToFarWithinHeightLimit(
            toPos,
            NetherPortalMatcher.findingRadius,
            toWorldHeightLimit
        ).filter(
            toWorld::isAir
        ).map(
            blockPos -> fromShape.matchShape(
                toWorld::isAir,
                p -> NetherPortalMatcher.isObsidian(toWorld, p),
                blockPos
            )
        ).filter(
            Objects::nonNull
        ).findFirst().orElse(null);
        
        if (toFrame != null) {
            return toFrame;
        }
        
        IntegerAABBInclusive airCubePlacement = NetherPortalGenerator.findAirCubePlacement(
            toWorld,
            toPos,
            toWorldHeightLimit,
            fromShape.axis,
            fromShape.areaBox.getSize()
        );
        
        NetherPortalShape result = fromShape.getShapeWithMovedAnchor(
            airCubePlacement.l.subtract(
                fromShape.areaBox.l
            ).add(fromShape.anchor)
        );
        return result;
    }
    
    //create portal entity and generate obsidian blocks and placeholder blocks
    //the portal blocks will be placed on both sides because the obsidian may break while generating
    //executed on server main thread
    private static void finishGeneratingPortal(
        Info info
    ) {
        //generate blocks
        
        assert false;
    }
}
