package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

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
    
    //return null for not found
    //executed on main server thread
    public static boolean onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        DimensionType toDimension = NetherPortalGenerator.getDestinationDimension(fromDimension);
    
        if (toDimension == null) return false;
        
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
            return false;
        }
    
        NetherPortalShape fromShape = foundShape;
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
    
        BlockPos fromPos = fromShape.innerAreaBox.getCenter();
    
        boolean isOtherGenerationRunning = McHelper.getEntitiesNearby(
            fromWorld, new Vec3d(fromPos), LoadingIndicatorEntity.class, 1
        ).findAny().isPresent();
        if (isOtherGenerationRunning) {
            return false;
        }
    
        BlockPos toPos = NetherPortalGenerator.getPosInOtherDimension(
            fromPos,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
    
        //avoid blockpos object creation
        BlockPos.Mutable temp = new BlockPos.Mutable();
    
        IntegerAABBInclusive toWorldHeightLimit =
            NetherPortalMatcher.getHeightLimit(toWorld.dimension.getType());
    
        Iterator<NetherPortalShape> iterator =
            NetherPortalMatcher.fromNearToFarWithinHeightLimit(
                toPos,
                150,
                toWorldHeightLimit
            ).map(
                blockPos -> {
                    if (!toWorld.isAir(blockPos)) {
                        return null;
                    }
                    return fromShape.matchShape(
                        toWorld::isAir,
                        p -> NetherPortalMatcher.isObsidian(toWorld, p),
                        blockPos,
                        temp
                    );
                }
            ).iterator();
    
        LoadingIndicatorEntity indicatorEntity =
            LoadingIndicatorEntity.entityType.create(fromWorld);
        indicatorEntity.isAlive = true;
        indicatorEntity.setPosition(
            fromPos.getX() + 0.5,
            fromPos.getY() + 0.5,
            fromPos.getZ() + 0.5
        );
        fromWorld.spawnEntity(indicatorEntity);
    
        McHelper.performSplitedFindingTaskOnServer(
            iterator,
            Objects::nonNull,
            (i) -> {
                boolean isIntact = recheckTheFrameThatIsBeingLighted(fromWorld, fromShape);
            
                if ((!isIntact)) {
                    Helper.log("Nether Portal Generation Aborted");
                    indicatorEntity.remove();
                    return false;
                }
            
                double progress = i / 20000000.0;
                indicatorEntity.setText(
                    "Searching for matched obsidian frame on the other side\n" +
                        (int) (progress * 100) + "%"
                );
            
                return true;
            },
            toShape -> {
                finishGeneratingPortal(new Info(
                    fromDimension, toDimension, fromShape, toShape
                ));
            },
            () -> {
                indicatorEntity.setText(
                    "Existing frame could not be found.\n" +
                        "Generating new portal"
                );
            
                ModMain.serverTaskList.addTask(() -> {
                    IntegerAABBInclusive airCubePlacement =
                        NetherPortalGenerator.findAirCubePlacement(
                            toWorld, toPos, toWorldHeightLimit,
                            fromShape.axis, fromShape.totalAreaBox.getSize()
                        );
                
                    NetherPortalShape toShape = fromShape.getShapeWithMovedAnchor(
                        airCubePlacement.l.subtract(
                            fromShape.totalAreaBox.l
                        ).add(fromShape.anchor)
                    );
                
                    toShape.frameAreaWithCorner.forEach(blockPos ->
                        toWorld.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState())
                    );
                
                    finishGeneratingPortal(new Info(
                        fromDimension, toDimension, fromShape, toShape
                    ));
                
                    return true;
                });
            }
        );
    
        return true;
    }
    
    private static void fillInPlaceHolderBlocks(
        ServerWorld fromWorld,
        NetherPortalShape netherPortalShape
    ) {
        netherPortalShape.area.forEach(
            blockPos -> NetherPortalGenerator.setPortalContentBlock(
                fromWorld, blockPos, netherPortalShape.axis
            )
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
    
    //create portal entity and generate obsidian blocks and placeholder blocks
    //the portal blocks will be placed on both sides because the obsidian may break while generating
    //executed on server main thread
    private static void finishGeneratingPortal(
        Info info
    ) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(info.from);
        ServerWorld toWorld = McHelper.getServer().getWorld(info.to);
    
        fillInPlaceHolderBlocks(fromWorld, info.fromShape);
        fillInPlaceHolderBlocks(toWorld, info.toShape);
    
        NewNetherPortalEntity[] portalArray = new NewNetherPortalEntity[]{
            NewNetherPortalEntity.entityType.create(fromWorld),
            NewNetherPortalEntity.entityType.create(fromWorld),
            NewNetherPortalEntity.entityType.create(toWorld),
            NewNetherPortalEntity.entityType.create(toWorld)
        };
    
        info.fromShape.initPortalPosAxisShape(
            portalArray[0], false
        );
        info.fromShape.initPortalPosAxisShape(
            portalArray[1], true
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[2], false
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[3], true
        );
    
        portalArray[0].dimensionTo = info.to;
        portalArray[1].dimensionTo = info.to;
        portalArray[2].dimensionTo = info.from;
        portalArray[3].dimensionTo = info.from;
    
        Vec3d offset = new Vec3d(info.toShape.innerAreaBox.l.subtract(
            info.fromShape.innerAreaBox.l
        ));
        portalArray[0].destination = portalArray[0].getPos().add(offset);
        portalArray[1].destination = portalArray[1].getPos().add(offset);
        portalArray[2].destination = portalArray[2].getPos().subtract(offset);
        portalArray[3].destination = portalArray[3].getPos().subtract(offset);
    
        portalArray[0].netherPortalShape = info.fromShape;
        portalArray[1].netherPortalShape = info.fromShape;
        portalArray[2].netherPortalShape = info.toShape;
        portalArray[3].netherPortalShape = info.toShape;
    
        portalArray[0].reversePortalId = portalArray[2].getUuid();
        portalArray[1].reversePortalId = portalArray[3].getUuid();
        portalArray[2].reversePortalId = portalArray[0].getUuid();
        portalArray[3].reversePortalId = portalArray[1].getUuid();
    
        fromWorld.spawnEntity(portalArray[0]);
        fromWorld.spawnEntity(portalArray[1]);
        toWorld.spawnEntity(portalArray[2]);
        toWorld.spawnEntity(portalArray[3]);
    }
}
