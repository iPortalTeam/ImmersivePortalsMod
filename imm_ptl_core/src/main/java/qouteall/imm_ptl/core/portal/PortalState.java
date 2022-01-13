package qouteall.imm_ptl.core.portal;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

public class PortalState {
    public RegistryKey<World> fromWorld;
    public Vec3d fromPos;
    public RegistryKey<World> toWorld;
    public Vec3d toPos;
    public double scaling;
    public DQuaternion rotation;
    public DQuaternion orientation;
    
    public PortalState(RegistryKey<World> fromWorld, Vec3d fromPos, RegistryKey<World> toWorld, Vec3d toPos, double scaling, DQuaternion rotation, DQuaternion orientation) {
        this.fromWorld = fromWorld;
        this.fromPos = fromPos;
        this.toWorld = toWorld;
        this.toPos = toPos;
        this.scaling = scaling;
        this.rotation = rotation;
        this.orientation = orientation;
    }
    
    public static PortalState interpolate(PortalState a, PortalState b, double progress) {
        Validate.isTrue(a.fromWorld == b.fromWorld);
        Validate.isTrue(a.toWorld == b.toWorld);
        
        return new PortalState(
            a.fromWorld,
            Helper.interpolatePos(a.fromPos, b.fromPos, progress),
            a.toWorld,
            Helper.interpolatePos(a.toPos, b.toPos, progress),
            MathHelper.lerp(progress, a.scaling, b.scaling),
            DQuaternion.interpolate(a.rotation, b.rotation, progress),
            DQuaternion.interpolate(a.orientation, b.orientation, progress)
        );
    }
    
}
