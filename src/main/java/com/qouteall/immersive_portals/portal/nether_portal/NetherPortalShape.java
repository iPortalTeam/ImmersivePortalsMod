package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.SpecialPortalShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetherPortalShape {
    public BlockPos anchor;
    public Set<BlockPos> area;
    public IntegerAABBInclusive areaBox;
    public Direction.Axis axis;
    public Set<BlockPos> frameAreaWithoutCorner;
    public Set<BlockPos> frameAreaWithCorner;
    
    public NetherPortalShape(
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
    
    //null for not found
    public static NetherPortalShape findArea(
        BlockPos startingPos,
        Direction.Axis axis,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian
    ) {
        if (!isAir.test(startingPos)) {
            return null;
        }
        
        Set<BlockPos> area = new HashSet<>();
        findAreaRecursively(
            startingPos,
            isAir,
            Helper.getAnotherFourDirections(axis),
            area
        );
        
        NetherPortalShape result =
            new NetherPortalShape(area, axis);
        
        if (!result.isFrameIntact(isObsidian)) {
            return null;
        }
        
        return result;
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
    
    //return null for not match
    public NetherPortalShape matchShape(
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        BlockPos newAnchor
    ) {
        if (!isAir.test(newAnchor)) {
            return null;
        }

//        boolean roughTest = Arrays.stream(Helper.getAnotherFourDirections(axis)).anyMatch(
//            direction -> isObsidian.test(newAnchor.add(direction.getVector()))
//        );
//
//        if (!roughTest) {
//            return null;
//        }
        
        boolean testFrame = frameAreaWithoutCorner.stream().map(
            blockPos -> blockPos.subtract(anchor).add(newAnchor)
        ).allMatch(
            isObsidian
        );
        
        if (!testFrame) {
            return null;
        }
        
        boolean testAir = area.stream().map(
            blockPos -> blockPos.subtract(anchor).add(newAnchor)
        ).allMatch(
            isAir
        );
        
        if (!testAir) {
            return null;
        }
        
        return getShapeWithMovedAnchor(newAnchor);
    }
    
    public NetherPortalShape getShapeWithMovedAnchor(
        BlockPos newAnchor
    ) {
        BlockPos offset = newAnchor.subtract(anchor);
        return new NetherPortalShape(
            area.stream().map(
                blockPos -> blockPos.add(offset)
            ).collect(Collectors.toSet()),
            axis
        );
    }
    
    public boolean isFrameIntact(
        Predicate<BlockPos> isObsidian
    ) {
        return frameAreaWithoutCorner.stream().allMatch(isObsidian::test);
    }
    
    public boolean isPortalIntact(
        Predicate<BlockPos> isPortalBlock,
        Predicate<BlockPos> isObsidian
    ) {
        return isFrameIntact(isObsidian) &&
            area.stream().allMatch(isPortalBlock);
    }
    
    public void initPortalPosAxisShape(Portal portal, boolean doInvert) {
        BlockPos centerBlockPos = areaBox.getCenter();
        Vec3d center = new Vec3d(centerBlockPos).add(0.5, 0.5, 0.5);
        portal.setPosition(center.x, center.y, center.z);
        
        Direction[] anotherFourDirections = Helper.getAnotherFourDirections(axis);
        Direction wDirection;
        Direction hDirection;
        if (doInvert) {
            wDirection = anotherFourDirections[0];
            hDirection = anotherFourDirections[1];
        }
        else {
            wDirection = anotherFourDirections[1];
            hDirection = anotherFourDirections[0];
        }
        portal.axisW = new Vec3d(wDirection.getVector());
        portal.axisH = new Vec3d(hDirection.getVector());
        
        SpecialPortalShape shape = new SpecialPortalShape();
        Vec3d offset = new Vec3d(
            Direction.from(axis, Direction.AxisDirection.POSITIVE).getVector()
        ).multiply(0.5);
        for (BlockPos blockPos : area) {
            Vec3d p1 = new Vec3d(blockPos).add(offset);
            Vec3d p2 = new Vec3d(blockPos).add(0.5, 0.5, 0.5).add(offset);
            double p1LocalX = p1.subtract(center).dotProduct(portal.axisW);
            double p1LocalY = p1.subtract(center).dotProduct(portal.axisH);
            double p2LocalX = p2.subtract(center).dotProduct(portal.axisW);
            double p2LocalY = p2.subtract(center).dotProduct(portal.axisH);
            shape.addTriangleForRectangle(
                p1LocalX, p1LocalY,
                p2LocalX, p2LocalY
            );
        }
        
        portal.specialShape = shape;
    }
    
}
