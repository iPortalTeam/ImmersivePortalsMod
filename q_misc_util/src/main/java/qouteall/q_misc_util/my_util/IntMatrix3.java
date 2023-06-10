package qouteall.q_misc_util.my_util;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import qouteall.q_misc_util.Helper;

import java.util.Objects;

// matrix for row vector transformation.
// usage: p * m.
// the left one gets applied first.
// Note: it's different to the MC transformation. MC transformation uses column vector. It's m * p. The right one applies first.
// Rotation-only matrices are orthogonal. Orthogonal matrices are symmetric,
// so the matrix for column vectors are the same as the matrices for row vectors for the same rotations.
public class IntMatrix3 {
    // the 3 rows of the matrix
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
    
    // `this` is applied first.
    public IntMatrix3 multiply(IntMatrix3 m) {
        return new IntMatrix3(
            m.transform(x),
            m.transform(y),
            m.transform(z)
        );
    }
    
    public Direction transformDirection(Direction direction) {
        BlockPos vec = transform(direction.getNormal());
        return Direction.fromDelta(vec.getX(), vec.getY(), vec.getZ());
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
