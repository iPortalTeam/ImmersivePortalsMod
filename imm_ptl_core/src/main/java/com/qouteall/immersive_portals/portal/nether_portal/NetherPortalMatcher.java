package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NetherPortalMatcher {
    // bad in performance
    @Deprecated
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
    
    @Deprecated
    public static IntBox getHeightLimit(
        World world
    ) {
        return new IntBox(
            new BlockPos(Integer.MIN_VALUE, 2, Integer.MIN_VALUE),
            new BlockPos(Integer.MAX_VALUE, world.getDimensionHeight() - 2, Integer.MAX_VALUE)
        );
    }
    
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    
    public static boolean isAirOrFire(BlockState blockState) {
        return blockState.isAir() || blockState.getBlock() == Blocks.FIRE;
    }
    
    //------------------------------------------------------------
    //detect air cube on ground
    
    static IntBox findVerticalPortalPlacement(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter
    ) {
        int radius = 16;
        IntBox airCube = getAirCubeOnSolidGround(
            areaSize, new BlockPos(6, 0, 6), world, searchingCenter,
            radius, true
        );
        
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Ground with 3 Spacing");
            airCube = getAirCubeOnSolidGround(
                areaSize, new BlockPos(2, 0, 2), world, searchingCenter,
                radius, true
            );
        }
        
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Ground with 1 Spacing");
            airCube = getAirCubeOnSolidGround(
                areaSize, new BlockPos(6, 0, 6), world, searchingCenter,
                radius, false
            );
        }
        
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Non Solid Surface");
            return null;
        }
        
        if (world.getBlockState(airCube.l.down()).getMaterial().isSolid()) {
            Helper.log("Generated Portal On Ground");
            
            return pushDownBox(world, airCube.getSubBoxInCenter(areaSize));
        }
        else {
            Helper.log("Generated Portal On Non Solid Surface");
            
            return levitateBox(world, airCube.getSubBoxInCenter(areaSize), 40);
        }
        
    }
    
    private static boolean isLavaLake(
        WorldAccess world, BlockPos blockPos
    ) {
        return world.getBlockState(blockPos).getBlock() == Blocks.LAVA &&
            world.getBlockState(blockPos.add(5, 0, 5)).getBlock() == Blocks.LAVA &&
            world.getBlockState(blockPos.add(-5, 0, -5)).getBlock() == Blocks.LAVA;
    }
    
    @Deprecated
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
            blockPos -> isAirOnSolidGround(world, blockPos)
        ).filter(
            blockPos -> groundBlockLimit.test(blockPos.add(0, -1, 0))
        ).map(
            basePoint -> IntBox.getBoxByBasePointAndSize(
                areaSize,
                basePoint.subtract(new Vec3i(-areaSize.getX() / 2, 0, -areaSize.getZ() / 2))
            )
        ).filter(
            box -> isAirCubeMediumPlace(world, box)
        ).findFirst().orElse(null);
    }
    
    private static IntBox expandFromBottomCenter(IntBox box, BlockPos spacing) {
        BlockPos boxSize = box.getSize();
        
        return box.getAdjusted(
            -spacing.getX() / 2, 0, -spacing.getZ() / 2,
            spacing.getX() / 2, spacing.getY(), spacing.getZ() / 2
        );
    }
    
    private static IntBox getAirCubeOnSolidGround(
        BlockPos areaSize,
        BlockPos ambientSpaceReserved,
        WorldAccess world,
        BlockPos searchingCenter,
        int findingRadius,
        boolean solidGround
    ) {
        Predicate<BlockPos> isAirOnGroundPredicate =
            blockPos -> solidGround ? isAirOnSolidGround(world, blockPos) :
                isAirOnGround(world, blockPos);
        return NetherPortalGeneration.fromNearToFarColumned(
            ((ServerWorld) world),
            searchingCenter.getX(), searchingCenter.getZ(),
            findingRadius
        ).filter(
            isAirOnGroundPredicate
        ).map(
            basePoint -> IntBox.getBoxByBasePointAndSize(areaSize, basePoint)
        ).filter(
            box -> isAirCubeMediumPlace(world, box)
        ).filter(
            box -> solidGround ?
                box.getSurfaceLayer(Direction.DOWN).stream().allMatch(isAirOnGroundPredicate)
                : true
        ).filter(
            box -> expandFromBottomCenter(box, ambientSpaceReserved).stream()
                .allMatch(blockPos -> world.getBlockState(blockPos).isOpaque())
        ).findFirst().orElse(null);
    }
    
    //make it possibly generate above ground
    static IntBox findHorizontalPortalPlacement(
        BlockPos areaSize,
        WorldAccess world,
        BlockPos searchingCenter
    ) {
        IntBox result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
            areaSize, world, searchingCenter,
            30, 12
        );
        if (result == null) {
            result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
                areaSize, world, searchingCenter,
                10, 12
            );
        }
        if (result == null) {
            result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
                areaSize, world, searchingCenter,
                1, 12
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
    
    // does not contain lava water
    public static boolean isSolidGroundBlock(BlockState blockState) {
        return blockState.getMaterial().isSolid();
    }
    
    // includes lava water
    public static boolean isGroundBlock(BlockState blockState) {
        return !blockState.isAir();
    }
    
    private static boolean isAirOnSolidGround(WorldAccess world, BlockPos blockPos) {
        return world.isAir(blockPos) &&
            isSolidGroundBlock(world.getBlockState(blockPos.add(0, -1, 0)));
    }
    
    private static boolean isAirOnGround(WorldAccess world, BlockPos blockPos) {
        return world.isAir(blockPos) &&
            isGroundBlock(world.getBlockState(blockPos.add(0, -1, 0)));
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
            box -> isAirCubeMediumPlace(world, box)
        ).findFirst().orElse(null);
    }
    
    public static boolean isAirCubeMediumPlace(WorldAccess world, IntBox box) {
        //the box out of height limit is not accepted
        if (box.h.getY() + 5 >= ((World) world).getDimensionHeight()) {
            return false;
        }
        if (box.l.getY() - 5 <= 0) {
            return false;
        }
        
        return isAllAir(world, box);
    }
    
    public static boolean isAllAir(WorldAccess world, IntBox box) {
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
        WorldAccess world, IntBox airCube, int maxOffset
    ) {
        Integer maxUpShift = Helper.getLastSatisfying(
            IntStream.range(1, maxOffset * 3 / 2).boxed(),
            upShift -> isAirCubeMediumPlace(
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
            i -> isAirCubeMediumPlace(
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
