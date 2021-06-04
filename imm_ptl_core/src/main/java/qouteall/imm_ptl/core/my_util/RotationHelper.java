package qouteall.imm_ptl.core.my_util;

import qouteall.imm_ptl.core.Helper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public class RotationHelper {
    //NOTE this will mutate a and return a
    public static Quaternion quaternionNumAdd(Quaternion a, Quaternion b) {
        a.set(
            a.getX() + b.getX(),
            a.getY() + b.getY(),
            a.getZ() + b.getZ(),
            a.getW() + b.getW()
        );
        return a;
    }
    
    //NOTE this will mutate a and reutrn a
    public static Quaternion quaternionScale(Quaternion a, float scale) {
        a.set(
            a.getX() * scale,
            a.getY() * scale,
            a.getZ() * scale,
            a.getW() * scale
        );
        return a;
    }
    
    //a quaternion is a 4d vector on 4d sphere
    //this method may mutate argument but will not change rotation
    public static Quaternion interpolateQuaternion(
        Quaternion a,
        Quaternion b,
        float t
    ) {
        a.normalize();
        b.normalize();
        
        double dot = dotProduct4d(a, b);
        
        if (dot < 0.0f) {
            a.scale(-1);
            dot = -dot;
        }
        
        double DOT_THRESHOLD = 0.9995;
        if (dot > DOT_THRESHOLD) {
            // If the inputs are too close for comfort, linearly interpolate
            // and normalize the result.
            
            Quaternion result = quaternionNumAdd(
                quaternionScale(a.copy(), 1 - t),
                quaternionScale(b.copy(), t)
            );
            result.normalize();
            return result;
        }
        
        double theta_0 = Math.acos(dot);
        double theta = theta_0 * t;
        double sin_theta = Math.sin(theta);
        double sin_theta_0 = Math.sin(theta_0);
        
        double s0 = Math.cos(theta) - dot * sin_theta / sin_theta_0;
        double s1 = sin_theta / sin_theta_0;
        
        return quaternionNumAdd(
            quaternionScale(a.copy(), (float) s0),
            quaternionScale(b.copy(), (float) s1)
        );
    }
    
    public static double dotProduct4d(Quaternion a, Quaternion b) {
        return a.getX() * b.getX() +
            a.getY() * b.getY() +
            a.getZ() * b.getZ() +
            a.getW() * b.getW();
    }
    
    public static boolean isClose(Quaternion a, Quaternion b, float valve) {
        a.normalize();
        b.normalize();
        if (a.getW() * b.getW() < 0) {
            a.scale(-1);
        }
        float da = a.getX() - b.getX();
        float db = a.getY() - b.getY();
        float dc = a.getZ() - b.getZ();
        float dd = a.getW() - b.getW();
        return da * da + db * db + dc * dc + dd * dd < valve;
    }
    
    public static Vec3d getRotated(Quaternion rotation, Vec3d vec) {
        Vec3f vector3f = new Vec3f(vec);
        vector3f.rotate(rotation);
        return new Vec3d(vector3f);
    }
    
    public static Quaternion ortholize(Quaternion quaternion) {
        if (quaternion.getW() < 0) {
            quaternion.scale(-1);
        }
        return quaternion;
    }
    
    public static Vec3d getRotatingAxis(Quaternion quaternion) {
        return new Vec3d(
            quaternion.getX(),
            quaternion.getY(),
            quaternion.getZ()
        ).normalize();
    }
    
    //naive interpolation is better?
    //not better
    public static Quaternion interpolateQuaternionNaive(
        Quaternion a,
        Quaternion b,
        float t
    ) {
        return Helper.makeIntoExpression(
            new Quaternion(
                MathHelper.lerp(t, a.getX(), b.getX()),
                MathHelper.lerp(t, a.getY(), b.getY()),
                MathHelper.lerp(t, a.getZ(), b.getZ()),
                MathHelper.lerp(t, a.getW(), b.getW())
            ),
            Quaternion::normalize
        );
    }
    
    
}
