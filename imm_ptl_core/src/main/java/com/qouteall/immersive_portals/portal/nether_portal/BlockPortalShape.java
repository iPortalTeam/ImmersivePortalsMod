package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockPortalShape {
    public BlockPos anchor;
    public Set<BlockPos> area;
    public IntBox innerAreaBox;
    public IntBox totalAreaBox;
    public Direction.Axis axis;
    public Set<BlockPos> frameAreaWithoutCorner;
    public Set<BlockPos> frameAreaWithCorner;
    
    private BlockPos firstFramePos;
    
    public BlockPortalShape(
        Set<BlockPos> area, Direction.Axis axis
    ) {
        this.area = area;
        this.axis = axis;
        
        calcAnchor();
        
        calcFrameArea();
        
        calcAreaBox();
    }
    
    public BlockPortalShape(
        CompoundTag tag
    ) {
        this(
            readArea(tag.getList("poses", 3)),
            Direction.Axis.values()[tag.getInt("axis")]
        );
    }
    
    private static Set<BlockPos> readArea(ListTag list) {
        int size = list.size();
        
        Validate.isTrue(size % 3 == 0);
        Set<BlockPos> result = new HashSet<>();
        
        for (int i = 0; i < size / 3; i++) {
            result.add(new BlockPos(
                list.getInt(i * 3 + 0),
                list.getInt(i * 3 + 1),
                list.getInt(i * 3 + 2)
            ));
        }
        
        return result;
    }
    
    
    public CompoundTag toTag() {
        CompoundTag data = new CompoundTag();
        ListTag list = new ListTag();
        
        area.forEach(blockPos -> {
            list.add(list.size(), IntTag.of(blockPos.getX()));
            list.add(list.size(), IntTag.of(blockPos.getY()));
            list.add(list.size(), IntTag.of(blockPos.getZ()));
        });
        
        data.put("poses", list);
        data.putInt("axis", axis.ordinal());
        
        return data;
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
    
    public void calcAreaBox() {
        innerAreaBox = Helper.reduce(
            new IntBox(anchor, anchor),
            area.stream(),
            IntBox::getExpanded
        );
        totalAreaBox = Helper.reduce(
            new IntBox(anchor, anchor),
            frameAreaWithoutCorner.stream(),
            IntBox::getExpanded
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
            new BlockPos(directions[0].getVector()).add(directions[1].getVector()),
            new BlockPos(directions[1].getVector()).add(directions[2].getVector()),
            new BlockPos(directions[2].getVector()).add(directions[3].getVector()),
            new BlockPos(directions[3].getVector()).add(directions[0].getVector())
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
        
        firstFramePos = frameAreaWithoutCorner.iterator().next();
    }
    
    //null for not found
    public static BlockPortalShape findArea(
        BlockPos startingPos,
        Direction.Axis axis,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian
    ) {
        if (!isAir.test(startingPos)) {
            return null;
        }
        
        return findShapeWithoutRegardingStartingPos(startingPos, axis, isAir, isObsidian);
    }
    
    public static BlockPortalShape findShapeWithoutRegardingStartingPos(
        BlockPos startingPos, Direction.Axis axis, Predicate<BlockPos> isAir, Predicate<BlockPos> isObsidian
    ) {
        startingPos = startingPos.toImmutable();
        
        Set<BlockPos> area = new HashSet<>();
        area.add(startingPos);
        
        Direction[] directions = Helper.getAnotherFourDirections(axis);
        boolean isNormalFrame = findAreaBreadthFirst(
            startingPos,
            isAir,
            isObsidian,
            directions,
            area,
            startingPos
        );
        
        if (!isNormalFrame) {
            return null;
        }
        
        return new BlockPortalShape(area, axis);
    }
    
    private static boolean findAreaBreadthFirst(
        BlockPos startingPos,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        Direction[] directions,
        Set<BlockPos> foundArea,
        BlockPos initialPos
    ) {
        
        ArrayDeque<BlockPos> newlyAdded = new ArrayDeque<>();
        newlyAdded.addLast(startingPos);
        
        while (!newlyAdded.isEmpty()) {
            if (foundArea.size() > 400) {
                return false;
            }
            
            BlockPos last = newlyAdded.pollFirst();
            for (Direction direction : directions) {
                BlockPos curr = last.offset(direction).toImmutable();
                if (!foundArea.contains(curr)) {
                    if (isAir.test(curr)) {
                        newlyAdded.addLast(curr);
                        foundArea.add(curr);
                    }
                    else if (isObsidian.test(curr)) {
                        //nothing happens
                    }
                    else {
                        return false;
                    }
                }
            }
            
            BlockPos delta = initialPos.subtract(startingPos);
            if (Math.abs(delta.getX()) > 20 || Math.abs(delta.getY()) > 20 || Math.abs(delta.getZ()) > 20) {
                return false;
            }
        }
        
        return true;
    }
    
    @Deprecated
    private static boolean findAreaRecursively(
        BlockPos currPos,
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        Direction[] directions,
        Set<BlockPos> foundArea,
        BlockPos initialPos
    ) {
        if (foundArea.size() > 400) {
            return false;
        }
        BlockPos delta = initialPos.subtract(currPos);
        if (Math.abs(delta.getX()) > 20 || Math.abs(delta.getY()) > 20 || Math.abs(delta.getZ()) > 20) {
            return false;
        }
        for (Direction direction : directions) {
            BlockPos newPos = currPos.add(direction.getVector());
            if (!foundArea.contains(newPos)) {
                if (isAir.test(newPos)) {
                    foundArea.add(newPos.toImmutable());
                    boolean shouldContinue = findAreaRecursively(
                        newPos,
                        isAir,
                        isObsidian,
                        directions,
                        foundArea,
                        initialPos
                    );
                    if (!shouldContinue) {
                        return false;
                    }
                }
                else {
                    if (!isObsidian.test(newPos)) {
                        //abort
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    //return null for not match
    public BlockPortalShape matchShape(
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        BlockPos newAnchor,
        BlockPos.Mutable temp
    ) {
        if (!isAir.test(newAnchor)) {
            return null;
        }
        
        boolean testFrame = testFrameWithoutCorner(isObsidian, newAnchor, temp);
        
        if (!testFrame) {
            return null;
        }
        
        boolean testAir = area.stream().map(
            blockPos -> temp.set(
                blockPos.getX() - anchor.getX() + newAnchor.getX(),
                blockPos.getY() - anchor.getY() + newAnchor.getY(),
                blockPos.getZ() - anchor.getZ() + newAnchor.getZ()
            )
            //blockPos.subtract(anchor).add(newAnchor)
        ).allMatch(
            isAir
        );
        
        if (!testAir) {
            return null;
        }
        
        return getShapeWithMovedAnchor(newAnchor);
    }
    
    private boolean testFrameWithoutCorner(
        Predicate<BlockPos> isObsidian,
        BlockPos newAnchor,
        BlockPos.Mutable temp
    ) {
        Function<BlockPos, BlockPos.Mutable> mapper = blockPos -> temp.set(
            blockPos.getX() - anchor.getX() + newAnchor.getX(),
            blockPos.getY() - anchor.getY() + newAnchor.getY(),
            blockPos.getZ() - anchor.getZ() + newAnchor.getZ()
        );
        
        //does this have optimization effect?
        if (!isObsidian.test(mapper.apply(firstFramePos))) {
            return false;
        }
        
        return frameAreaWithoutCorner.stream().map(mapper).allMatch(isObsidian);
    }
    
    public BlockPortalShape getShapeWithMovedAnchor(
        BlockPos newAnchor
    ) {
        BlockPos offset = newAnchor.subtract(anchor);
        return new BlockPortalShape(
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
        Vec3d center = innerAreaBox.getCenterVec();
        portal.updatePosition(center.x, center.y, center.z);
        
        IntBox rectanglePart = Helper.expandRectangle(
            anchor,
            blockPos -> area.contains(blockPos),
            axis
        );
        
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
        portal.axisW = Vec3d.of(wDirection.getVector());
        portal.axisH = Vec3d.of(hDirection.getVector());
        portal.width = Helper.getCoordinate(innerAreaBox.getSize(), wDirection.getAxis());
        portal.height = Helper.getCoordinate(innerAreaBox.getSize(), hDirection.getAxis());
        
        GeometryPortalShape shape = new GeometryPortalShape();
        Vec3d offset = Vec3d.of(
            Direction.get(Direction.AxisDirection.POSITIVE, axis)
                .getVector()
        ).multiply(0.5);
        
        Stream.concat(
            area.stream()
                .filter(blockPos -> !rectanglePart.contains(blockPos))
                .map(blockPos -> new IntBox(blockPos, blockPos)),
            Stream.of(rectanglePart)
        ).forEach(part -> {
            Vec3d p1 = Vec3d.of(part.l).add(offset);
            Vec3d p2 = Vec3d.of(part.h).add(1, 1, 1).add(offset);
            double p1LocalX = p1.subtract(center).dotProduct(portal.axisW);
            double p1LocalY = p1.subtract(center).dotProduct(portal.axisH);
            double p2LocalX = p2.subtract(center).dotProduct(portal.axisW);
            double p2LocalY = p2.subtract(center).dotProduct(portal.axisH);
            shape.addTriangleForRectangle(
                p1LocalX, p1LocalY,
                p2LocalX, p2LocalY
            );
        });
        
        portal.specialShape = shape;
        
        Vec3d p1 = Vec3d.of(rectanglePart.l).add(offset);
        Vec3d p2 = Vec3d.of(rectanglePart.h).add(1, 1, 1).add(offset);
        double p1LocalX = p1.subtract(center).dotProduct(portal.axisW);
        double p1LocalY = p1.subtract(center).dotProduct(portal.axisH);
        double p2LocalX = p2.subtract(center).dotProduct(portal.axisW);
        double p2LocalY = p2.subtract(center).dotProduct(portal.axisH);
        portal.initCullableRange(
            p1LocalX, p2LocalX,
            p1LocalY, p2LocalY
        );
    }
    
    public BlockPortalShape matchShapeWithMovedFirstFramePos(
        Predicate<BlockPos> isAir,
        Predicate<BlockPos> isObsidian,
        BlockPos newFirstObsidianPos,
        BlockPos.Mutable temp
    ) {
        boolean testFrame = frameAreaWithoutCorner.stream().map(blockPos1 -> temp.set(
            blockPos1.getX() - firstFramePos.getX() + newFirstObsidianPos.getX(),
            blockPos1.getY() - firstFramePos.getY() + newFirstObsidianPos.getY(),
            blockPos1.getZ() - firstFramePos.getZ() + newFirstObsidianPos.getZ()
        )).allMatch(isObsidian);
        
        if (!testFrame) {
            return null;
        }
        
        boolean testAir = area.stream().map(blockPos -> temp.set(
            blockPos.getX() - firstFramePos.getX() + newFirstObsidianPos.getX(),
            blockPos.getY() - firstFramePos.getY() + newFirstObsidianPos.getY(),
            blockPos.getZ() - firstFramePos.getZ() + newFirstObsidianPos.getZ()
        )).allMatch(isAir);
        
        if (!testAir) {
            return null;
        }
        
        BlockPos offset = newFirstObsidianPos.subtract(firstFramePos);
        return new BlockPortalShape(
            area.stream().map(
                blockPos -> blockPos.add(offset)
            ).collect(Collectors.toSet()),
            axis
        );
    }
    
    public static boolean isSquareShape(BlockPortalShape shape, int length) {
        BlockPos areaSize = shape.innerAreaBox.getSize();
        
        Pair<Direction.Axis, Direction.Axis> xs = Helper.getAnotherTwoAxis(shape.axis);
        
        return Helper.getCoordinate(areaSize, xs.getLeft()) == length &&
            Helper.getCoordinate(areaSize, xs.getRight()) == length &&
            shape.area.size() == (length * length);
    }
    
    public static BlockPortalShape getSquareShapeTemplate(
        Direction.Axis axis,
        int length
    ) {
        Pair<Direction, Direction> perpendicularDirections = Helper.getPerpendicularDirections(
            Direction.from(axis, Direction.AxisDirection.POSITIVE)
        );
        
        Set<BlockPos> area = new HashSet<>();
        
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                area.add(
                    BlockPos.ORIGIN.offset(perpendicularDirections.getLeft(), i)
                        .offset(perpendicularDirections.getRight(), j)
                );
            }
        }
        
        return new BlockPortalShape(area, axis);
    }
    
    public BlockPortalShape getShapeWithMovedTotalAreaBox(IntBox newTotalAreaBox) {
        Validate.isTrue(totalAreaBox.getSize().equals(newTotalAreaBox.getSize()));
    
        return getShapeWithMovedAnchor(
            newTotalAreaBox.l.subtract(totalAreaBox.l)
                .add(anchor)
        );
    }
}
