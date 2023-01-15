package qouteall.q_misc_util.my_util;


import com.mojang.math.OctahedralGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

import java.util.Arrays;

/**
 * Axis-Aligned Rotations.
 * Vanilla's {@link OctahedralGroup} contains the mirrored transformations. This only contain rotations.
 */
public enum AARotation {
    
    SOUTH_ROT0(Direction.SOUTH, Direction.EAST),
    SOUTH_ROT90(Direction.SOUTH, Direction.UP),
    SOUTH_ROT180(Direction.SOUTH, Direction.WEST),
    SOUTH_ROT270(Direction.SOUTH, Direction.DOWN),
    
    NORTH_ROT0(Direction.NORTH, Direction.WEST),
    NORTH_ROT90(Direction.NORTH, Direction.UP),
    NORTH_ROT180(Direction.NORTH, Direction.EAST),
    NORTH_ROT270(Direction.NORTH, Direction.DOWN),
    
    EAST_ROT0(Direction.EAST, Direction.NORTH),
    EAST_ROT90(Direction.EAST, Direction.UP),
    EAST_ROT180(Direction.EAST, Direction.SOUTH),
    EAST_ROT270(Direction.EAST, Direction.DOWN),
    
    WEST_ROT0(Direction.WEST, Direction.SOUTH),
    WEST_ROT90(Direction.WEST, Direction.UP),
    WEST_ROT180(Direction.WEST, Direction.NORTH),
    WEST_ROT270(Direction.WEST, Direction.DOWN),
    
    UP_ROT0(Direction.UP, Direction.NORTH),
    UP_ROT90(Direction.UP, Direction.WEST),
    UP_ROT180(Direction.UP, Direction.SOUTH),
    UP_ROT270(Direction.UP, Direction.EAST),
    
    DOWN_ROT0(Direction.DOWN, Direction.SOUTH),
    DOWN_ROT90(Direction.DOWN, Direction.WEST),
    DOWN_ROT180(Direction.DOWN, Direction.NORTH),
    DOWN_ROT270(Direction.DOWN, Direction.EAST);
    
    public static final AARotation IDENTITY = SOUTH_ROT0;
    
    public final Direction transformedX;
    public final Direction transformedY;
    public final Direction transformedZ;
    public final IntMatrix3 matrix;
    
    
    AARotation(Direction transformedZ, Direction transformedX) {
        this.transformedZ = transformedZ;
        this.transformedX = transformedX;
        this.transformedY = dirCrossProduct(transformedZ, transformedX);
        matrix = new IntMatrix3(
            this.transformedX.getNormal(),
            this.transformedY.getNormal(),
            this.transformedZ.getNormal()
        );
    }
    
    public BlockPos transform(Vec3i vec) {
        return matrix.transform(vec);
    }
    
    public Direction transformDirection(Direction direction) {
        return Direction.fromNormal(transform(direction.getNormal()));
    }
    
    public static Direction dirCrossProduct(Direction a, Direction b) {
        return Direction.fromNormal(
            a.getStepY() * b.getStepZ() - a.getStepZ() * b.getStepY(),
            a.getStepZ() * b.getStepX() - a.getStepX() * b.getStepZ(),
            a.getStepX() * b.getStepY() - a.getStepY() * b.getStepX()
        );
    }
    
    private static final AARotation[][] multiplicationCache = new AARotation[24][24];
    
    static {
        for (AARotation a : values()) {
            for (AARotation b : values()) {
                multiplicationCache[a.ordinal()][b.ordinal()] = a.rawMultiply(b);
            }
        }
    }
    
    // firstly apply other, then apply this
    public AARotation multiply(AARotation other) {
        return multiplicationCache[this.ordinal()][other.ordinal()];
    }
    
    // firstly apply other, then apply this
    private AARotation rawMultiply(AARotation other) {
        return getAARotationFromZX(
            transformDirection(other.transformedZ),
            transformDirection(other.transformedX)
        );
    }
    
    public static AARotation getAARotationFromZX(Direction transformedZ, Direction transformedX) {
        for (AARotation value : values()) {
            if (value.transformedZ == transformedZ && value.transformedX == transformedX) {
                return value;
            }
        }
        throw new IllegalArgumentException();
    }
    
    public static AARotation getAARotationFromYZ(Direction transformedY, Direction transformedZ) {
        for (AARotation value : values()) {
            if (value.transformedY == transformedY && value.transformedZ == transformedZ) {
                return value;
            }
        }
        throw new IllegalArgumentException();
    }
    
    public static AARotation getAARotationFromXY(Direction transformedX, Direction transformedY) {
        for (AARotation value : values()) {
            if (value.transformedX == transformedX && value.transformedY == transformedY) {
                return value;
            }
        }
        throw new IllegalArgumentException();
    }
    
    private static final AARotation[] inverseCache = new AARotation[24];
    
    static {
        AARotation[] values = values();
        for (int i = 0; i < values.length; i++) {
            AARotation rot = values[i];
            // brute force inverse
            AARotation inverse = Arrays.stream(values())
                .filter(b -> rot.multiply(b) == IDENTITY).findFirst().orElseThrow();
            inverseCache[i] = inverse;
        }
    }
    
    public AARotation getInverse() {
        return inverseCache[this.ordinal()];
    }
}

