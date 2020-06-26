package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.dimension.DimensionType;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//Well it will create millions of short-lived BlockPos objects
//It will generate intense GC pressure
//This is not frequently invoked so I don't want to optimize it
public class NetherPortalMatcher {
    public static Stream<BlockPos> fromNearToFarWithinHeightLimit(
        BlockPos searchingCenter,
        int maxRadius,
        IntBox heightLimit
    ) {
        return IntStream
            .range(0, maxRadius)
            .boxed()
            .flatMap(
                r -> new IntBox(
                    new BlockPos(-r, -r, -r),
                    new BlockPos(r, r, r)
                ).getMoved(
                    searchingCenter
                ).forSixSurfaces(
                    stream -> stream.map(
                        box -> IntBox.getIntersect(box, heightLimit)
                    ).filter(Objects::nonNull)
                )
            );
    }
    
    public static final int maxFrameSize = 40;
    public static final IntBox heightLimitOverworld = new IntBox(
        new BlockPos(Integer.MIN_VALUE, 2, Integer.MIN_VALUE),
        new BlockPos(Integer.MAX_VALUE, 254, Integer.MAX_VALUE)
    );
    public static final IntBox heightLimitNether = new IntBox(
        new BlockPos(Integer.MIN_VALUE, 2, Integer.MIN_VALUE),
        new BlockPos(Integer.MAX_VALUE, 126, Integer.MAX_VALUE)
    );
    
    public static IntBox getHeightLimit(
        World world
    ) {
        return new IntBox(
            new BlockPos(Integer.MIN_VALUE, 2, Integer.MIN_VALUE),
            new BlockPos(Integer.MAX_VALUE, world.getDimensionHeight() - 2, Integer.MAX_VALUE)
        );
    }
    
    public static Stream<BlockPos> forOneDirection(
        BlockPos startPos,
        Direction facing,
        int limit
    ) {
        return IntStream.range(
            0,
            limit
        ).mapToObj(num -> startPos.add(Helper.scale(facing.getVector(), num)));
    }
    
    //@Nullable
    private static IntBox detectStick(
        WorldAccess world,
        BlockPos center,
        Direction.Axis axis,
        Predicate<BlockPos> predicate,
        int minLength
    ) {
        BlockPos lowExtremity = detectStickForOneDirection(
            center,
            Direction.get(
                Direction.AxisDirection.NEGATIVE,
                axis
            ),
            predicate
        );
        if (lowExtremity == null) {
            return null;
        }
        
        BlockPos highExtremity = detectStickForOneDirection(
            center,
            Direction.get(
                Direction.AxisDirection.POSITIVE,
                axis
            ),
            predicate
        );
        if (highExtremity == null) {
            return null;
        }
        
        int stickLength = Math.abs(
            Helper.getCoordinate(lowExtremity, axis) - Helper.getCoordinate(highExtremity, axis)
        );
        if (stickLength < minLength) {
            return null;
        }
        return new IntBox(lowExtremity, highExtremity);
    }
    
    //@Nullable
    private static BlockPos detectStickForOneDirection(
        BlockPos startPos,
        Direction facing,
        Predicate<BlockPos> predicate
    ) {
        return Helper.getLastSatisfying(
            forOneDirection(
                startPos,
                facing,
                maxFrameSize
            ),
            predicate
        );
    }
    
    private static boolean isAir(WorldAccess world, BlockPos pos) {
        return world.isAir(pos);
    }
    
    public static boolean isAirOrFire(WorldAccess world, BlockPos pos) {
        return world.isAir(pos) || world.getBlockState(pos).getBlock() == Blocks.FIRE;
    }
    
    //------------------------------------------------------------
    //detect air cube on ground
    
    static IntBox findVerticalPortalPlacement(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter,
        int findingRadius
    ) {
        IntBox airCube = getAirCubeOnSolidGround(
            areaSize.add(5, 0, 5), world, searchingCenter,
            findingRadius / 8 - 5
        );
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Ground");
            return null;
        }
        
        if (world.getBlockState(airCube.l.add(0, -1, 0)).getBlock() == Blocks.LAVA) {
            Helper.log("Generated Portal On Lava Lake");
            
            return levitateBox(world, airCube);
        }
        
        Helper.log("Generated Portal On Ground");
        
