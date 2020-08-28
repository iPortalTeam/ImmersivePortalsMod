package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiligentMatcher {
    public static class IntMatrix3 {
        public final Vec3i x;
        public final Vec3i y;
        public final Vec3i z;
        
        public IntMatrix3(Vec3i x, Vec3i y, Vec3i z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public IntMatrix3(DirectionTransformation t) {
            Direction d1 = t.map(Direction.from(Direction.Axis.X, Direction.AxisDirection.POSITIVE));
            Direction d2 = t.map(Direction.from(Direction.Axis.Y, Direction.AxisDirection.POSITIVE));
            Direction d3 = t.map(Direction.from(Direction.Axis.Z, Direction.AxisDirection.POSITIVE));
            x = d1.getVector();
            y = d2.getVector();
            z = d3.getVector();
        }
        
        public boolean isRotationTransformation() {
            double r = Vec3d.of(x).crossProduct(Vec3d.of(y)).dotProduct(Vec3d.of(z));
            
            return r > 0;
        }
        
        public BlockPos transform(Vec3i p) {
            return Helper.scale(x, p.getX())
                .add(Helper.scale(y, p.getY()))
                .add(Helper.scale(z, p.getZ()));
        }
        
        public Direction transformDirection(Direction direction) {
            BlockPos vec = transform(direction.getVector());
            return Direction.fromVector(vec.getX(), vec.getY(), vec.getZ());
        }
    }
    
    public static final List<IntMatrix3> rotationTransformations = Util.make(() -> {
        return Arrays.stream(DirectionTransformation.values())
            .map(IntMatrix3::new)
            .filter(IntMatrix3::isRotationTransformation)
            .collect(Collectors.toList());
    });
    
    public static List<BlockPortalShape> getMatchableShapeVariants(
        BlockPortalShape original,
        int maxShapeLen
    ) {
        HashSet<BlockPortalShape> result = new HashSet<>();
        
        BlockPortalShape shrinked = shrinkShape(original);
        
        BlockPos shrinkedShapeSize = shrinked.innerAreaBox.getSize();
        int shrinkedShapeLen = Math.max(shrinkedShapeSize.getX(), Math.max(shrinkedShapeSize.getY(), shrinkedShapeSize.getZ()));
        int multiplyFactor = (int) Math.ceil(((double) maxShapeLen) / shrinkedShapeLen);
        
        for (IntMatrix3 t : rotationTransformations) {
            BlockPortalShape rotatedShape = rotateShape(shrinked, t);
            result.add(regularizeShape(rotatedShape));
            
            for (int mul = 2; mul <= multiplyFactor; mul++) {
                result.add(regularizeShape(expandShape(rotatedShape, mul)));
            }
        }
        
        return new ArrayList<>(result);
    }
    
    public static BlockPortalShape regularizeShape(BlockPortalShape rotatedShape) {
        return rotatedShape.getShapeWithMovedAnchor(BlockPos.ORIGIN);
    }
    
    public static BlockPortalShape rotateShape(BlockPortalShape shape, IntMatrix3 t) {
        Set<BlockPos> newArea = shape.area.stream().map(
            b -> t.transform(b)
        ).collect(Collectors.toSet());
        Direction.Axis newAxis = t.transformDirection(
            Direction.from(shape.axis, Direction.AxisDirection.POSITIVE)
        ).getAxis();
        return new BlockPortalShape(newArea, newAxis);
    }
    
    public static BlockPortalShape shrinkShape(BlockPortalShape shape) {
        HashSet<BlockPos> area = new HashSet<>(shape.area);
    
        ArrayList<IntBox> boxList = decomposeShape(shape, area);
    
        IntArrayList sideLenList = new IntArrayList();
        Pair<Direction.Axis, Direction.Axis> axs = Helper.getAnotherTwoAxis(shape.axis);
        for (IntBox box : boxList) {
            BlockPos boxSize = box.getSize();
            int a = Helper.getCoordinate(boxSize, axs.getLeft());
            int b = Helper.getCoordinate(boxSize, axs.getRight());
            sideLenList.add(a);
            sideLenList.add(b);
        }
        
        int div = sideLenList.stream().reduce(DiligentMatcher::getGreatestCommonDivisor).get();
        
        Validate.isTrue(div != 0);
        
        if (div == 1) {
            return shape;
        }
        
        Set<BlockPos> newArea = shape.area.stream().map(
            b -> Helper.divide(b, div)
        ).collect(Collectors.toSet());
        
        return new BlockPortalShape(newArea, shape.axis);
    }
    
    public static ArrayList<IntBox> decomposeShape(BlockPortalShape shape, HashSet<BlockPos> area) {
        ArrayList<IntBox> boxList = new ArrayList<>();
        while (!area.isEmpty()) {
            IntBox box = splitBoxFromArea(area, shape.axis);
            if (box == null) {
                break;
            }
            boxList.add(box);
        }
        return boxList;
    }
    
    public static BlockPortalShape expandShape(
        BlockPortalShape shape,
        int multiplyFactor
    ) {
        Pair<Direction.Axis, Direction.Axis> axs = Helper.getAnotherTwoAxis(shape.axis);
        Vec3i v1 = Direction.from(axs.getLeft(), Direction.AxisDirection.POSITIVE).getVector();
        Vec3i v2 = Direction.from(axs.getRight(), Direction.AxisDirection.POSITIVE).getVector();
        
        return new BlockPortalShape(
            shape.area.stream().flatMap(
                basePos -> IntStream.range(0, multiplyFactor).boxed().flatMap(dx ->
                    IntStream.range(0, multiplyFactor).mapToObj(dy ->
                        basePos.add(Helper.scale(v1, dx).add(Helper.scale(v2, dy)))
                    )
                )
            ).collect(Collectors.toSet()),
            shape.axis
        );
    }
    
    public static int getGreatestCommonDivisor(int a, int b) {
        Validate.isTrue(a > 0 && b > 0);
        if (a == b) {
            return a;
        }
        if (a < b) {
            return getGreatestCommonDivisor(b, a);
        }
        int w = a % b;
        if (w == 0) {
            return b;
        }
        return getGreatestCommonDivisor(a, w);
    }
    
    private static IntBox splitBoxFromArea(
        Set<BlockPos> area,
        Direction.Axis axis
    ) {
        Iterator<BlockPos> iterator = area.iterator();
        if (!iterator.hasNext()) {
            return null;
        }
        BlockPos firstElement = iterator.next();
        
        IntBox expanded = Helper.expandRectangle(
            firstElement,
            area::contains,
            axis
        );
        
        expanded.stream().forEach(b -> area.remove(b));
        
        return expanded;
    }
}
