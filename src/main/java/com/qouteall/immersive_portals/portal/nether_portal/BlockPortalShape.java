package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
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

public class BlockPortalShape {
    public BlockPos anchor;
    public Set<BlockPos> area;
    public IntBox innerAreaBox;
    public IntBox totalAreaBox;
    public Direction.Axis axis;
    public Set<BlockPos> frameAreaWithoutCorner;
    public Set<BlockPos> frameAreaWithCorner;
    
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
        
        Set<BlockPos> area = new HashSet<>();
        area.add(startingPos);
        findAreaRecursively(
            startingPos,
            isAir,
            Helper.getAnotherFourDirections(axis),
            area
        );
        
        BlockPortalShape result =
            new BlockPortalShape(area, axis);
        
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
                if (isEmptyPos.test(newPos)) {
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
        
        boolean testFrame = frameAreaWithoutCorner.stream().map(
            blockPos -> temp.set(
                blockPos.getX() - anchor.getX() + newAnchor.getX(),
                blockPos.getY() - anchor.getY() + newAnchor.getY(),
                blockPos.getZ() - anchor.getZ() + newAnchor.getZ()
            )
        ).allMatch(
            isObsidian
        );
        
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
        portal.axisW = new Vec3d(wDirection.getVector());
        portal.axisH = new Vec3d(hDirection.getVector());
        portal.width = Helper.getCoordinate(innerAreaBox.getSize(), wDirection.getAxis());
        portal.height = Helper.getCoordinate(innerAreaBox.getSize(), hDirection.getAxis());
        
        GeometryPortalShape shape = new GeometryPortalShape();
        Vec3d offset = new Vec3d(
            Direction.get(Direction.AxisDirection.POSITIVE, axis)
                .getVector()
        ).multiply(0.5);
    
        Stream.concat(
            area.stream()
                .filter(blockPos -> !rectanglePart.contains(blockPos))
                .map(blockPos -> new IntBox(blockPos, blockPos)),
            Stream.of(rectanglePart)
        ).forEach(part -> {
            Vec3d p1 = new Vec3d(part.l).add(offset);
            Vec3d p2 = new Vec3d(part.h).add(1, 1, 1).add(offset);
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
        
        Vec3d p1 = new Vec3d(rectanglePart.l).add(offset);
        Vec3d p2 = new Vec3d(rectanglePart.h).add(1, 1, 1).add(offset);
        double p1LocalX = p1.subtract(center).dotProduct(portal.axisW);
        double p1LocalY = p1.subtract(center).dotProduct(portal.axisH);
        double p2LocalX = p2.subtract(center).dotProduct(portal.axisW);
        double p2LocalY = p2.subtract(center).dotProduct(portal.axisH);
        portal.initCullableRange(
            p1LocalX, p2LocalX,
            p1LocalY, p2LocalY
        );
    }
    
}
