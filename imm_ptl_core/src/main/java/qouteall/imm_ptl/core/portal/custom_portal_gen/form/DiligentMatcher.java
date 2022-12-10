package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.google.common.math.IntMath;
import com.mojang.math.OctahedralGroup;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix3f;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
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
        
        public IntMatrix3(OctahedralGroup t) {
            Direction d1 = t.rotate(Direction.fromAxisAndDirection(Direction.Axis.X, Direction.AxisDirection.POSITIVE));
            Direction d2 = t.rotate(Direction.fromAxisAndDirection(Direction.Axis.Y, Direction.AxisDirection.POSITIVE));
            Direction d3 = t.rotate(Direction.fromAxisAndDirection(Direction.Axis.Z, Direction.AxisDirection.POSITIVE));
            x = d1.getNormal();
            y = d2.getNormal();
            z = d3.getNormal();
        }
        
        // p * m  p is horizontal vector
        public BlockPos transform(Vec3i p) {
            return Helper.scale(x, p.getX())
                .offset(Helper.scale(y, p.getY()))
                .offset(Helper.scale(z, p.getZ()));
        }
        
        public IntMatrix3 multiply(IntMatrix3 m) {
            return new IntMatrix3(
                m.transform(x),
                m.transform(y),
                m.transform(z)
            );
        }
        
        public Direction transformDirection(Direction direction) {
            BlockPos vec = transform(direction.getNormal());
            return Direction.fromNormal(vec.getX(), vec.getY(), vec.getZ());
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntMatrix3 that = (IntMatrix3) o;
            return x.equals(that.x) &&
                y.equals(that.y) &&
                z.equals(that.z);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
        
        public static IntMatrix3 getIdentity() {
            return new IntMatrix3(
                new BlockPos(1, 0, 0),
                new BlockPos(0, 1, 0),
                new BlockPos(0, 0, 1)
            );
        }
        
        public Matrix3f toMatrix() {
            Matrix3f matrix = new Matrix3f();
            matrix.set(0, 0, x.getX());
            matrix.set(0, 1, x.getY());
            matrix.set(0, 2, x.getZ());
            
            matrix.set(1, 0, y.getX());
            matrix.set(1, 1, y.getY());
            matrix.set(1, 2, y.getZ());
            
            matrix.set(2, 0, z.getX());
            matrix.set(2, 1, z.getY());
            matrix.set(2, 2, z.getZ());
            
            return matrix;
        }
        
        public DQuaternion toQuaternion() {
            return DQuaternion.matrixToQuaternion(
                Vec3.atLowerCornerOf(x),
                Vec3.atLowerCornerOf(y),
                Vec3.atLowerCornerOf(z)
            );
        }
    }
    
    public static int dirDotProduct(Direction dir, Direction b) {
        if (dir.getAxis() != b.getAxis()) {
            return 0;
        }
        if (dir.getAxisDirection() == b.getAxisDirection()) {
            return 1;
        }
        else {
            return -1;
        }
    }
    
    // counter clockwise
    public static Direction rotateAlong(Direction dir, Direction axis) {
        Tuple<Direction, Direction> ds = Helper.getPerpendicularDirections(axis);
        Direction d1 = ds.getA();
        Direction d2 = ds.getB();
        
        int c1 = dirDotProduct(dir, d1);
        int c2 = dirDotProduct(dir, d2);
        int ca = dirDotProduct(dir, axis);
        
        int nc1 = -c2;
        int nc2 = c1;
        
        BlockPos finalVec = Helper.scale(d1.getNormal(), nc1)
            .offset(Helper.scale(d2.getNormal(), nc2))
            .offset(Helper.scale(axis.getNormal(), ca));
        return Direction.fromNormal(finalVec.getX(), finalVec.getY(), finalVec.getZ());
    }
    
    public static IntMatrix3 getRotation90(Direction direction) {
        return new IntMatrix3(
            rotateAlong(Direction.fromAxisAndDirection(Direction.Axis.X, Direction.AxisDirection.POSITIVE), direction).getNormal(),
            rotateAlong(Direction.fromAxisAndDirection(Direction.Axis.Y, Direction.AxisDirection.POSITIVE), direction).getNormal(),
            rotateAlong(Direction.fromAxisAndDirection(Direction.Axis.Z, Direction.AxisDirection.POSITIVE), direction).getNormal()
        );
    }
    
    // sorted from simpler rotations to complex rotations
    public static final List<IntMatrix3> rotationTransformations = Util.make(() -> {
        List<IntMatrix3> basicRotations = Arrays.stream(Direction.values())
            .map(DiligentMatcher::getRotation90)
            .collect(Collectors.toList());
        
        IntMatrix3 identity = IntMatrix3.getIdentity();
        
        ArrayList<IntMatrix3> rotationList = new ArrayList<>();
        Set<IntMatrix3> rotationSet = new HashSet<>();
        
        rotationList.add(identity);
        rotationSet.add(identity);
        
        for (int i = 0; i < 3; i++) {
            ArrayList<IntMatrix3> newlyAdded = new ArrayList<>();
            for (IntMatrix3 rot : rotationList) {
                for (IntMatrix3 basicRotation : basicRotations) {
                    IntMatrix3 newRot = rot.multiply(basicRotation);
                    if (!rotationSet.contains(newRot)) {
                        rotationSet.add(newRot);
                        newlyAdded.add(newRot);
                    }
                }
            }
            
            rotationList.addAll(newlyAdded);
        }
        
        Validate.isTrue(rotationList.size() == 24);
        
        return rotationList;
    });
    
    public static class TransformedShape {
        public final BlockPortalShape originalShape;
        public final BlockPortalShape transformedShape;
        public final IntMatrix3 rotation;
        public final double scale;
        
        public TransformedShape(BlockPortalShape originalShape, BlockPortalShape transformedShape, IntMatrix3 rotation, double scale) {
            this.originalShape = originalShape;
            this.transformedShape = transformedShape;
            this.rotation = rotation;
            this.scale = scale;
        }
    }
    
    public static List<TransformedShape> getMatchableShapeVariants(
        BlockPortalShape original,
        int maxShapeLen
    ) {
        List<TransformedShape> result = new ArrayList<>();
        HashSet<BlockPortalShape> shapeSet = new HashSet<>();
        
        int divFactor = getShapeShrinkFactor(original);
        
        BlockPortalShape shrinked = shrinkShapeBy(original, divFactor);
        
        BlockPos shrinkedShapeSize = shrinked.innerAreaBox.getSize();
        int shrinkedShapeLen = Math.max(shrinkedShapeSize.getX(), Math.max(shrinkedShapeSize.getY(), shrinkedShapeSize.getZ()));
        int maxMultiplyFactor = (int) Math.floor(((double) maxShapeLen) / shrinkedShapeLen);
        
        for (IntMatrix3 rotation : rotationTransformations) {
            BlockPortalShape rotatedShape = rotateShape(shrinked, rotation);
            BlockPortalShape newShape = regularizeShape(rotatedShape);
            boolean isNew = shapeSet.add(newShape);
            if (isNew) {
                result.add(new TransformedShape(
                    original, newShape, rotation, 1.0 / divFactor
                ));
                
                for (int mul = 2; mul <= maxMultiplyFactor; mul++) {
                    BlockPortalShape expanded = regularizeShape(upscaleShape(rotatedShape, mul));
                    isNew = shapeSet.add(expanded);
                    if (isNew) {
                        result.add(new TransformedShape(
                            original, expanded,
                            rotation, ((double) mul) / divFactor
                        ));
                    }
                }
            }
        }
        
        return result;
    }
    
    public static BlockPortalShape regularizeShape(BlockPortalShape rotatedShape) {
        return rotatedShape.getShapeWithMovedAnchor(BlockPos.ZERO);
    }
    
    public static BlockPortalShape rotateShape(BlockPortalShape shape, IntMatrix3 t) {
        Set<BlockPos> newArea = shape.area.stream().map(
            b -> t.transform(b)
        ).collect(Collectors.toSet());
        Direction.Axis newAxis = t.transformDirection(
            Direction.fromAxisAndDirection(shape.axis, Direction.AxisDirection.POSITIVE)
        ).getAxis();
        return new BlockPortalShape(newArea, newAxis);
    }
    
    public static int getShapeShrinkFactor(BlockPortalShape shape) {
        HashSet<BlockPos> area = new HashSet<>(shape.area);
        
        ArrayList<IntBox> boxList = decomposeShape(shape, area);
        
        IntArrayList sideLenList = new IntArrayList();
        Tuple<Direction.Axis, Direction.Axis> axs = Helper.getAnotherTwoAxis(shape.axis);
        for (IntBox box : boxList) {
            BlockPos boxSize = box.getSize();
            int a = Helper.getCoordinate(boxSize, axs.getA());
            int b = Helper.getCoordinate(boxSize, axs.getB());
            sideLenList.add(a);
            sideLenList.add(b);
        }
        
        return sideLenList.stream().reduce((a, b) -> IntMath.gcd(a, b)).get();
    }
    
    public static BlockPortalShape shrinkShapeBy(BlockPortalShape shape, int div) {
        Validate.isTrue(div != 0);
        
        BlockPortalShape regularized = regularizeShape(shape);
        
        if (div == 1) {
            return regularized;
        }
        
        Set<BlockPos> newArea = regularized.area.stream().map(
            b -> new BlockPos(
                Math.floorDiv(b.getX(), div),
                Math.floorDiv(b.getY(), div),
                Math.floorDiv(b.getZ(), div)
            )
        ).collect(Collectors.toSet());
        
        return new BlockPortalShape(newArea, regularized.axis);
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
    
    public static BlockPortalShape upscaleShape(
        BlockPortalShape shape,
        int multiplyFactor
    ) {
        Tuple<Direction.Axis, Direction.Axis> axs = Helper.getAnotherTwoAxis(shape.axis);
        Vec3i v1 = Direction.fromAxisAndDirection(axs.getA(), Direction.AxisDirection.POSITIVE).getNormal();
        Vec3i v2 = Direction.fromAxisAndDirection(axs.getB(), Direction.AxisDirection.POSITIVE).getNormal();
        
        return new BlockPortalShape(
            shape.area.stream().flatMap(
                basePos -> IntStream.range(0, multiplyFactor).boxed().flatMap(dx ->
                    IntStream.range(0, multiplyFactor).mapToObj(dy ->
                        Helper.scale(basePos, multiplyFactor)
                            .offset(Helper.scale(v1, dx).offset(Helper.scale(v2, dy)))
                    )
                )
            ).collect(Collectors.toSet()),
            shape.axis
        );
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
