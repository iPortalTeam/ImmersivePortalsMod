package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.dimension.DimensionType;

import java.util.Random;

//import com.sun.istack.internal.NotNull;
//import com.sun.istack.internal.Nullable;

public class NetherPortalGenerator {
    public final static int randomShiftFactor = 20;
    
    
    public static BlockPos getRandomShift() {
        Random rand = new Random();
        return new BlockPos(
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor,
            (rand.nextDouble() * 2 - 1) * randomShiftFactor
        );
    }
    
    public static IntegerAABBInclusive findAirCubePlacement(
        ServerWorld toWorld,
        BlockPos mappedPosInOtherDimension,
        IntegerAABBInclusive heightLimit,
        Direction.Axis axis,
        BlockPos neededAreaSize,
        int findingRadius
    ) {
        IntegerAABBInclusive foundAirCube =
            axis == Direction.Axis.Y ?
                NetherPortalMatcher.findHorizontalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension,
                    heightLimit, findingRadius
                ) :
                NetherPortalMatcher.findVerticalPortalPlacement(
                    neededAreaSize, toWorld, mappedPosInOtherDimension,
                    heightLimit, findingRadius
                );
        
        if (foundAirCube == null) {
            Helper.log("Cannot find normal portal placement");
            foundAirCube = NetherPortalMatcher.findCubeAirAreaAtAnywhere(
                neededAreaSize,
                toWorld,
                mappedPosInOtherDimension, heightLimit, findingRadius
            );
        }
        
        if (foundAirCube == null) {
            Helper.err("No place to put portal? " +
                "Force placed portal. It will occupy normal blocks.");
            
            foundAirCube = IntegerAABBInclusive.getBoxByBasePointAndSize(
                neededAreaSize,
                mappedPosInOtherDimension
            );
        }
        return foundAirCube;
    }
    
    public static DimensionType getDestinationDimension(
        DimensionType fromDimension
    ) {
        if (fromDimension == DimensionType.THE_NETHER) {
            return DimensionType.OVERWORLD;
        }
        else if (fromDimension == DimensionType.THE_END) {
            return null;
        }
        else {
            //you can access nether in any other dimension
            //including alternate dimensions
            return DimensionType.THE_NETHER;
        }
    }
    
    public static BlockPos mapPosition(
        BlockPos pos,
        DimensionType dimensionFrom,
        DimensionType dimensionTo
    ) {
        if (dimensionFrom == DimensionType.OVERWORLD && dimensionTo == DimensionType.THE_NETHER) {
            return new BlockPos(
                pos.getX() / 8,
                pos.getY(),
                pos.getZ() / 8
            );
        }
        else if (dimensionFrom == DimensionType.THE_NETHER && dimensionTo == DimensionType.OVERWORLD) {
            return new BlockPos(
                pos.getX() * 8,
                pos.getY(),
                pos.getZ() * 8
            );
        }
        else {
            return pos;
        }
    }
    
    public static void setPortalContentBlock(
        ServerWorld world,
        BlockPos pos,
        Direction.Axis normalAxis
    ) {
        world.setBlockState(
            pos,
            PortalPlaceholderBlock.instance.getDefaultState().with(
                PortalPlaceholderBlock.AXIS, normalAxis
            ),
            3
        );
    }
    
}
