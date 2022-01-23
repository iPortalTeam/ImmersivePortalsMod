package qouteall.q_misc_util.my_util;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import qouteall.q_misc_util.Helper;

public class RotationHelper {
    //NOTE this will mutate a and return a
    public static Quaternion quaternionNumAdd(Quaternion a, Quaternion b) {
        a.set(
            a.i() + b.i(),
            a.j() + b.j(),
            a.k() + b.k(),
            a.r() + b.r()
        );
        return a;
    }
    
    //NOTE this will mutate a and reutrn a
    public static Quaternion quaternionScale(Quaternion a, float scale) {
        a.set(
            a.i() * scale,
            a.j() * scale,
            a.k() * scale,
            a.r() * scale
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
            a.mul(-1);
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
        return a.i() * b.i() +
            a.j() * b.j() +
            a.k() * b.k() +
            a.r() * b.r();
    }
    
    public static boolean isClose(Quaternion a, Quaternion b, float valve) {
        a.normalize();
        b.normalize();
        if (a.r() * b.r() < 0) {
            a.mul(-1);
        }
        float da = a.i() - b.i();
        float db = a.j() - b.j();
        float dc = a.k() - b.k();
        float dd = a.r() - b.r();
        return da * da + db * db + dc * dc + dd * dd < valve;
    }
    
    public static Vec3 getRotated(Quaternion rotation, Vec3 vec) {
        Vector3f vector3f = new Vector3f(vec);
        vector3f.transform(rotation);
        return new Vec3(vector3f);
    }
    
    public static Quaternion ortholize(Quaternion quaternion) {
        if (quaternion.r() < 0) {
            quaternion.mul(-1);
        }
        return quaternion;
    }
    
    public static Vec3 getRotatingAxis(Quaternion quaternion) {
        return new Vec3(
            quaternion.i(),
            quaternion.j(),
            quaternion.k()
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
                Mth.lerp(t, a.i(), b.i()),
                Mth.lerp(t, a.j(), b.j()),
                Mth.lerp(t, a.k(), b.k()),
                Mth.lerp(t, a.r(), b.r())
            ),
            Quaternion::normalize
        );
    }
    
    
}
