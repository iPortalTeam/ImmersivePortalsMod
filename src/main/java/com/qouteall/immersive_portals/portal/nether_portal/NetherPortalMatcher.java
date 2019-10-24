package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.minecraft.block.Blocks;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
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
        IntegerAABBInclusive heightLimit
    ) {
        return IntStream
            .range(0, maxRadius)
            .boxed()
            .flatMap(
                r -> new IntegerAABBInclusive(
                    new BlockPos(-r, -r, -r),
                    new BlockPos(r, r, r)
                ).getMoved(
                    searchingCenter
                ).forSixSurfaces(
                    stream -> stream.map(
                        box -> IntegerAABBInclusive.getIntersect(box, heightLimit)
                    ).filter(Objects::nonNull)
                )
            );
    }
    
    //------------------------------------------------------------
    //detect frame from inner pos
    
    public static final int maxFrameSize = 40;
    public static final int findingRadius = 150;
    public static final IntegerAABBInclusive heightLimitOverworld = new IntegerAABBInclusive(
        new BlockPos(Integer.MIN_VALUE, 2, Integer.MIN_VALUE),
        new BlockPos(Integer.MAX_VALUE, 254, Integer.MAX_VALUE)
    );
    public static final IntegerAABBInclusive heightLimitNether = new IntegerAABBInclusive(
        new BlockPos(Integer.MIN_VALUE, 2, Integer.MIN_VALUE),
        new BlockPos(Integer.MAX_VALUE, 126, Integer.MAX_VALUE)
    );
    
    public static IntegerAABBInclusive getHeightLimit(
        DimensionType dimension
    ) {
        return dimension == DimensionType.THE_NETHER ? heightLimitNether : heightLimitOverworld;
    }
    
    //return null for no legal obsidian frame
    public static ObsidianFrame detectFrameFromInnerPos(
        IWorld world,
        BlockPos innerPos,
        Direction.Axis normalAxis,
        Predicate<IntegerAABBInclusive> innerAreaFilter
    ) {
        if (!isAirOrFire(world, innerPos)) {
            return null;
        }
        
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(normalAxis);
        
        IntegerAABBInclusive innerArea = detectInnerArea(
            world,
            innerPos,
            normalAxis,
            anotherTwoAxis
        );
        
        if (innerArea == null) {
            return null;
        }
        
        if (!innerAreaFilter.test(innerArea)) {
            return null;
        }
    
        if (!isObsidianFrameIntact(world, normalAxis, innerArea)) {
            return null;
        }
        
        return new ObsidianFrame(normalAxis, innerArea);
    }
    
    public static ObsidianFrame detectFrameFromInnerPos(
        IWorld world,
        BlockPos innerPos,
        Direction.Axis normalAxis
    ) {
        return detectFrameFromInnerPos(
            world, innerPos, normalAxis, (innerArea) -> true
        );
    }
    
    public static boolean isObsidianFrameIntact(
        IWorld world,
        Direction.Axis normalAxis,
        IntegerAABBInclusive innerArea
    ) {
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(normalAxis);
        
        return Arrays.stream(Helper.getAnotherFourDirections(normalAxis))
            .map(
                facing -> innerArea.getSurfaceLayer(
                    facing
                ).getMoved(
                    facing.getVector()
                )
            ).allMatch(
                box -> box.stream().allMatch(
                    blockPos -> isObsidian(world, blockPos)
                )
            );
    }
    
    private static IntegerAABBInclusive detectInnerArea(
        IWorld world,
        BlockPos innerPos,
        Direction.Axis normalAxis,
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis
    ) {
        IntegerAABBInclusive stick1 = detectStick(
            world, innerPos, anotherTwoAxis.getLeft(),
            blockPos -> isAirOrFire(world, blockPos), 1
        );
        if (stick1 == null) return null;
        
        IntegerAABBInclusive stick2 = detectStick(
            world, innerPos, anotherTwoAxis.getRight(),
            blockPos -> isAirOrFire(world, blockPos), 1
        );
        if (stick2 == null) return null;
        
        IntegerAABBInclusive innerArea = IntegerAABBInclusive.getContainingBox(stick1, stick2);
        
        assert Math.abs(Helper.getCoordinate(innerArea.getSize(), normalAxis)) == 1;
        
        //check inner air
        if (!innerArea.stream().allMatch(blockPos -> isAirOrFire(world, blockPos))) {
            return null;
        }
        
        return innerArea;
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
    private static IntegerAABBInclusive detectStick(
        IWorld world,
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
        return new IntegerAABBInclusive(lowExtremity, highExtremity);
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
    
    private static boolean isObsidian(IWorld world, BlockPos obsidianPos) {
        return world.getBlockState(obsidianPos) == Blocks.OBSIDIAN.getDefaultState();
    }
    
    private static boolean isAir(IWorld world, BlockPos pos) {
        return world.isAir(pos);
    }
    
    private static boolean isAirOrFire(IWorld world, BlockPos pos) {
        return world.isAir(pos) || world.getBlockState(pos).getBlock() == Blocks.FIRE;
    }
    
    //------------------------------------------------------------
    //detect air cube on ground
    
    static IntegerAABBInclusive findVerticalPortalPlacement(
        BlockPos areaSize,
        IWorld world,
        BlockPos searchingCenter,
        IntegerAABBInclusive heightLimit,
        int findingRadius
    ) {
        IntegerAABBInclusive aboveLavaLake = getAirCubeOnGround(
            areaSize.add(20, 20, 20), world, searchingCenter,
            heightLimit, findingRadius / 4,
            blockPos -> isLavaLake(world, blockPos)
        );
        if (aboveLavaLake != null) {
            Helper.log("Generated Portal Above Lava Lake");
            return aboveLavaLake.getSubBoxInCenter(areaSize);
        }
    
        Helper.log("Generated Portal On Ground");
    
        IntegerAABBInclusive biggerArea = getAirCubeOnSolidGround(
            areaSize.add(6, 0, 6), world, searchingCenter,
            heightLimit, findingRadius
        );
        return pushDownBox(world, biggerArea.getSubBoxInCenter(areaSize));
    }
    
    private static boolean isLavaLake(
        IWorld world, BlockPos blockPos
    ) {
        return world.getBlockState(blockPos).getBlock() == Blocks.LAVA &&
            world.getBlockState(blockPos.add(5, 0, 5)).getBlock() == Blocks.LAVA &&
            world.getBlockState(blockPos.add(-5, 0, -5)).getBlock() == Blocks.LAVA;
    }
    
    private static IntegerAABBInclusive getAirCubeOnGround(
        BlockPos areaSize,
        IWorld world,
        BlockPos searchingCenter,
        IntegerAABBInclusive heightLimit,
        int findingRadius,
        Predicate<BlockPos> groundBlockLimit
    ) {
        return fromNearToFarWithinHeightLimit(
            searchingCenter,
            findingRadius,
            heightLimit
        ).filter(
            blockPos -> isAirOnGround(world, blockPos)
        ).filter(
            blockPos -> groundBlockLimit.test(blockPos.add(0, -1, 0))
        ).map(
            basePoint -> IntegerAABBInclusive.getBoxByBasePointAndSize(
                areaSize,
                basePoint.subtract(new Vec3i(-areaSize.getX() / 2, 0, -areaSize.getZ() / 2))
            )
        ).filter(
            box -> isAllAir(world, box)
        ).findFirst().orElse(null);
    }
    
    private static IntegerAABBInclusive getAirCubeOnSolidGround(
        BlockPos areaSize,
        IWorld world,
        BlockPos searchingCenter,
        IntegerAABBInclusive heightLimit,
        int findingRadius
    ) {
        return fromNearToFarWithinHeightLimit(
            searchingCenter,
            findingRadius,
            heightLimit
        ).filter(
            blockPos -> isAirOnGround(world, blockPos)
        ).filter(
            blockPos -> isAirOnGround(
                world,
                blockPos.add(areaSize.getX() - 1, 0, areaSize.getZ() - 1)
            )
        ).map(
            basePoint -> IntegerAABBInclusive.getBoxByBasePointAndSize(areaSize, basePoint)
        ).filter(
            box -> isAllAir(world, box)
        ).findFirst().orElse(null);
    }
    
    //make it possibly generate above ground
    static IntegerAABBInclusive findHorizontalPortalPlacement(
        BlockPos areaSize,
        IWorld world,
        BlockPos searchingCenter,
        IntegerAABBInclusive heightLimit,
        int findingRadius
    ) {
        IntegerAABBInclusive result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
            areaSize, world, searchingCenter, heightLimit,
            30
        );
        if (result == null) {
            result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
                areaSize, world, searchingCenter, heightLimit,
                10
            );
        }
        if (result == null) {
            result = findHorizontalPortalPlacementWithVerticalSpaceReserved(
                areaSize, world, searchingCenter, heightLimit,
                1
            );
        }
        return result;
    }
    
    private static IntegerAABBInclusive findHorizontalPortalPlacementWithVerticalSpaceReserved(
        BlockPos areaSize,
        IWorld world,
        BlockPos searchingCenter,
        IntegerAABBInclusive heightLimit,
        int verticalSpaceReserve
    ) {
        BlockPos growVertically = new BlockPos(
            areaSize.getX(),
            verticalSpaceReserve,
            areaSize.getZ()
        );
        IntegerAABBInclusive foundCubeArea = findCubeAirAreaAtAnywhere(
            growVertically, world, searchingCenter, heightLimit, findingRadius
        );
        if (foundCubeArea == null) {
            return null;
        }
        return foundCubeArea.getSubBoxInCenter(areaSize);
    }
    
    private static boolean isAirOnGround(
        IWorld world,
        BlockPos blockPos
    ) {
        if (world.isAir(blockPos)) {
            BlockPos belowPos = blockPos.add(0, -1, 0);
            return !world.isAir(belowPos);
        }
        
        return false;
    }
    
    static IntegerAABBInclusive findCubeAirAreaAtAnywhere(
        BlockPos areaSize,
        IWorld world,
        BlockPos searchingCenter,
        IntegerAABBInclusive heightLimit,
        int findingRadius
    ) {
        return fromNearToFarWithinHeightLimit(
            searchingCenter,
            findingRadius, heightLimit
        ).map(
            basePoint -> IntegerAABBInclusive.getBoxByBasePointAndSize(
                areaSize, basePoint
            )
        ).filter(
            box -> isAllAir(world, box)
        ).findFirst().orElse(null);
    }
    
    public static boolean isAllAir(IWorld world, IntegerAABBInclusive box) {
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
    
    //------------------------------------------------------------
    //detect existing obsidian frame
    
    //@Nullable
    public static ObsidianFrame findEmptyObsidianFrame(
        IWorld world,
        BlockPos searchingCenter,
        Direction.Axis normalAxis,
        Predicate<IntegerAABBInclusive> filter,
        int findingRadius
    ) {
        Pair<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(normalAxis);
        Direction roughTestObsidianFace1 = Direction.get(
            Direction.AxisDirection.POSITIVE,
            anotherTwoAxis.getLeft()
        );
        Direction roughTestObsidianFace2 = Direction.get(
            Direction.AxisDirection.POSITIVE,
            anotherTwoAxis.getRight()
        );
        
        Optional<ObsidianFrame> result =
            fromNearToFarWithinHeightLimit(searchingCenter, findingRadius, heightLimitOverworld)
                .filter(
                    blockPos -> isAirOnObsidian(
                        world, blockPos,
                        roughTestObsidianFace1,
                        roughTestObsidianFace2
                    )
                )
                .map(
                    blockPos -> detectFrameFromInnerPos(
                        world, blockPos, normalAxis, filter
                    )
                )
                .filter(
                    Objects::nonNull
                )
                .findFirst();
        
        return result.orElse(null);
    }
    
    private static boolean isAirOnObsidian(
        IWorld world,
        BlockPos blockPos,
        Direction obsidianFace1,
        Direction obsidianFace2
    ) {
        return world.isAir(blockPos) &&
            isObsidian(
                world,
                blockPos.add(obsidianFace1.getVector())
            ) &&
            isObsidian(
                world,
                blockPos.add(obsidianFace2.getVector())
            );
    }
    
    
    //move the box up
    public static IntegerAABBInclusive levitateBox(
        IWorld world, IntegerAABBInclusive airCube
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
    
    public static IntegerAABBInclusive pushDownBox(
        IWorld world, IntegerAABBInclusive airCube
    ) {
        Integer downShift = Helper.getLastSatisfying(
            IntStream.range(1, 40).boxed(),
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
