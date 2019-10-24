package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpecialNetherPortalShape {
    public BlockPos anchor;
    public Set<BlockPos> area;
    public IntegerAABBInclusive areaBox;
    public Direction.Axis axis;
    public Set<BlockPos> frameAreaWithoutCorner;
    public Set<BlockPos> frameAreaWithCorner;
    
    public SpecialNetherPortalShape(
        Set<BlockPos> area, Direction.Axis axis
    ) {
        this.area = area;
        this.axis = axis;
        
        calcAnchor();
        
        calcArea();
        
        calcFrameArea();
    }
    
    public void calcAnchor() {
        anchor = area.stream()
            .min(
                Comparator.<BlockPos>comparingInt(
                    Vec3i::getX
                ).<BlockPos>thenComparingInt(
                    Vec3i::getY
                ).<BlockPos>thenComparingInt(
                    Vec3i::getZ
                )
            ).get();
        
        Validate.notNull(anchor);
    }
    
    public void calcArea() {
        areaBox = Helper.reduceWithDifferentType(
            new IntegerAABBInclusive(anchor, anchor),
            area.stream(),
            IntegerAABBInclusive::getExpanded
        );
    }
    
    public void calcFrameArea() {
        Direction[] directions = Helper.getAnotherFourDirections(axis);
        frameAreaWithoutCorner = area.stream().flatMap(
            blockPos -> Stream.of(
                blockPos.add(directions[0].getVector()),
                blockPos.add(directions[1].getVector()),
                blockPos.add(directions[2].getVector()),
                blockPos.add(directions[3].getVector())
            )
        ).filter(
            blockPos -> !area.contains(blockPos)
        ).collect(Collectors.toSet());
        
        BlockPos[] cornerOffsets = {
            new BlockPos(directions[0].getVector()).add(directions[2].getVector()),
            new BlockPos(directions[1].getVector()).add(directions[3].getVector()),
            new BlockPos(directions[2].getVector()).add(directions[0].getVector()),
            new BlockPos(directions[3].getVector()).add(directions[1].getVector())
        };
        
        frameAreaWithCorner = area.stream().flatMap(
            blockPos -> Stream.of(
                blockPos.add(cornerOffsets[0]),
                blockPos.add(cornerOffsets[1]),
                blockPos.add(cornerOffsets[2]),
                blockPos.add(cornerOffsets[3])
            )
        ).filter(
            blockPos -> !area.contains(blockPos)
        ).collect(Collectors.toSet());
        frameAreaWithCorner.addAll(frameAreaWithoutCorner);
    }
    
    public static SpecialNetherPortalShape findArea(
        ServerWorld world,
        BlockPos startingPos,
        Direction.Axis axis
    ) {
        Set<BlockPos> area = new HashSet<>();
        findAreaRecursively(
            startingPos,
            world::isAir,
            Helper.getAnotherFourDirections(axis),
            area
        );
        
        return new SpecialNetherPortalShape(area, axis);
    }
    
    private static void findAreaRecursively(
        BlockPos startingPos,
        Predicate<BlockPos> isEmptyPos,
        Direction[] directions,
        Set<BlockPos> foundArea
    ) {
        if (foundArea.size() > 400) {
            return;
        }
        for (Direction direction : directions) {
            BlockPos newPos = startingPos.add(direction.getVector());
            if (!foundArea.contains(newPos)) {
                foundArea.add(newPos);
                findAreaRecursively(
                    newPos,
                    isEmptyPos,
                    directions,
                    foundArea
                );
            }
        }
    }
    
    public boolean isShapeMatched(
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        BlockPos newAnchor
    ) {
        if (!isAir.test(newAnchor)) {
            return false;
        }
        
        boolean roughTest = Arrays.stream(Helper.getAnotherFourDirections(axis)).anyMatch(
            direction -> isObsidian.test(newAnchor.add(direction.getVector()))
        );
        
        if (!roughTest) {
            return false;
        }
        
        boolean testFrame = frameAreaWithoutCorner.stream().map(
            blockPos -> blockPos.subtract(anchor).add(newAnchor)
        ).allMatch(
            isObsidian
        );
        
        if (!testFrame) {
            return false;
        }
        
        boolean testAir = area.stream().map(
            blockPos -> blockPos.subtract(anchor).add(newAnchor)
        ).allMatch(
            isAir
        );
        
        return testAir;
    }
}