        return pushDownBox(world, airCube.getSubBoxInCenter(areaSize));
    }
    
    private static boolean isLavaLake(
        WorldAccess world, BlockPos blockPos
    ) {
        return world.getBlockState(blockPos).getBlock() == Blocks.LAVA &&
            world.getBlockState(blockPos.add(5, 0, 5)).getBlock() == Blocks.LAVA &&
            world.getBlockState(blockPos.add(-5, 0, -5)).getBlock() == Blocks.LAVA;
    }
    
    private static IntBox getAirCubeOnGround(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter,
        int findingRadius,
        Predicate<BlockPos> groundBlockLimit
    ) {
        return NetherPortalGeneration.fromNearToFarColumned(
            ((ServerWorld) world),
            searchingCenter.getX(), searchingCenter.getZ(),
            findingRadius
        ).filter(
            blockPos -> isAirOnGround(world, blockPos)
        ).filter(
            blockPos -> groundBlockLimit.test(blockPos.add(0, -1, 0))
        ).map(
            basePoint -> IntBox.getBoxByBasePointAndSize(
                areaSize,
                basePoint.subtract(new Vec3i(-areaSize.getX() / 2, 0, -areaSize.getZ() / 2))
            )
        ).filter(
            box -> isAllAir(world, box)
        ).findFirst().orElse(null);
    }
    
    private static IntBox getAirCubeOnSolidGround(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter,
        int findingRadius
    ) {
        return NetherPortalGeneration.fromNearToFarColumned(
            ((ServerWorld) world),
            searchingCenter.getX(), searchingCenter.getZ(),
            findingRadius
        ).filter(
            blockPos -> isAirOnGround(world, blockPos)
        ).filter(
            blockPos -> isAirOnGround(
                world,
                blockPos.add(areaSize.getX() - 1, 0, areaSize.getZ() - 1)
            )
        ).map(
            basePoint -> IntBox.getBoxByBasePointAndSize(areaSize, basePoint)
        ).filter(
            box -> isAllAir(world, box)
        ).findFirst().orElse(null);
    }
    
    //make it possibly generate above ground
    static IntBox findHorizontalPortalPlacement(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter,
        int findingRadius
    ) {
        IntBox result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
            areaSize, world, searchingCenter,
            30, findingRadius / 8
        );
        if (result == null) {
            result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
                areaSize, world, searchingCenter,
                10, findingRadius / 8
            );
        }
        if (result == null) {
            result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
                areaSize, world, searchingCenter,
                1, findingRadius / 8
            );
        }
        return result;
    }
    
    private static IntBox findHorizontalPortalPlacementWithVerticalSpaceReserved(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter,
        int verticalSpaceReserve,
        int findingRadius
    ) {
        BlockPos growVertically = new BlockPos(
            areaSize.getX(),
            verticalSpaceReserve,
            areaSize.getZ()
        );
        IntBox foundCubeArea = findCubeAirAreaAtAnywhere(
            growVertically, world, searchingCenter, findingRadius
        );
        if (foundCubeArea == null) {
            return null;
        }
        return foundCubeArea.getSubBoxInCenter(areaSize);
    }
    
    private static boolean isAirOnGround(
        WorldAccess world,
        BlockPos blockPos
    ) {
        if (world.isAir(blockPos)) {
            BlockPos belowPos = blockPos.add(0, -1, 0);
            return !world.isAir(belowPos);
        }
        
        return false;
    }
    
    static IntBox findCubeAirAreaAtAnywhere(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter,
        int findingRadius
    ) {
        return NetherPortalGeneration.fromNearToFarColumned(
            ((ServerWorld) world),
            searchingCenter.getX(), searchingCenter.getZ(),
            findingRadius
        ).map(
            basePoint -> IntBox.getBoxByBasePointAndSize(
                areaSize, basePoint
            )
        ).filter(
            box -> isAllAir(world, box)
        ).findFirst().orElse(null);
    }
    
    public static boolean isAllAir(WorldAccess world, IntBox box) {
        //the box out of height limit is not accepted
        if (box.h.getY() + 5 >= ((World) world).getDimensionHeight()) {
            return false;
        }
        if (box.l.getY() - 5 <= 0) {
            return false;
        }
        
        
        boolean roughTest = Arrays.stream(box.getEightVertices()).allMatch(
            blockPos -> isAir(world, blockPos)
        );
        if (!roughTest) {
            return false;
        }
        return box.stream().allMatch(
            blockPos -> isAir(world, blockPos)
        );
    }
    
    
    //move the box up
    public static IntBox levitateBox(
        WorldAccess world, IntBox airCube
    ) {
        Integer maxUpShift = Helper.getLastSatisfying(
            IntStream.range(1, 40).boxed(),
            upShift -> isAllAir(
                world,
                airCube.getMoved(new Vec3i(0, upShift, 0))
            )
        );
        if (maxUpShift == null) {
            maxUpShift = 0;
        }
        
        return airCube.getMoved(new Vec3i(0, maxUpShift * 2 / 3, 0));
    }
    
    public static IntBox pushDownBox(
        WorldAccess world, IntBox airCube
    ) {
        Integer downShift = Helper.getLastSatisfying(
            IntStream.range(0, 40).boxed(),
            i -> isAllAir(
                world,
                airCube.getMoved(new Vec3i(0, -i, 0))
            )
        );
        if (downShift == null) {
            downShift = 0;
        }
        
        return airCube.getMoved(new Vec3i(0, -downShift, 0));
    }
    
}
