package qouteall.imm_ptl.core.portal.nether_portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class NetherPortalMatcher {
    private static boolean isAir(LevelAccessor world, BlockPos pos) {
        return world.isEmptyBlock(pos);
    }
    
    static IntBox findVerticalPortalPlacement(
        BlockPos areaSize,
        LevelAccessor world,
        BlockPos searchingCenter
    ) {
        int radius = 16;
        
        int maxY = McHelper.getMaxContentYExclusive(world);
        int minY = McHelper.getMinY(world);
        
        // search for place above ground
        IntBox airCube = getAirCubeOnSolidGround(
            areaSize, new BlockPos(6, 0, 6), world, searchingCenter,
            radius, true,
            64, maxY
        );
        
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Ground with 6 Spacing");
            airCube = getAirCubeOnSolidGround(
                areaSize, new BlockPos(2, 0, 2), world, searchingCenter,
                radius, true,
                64, maxY
            );
        }
        
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Ground with 2 Spacing");
            airCube = getAirCubeOnSolidGround(
                areaSize, new BlockPos(6, 0, 6), world, searchingCenter,
                radius, false,
                minY, maxY
            );
        }
        
        if (airCube == null) {
            Helper.log("Cannot Find Portal Placement on Solid Surface");
            return null;
        }
        
        if (world.getBlockState(airCube.l.below()).isSolid()) {
            Helper.log("Generated Portal On Ground");
            
            return pushDownBox(world, airCube.getSubBoxInCenter(areaSize));
        }
        else {
            Helper.log("Generated Portal On Non Solid Surface");
            
            return levitateBox(world, airCube.getSubBoxInCenter(areaSize), 40);
        }
        
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
        LevelAccessor world,
        BlockPos searchingCenter,
        int findingRadius,
        boolean solidGround,
        int startY,
        int endY
    ) {
        Predicate<BlockPos> isAirOnGroundPredicate =
            blockPos -> solidGround ? isAirOnSolidGround(world, blockPos) :
                isAirOnGround(world, blockPos);
        
        return BlockTraverse.searchColumned(
            searchingCenter.getX(), searchingCenter.getZ(), findingRadius,
            startY, endY,
            mutable -> {
                if (isAirOnGroundPredicate.test(mutable)) {
                    IntBox box = IntBox.fromBasePointAndSize(mutable, areaSize);
                    
                    IntBox expanded = expandFromBottomCenter(box, ambientSpaceReserved);
                    if (isAirCubeMediumPlace(world, expanded)) {
                        if (solidGround) {
                            if (BlockTraverse.boxAllMatch(box.getSurfaceLayer(Direction.DOWN), isAirOnGroundPredicate)) {
                                if (isAirOnGroundPredicate.test(expanded.l)) {
                                    return box;
                                }
                            }
                        }
                        else {
                            return box;
                        }
                    }
                }
                return null;
            }
        );
    }
    
    //make it possibly generate above ground
    static IntBox findHorizontalPortalPlacement(
        BlockPos areaSize,
        LevelAccessor world,
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
        LevelAccessor world,
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
        return blockState.isSolid();
    }
    
    // includes lava water
    public static boolean isGroundBlock(BlockState blockState) {
        return !blockState.isAir();
    }
    
    private static boolean isAirOnSolidGround(LevelAccessor world, BlockPos blockPos) {
        return world.isEmptyBlock(blockPos) &&
            isSolidGroundBlock(world.getBlockState(blockPos.offset(0, -1, 0)));
    }
    
    private static boolean isAirOnGround(LevelAccessor world, BlockPos blockPos) {
        return world.isEmptyBlock(blockPos) &&
            isGroundBlock(world.getBlockState(blockPos.offset(0, -1, 0)));
    }
    
    public static IntBox findCubeAirAreaAtAnywhere(
        BlockPos areaSize,
        LevelAccessor world,
        BlockPos searchingCenter,
        int findingRadius
    ) {
        return BlockTraverse.searchColumned(
            searchingCenter.getX() - (areaSize.getX() / 2),
            searchingCenter.getZ() - (areaSize.getZ() / 2),
            findingRadius,
            1 + McHelper.getMinY(world), McHelper.getMaxYExclusive(world) - 1,
            mutable -> {
                IntBox box = IntBox.fromBasePointAndSize(mutable, areaSize);
                if (isAirCubeMediumPlace(world, box)) {
                    return box;
                }
                else {
                    return null;
                }
            }
        );
    }
    
    public static boolean isAirCubeMediumPlace(LevelAccessor world, IntBox box) {
        //the box out of height limit is not accepted
        if (box.h.getY() + 1 >= McHelper.getMaxContentYExclusive(world)) {
            return false;
        }
        if (box.l.getY() - 1 <= McHelper.getMinY(world)) {
            return false;
        }
        
        return isAllAir(world, box);
    }
    
    public static boolean isAllAir(LevelAccessor world, IntBox box) {
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
        LevelAccessor world, IntBox airCube, int maxOffset
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
        LevelAccessor world, IntBox airCube
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
