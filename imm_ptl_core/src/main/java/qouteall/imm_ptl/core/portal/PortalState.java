package qouteall.imm_ptl.core.portal;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

public class PortalState {
    public final RegistryKey<World> fromWorld;
    public final Vec3d fromPos;
    public final RegistryKey<World> toWorld;
    public final Vec3d toPos;
    public final double scaling;
    public final DQuaternion rotation;
    public final DQuaternion orientation;
    public final double width;
    public final double height;
    
    public PortalState(RegistryKey<World> fromWorld, Vec3d fromPos, RegistryKey<World> toWorld, Vec3d toPos, double scaling, DQuaternion rotation, DQuaternion orientation, double width, double height) {
        this.fromWorld = fromWorld;
        this.fromPos = fromPos;
        this.toWorld = toWorld;
        this.toPos = toPos;
        this.scaling = scaling;
        this.rotation = rotation;
        this.orientation = orientation;
        this.width = width;
        this.height = height;
    }
    
    public static PortalState interpolate(
        PortalState a, PortalState b, double progress, boolean inverseScale
    ) {
        Validate.isTrue(a.fromWorld == b.fromWorld);
        Validate.isTrue(a.toWorld == b.toWorld);
        
        return new PortalState(
            a.fromWorld,
            Helper.interpolatePos(a.fromPos, b.fromPos, progress),
            a.toWorld,
            Helper.interpolatePos(a.toPos, b.toPos, progress),
            interpolateScale(a, b, progress, inverseScale),
            DQuaternion.interpolate(a.rotation, b.rotation, progress),
            DQuaternion.interpolate(a.orientation, b.orientation, progress),
            MathHelper.lerp(progress, a.width, b.width),
            MathHelper.lerp(progress, a.height, b.height)
        );
    }
    
    private static double interpolateScale(PortalState a, PortalState b, double progress, boolean inverseScale) {
        if (inverseScale) {
            return 1.0 / (MathHelper.lerp(progress, 1.0 / a.scaling, 1.0 / b.scaling));
        }
        else {
            return MathHelper.lerp(progress, a.scaling, b.scaling);
        }
    }
    
}
