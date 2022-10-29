package qouteall.imm_ptl.core.portal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.McHelper;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.my_util.DQuaternion;

/**
 * The animatable states of a portal.
 */
public class PortalState {
    public final ResourceKey<Level> fromWorld;
    public final Vec3 fromPos;
    public final ResourceKey<Level> toWorld;
    public final Vec3 toPos;
    public final double scaling;
    public final DQuaternion rotation;
    public final DQuaternion orientation;
    public final double width;
    public final double height;
    
    public PortalState(ResourceKey<Level> fromWorld, Vec3 fromPos, ResourceKey<Level> toWorld, Vec3 toPos, double scaling, DQuaternion rotation, DQuaternion orientation, double width, double height) {
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
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("fromWorld", fromWorld.location().toString());
        tag.putString("toWorld", toWorld.location().toString());
        Helper.putVec3d(tag, "fromPos", fromPos);
        Helper.putVec3d(tag, "toPos", toPos);
        tag.putDouble("scaling", scaling);
        tag.putDouble("width", width);
        tag.putDouble("height", height);
        tag.put("rotation", rotation.toTag());
        tag.put("orientation", orientation.toTag());
        return tag;
    }
    
    public static PortalState fromTag(CompoundTag tag) {
        ResourceKey<Level> fromWorld = DimId.idToKey(tag.getString("fromWorld"));
        ResourceKey<Level> toWorld = DimId.idToKey(tag.getString("toWorld"));
        Vec3 fromPos = Helper.getVec3d(tag, "fromPos");
        Vec3 toPos = Helper.getVec3d(tag, "toPos");
        double scaling = tag.getDouble("scaling");
        double width = tag.getDouble("width");
        double height = tag.getDouble("height");
        DQuaternion rotation = DQuaternion.fromTag(tag.getCompound("rotation"));
        DQuaternion orientation = DQuaternion.fromTag(tag.getCompound("orientation"));
        return new PortalState(
            fromWorld, fromPos, toWorld, toPos, scaling, rotation, orientation, width, height
        );
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
            Mth.lerp(progress, a.width, b.width),
            Mth.lerp(progress, a.height, b.height)
        );
    }
    
    private static double interpolateScale(PortalState a, PortalState b, double progress, boolean inverseScale) {
        if (inverseScale) {
            return 1.0 / (Mth.lerp(progress, 1.0 / a.scaling, 1.0 / b.scaling));
        }
        else {
            return Mth.lerp(progress, a.scaling, b.scaling);
        }
    }
    
    public Vec3 getPointOnSurface(double localX, double localY) {
        Vec3 axisW = McHelper.getAxisWFromOrientation(orientation);
        Vec3 axisH = McHelper.getAxisHFromOrientation(orientation);
        
        return axisW.scale(localX).add(axisH.scale(localY)).add(fromPos);
    }
    
    /**
     * NOTE this does not consider mirror.
     * This should only be used for teleportation calculation. (Mirrors cannot teleport)
     */
    public Vec3 transformPoint(Vec3 pos) {
        Vec3 offset = pos.subtract(fromPos);
        Vec3 rotated = rotation.rotate(offset);
        Vec3 scaled = rotated.scale(scaling);
        return scaled.add(toPos);
    }
    
    /**
     * NOTE this does not consider mirror.
     * This should only be used for teleportation calculation. (Mirrors cannot teleport)
     */
    public Vec3 transformVec(Vec3 vec) {
        Vec3 rotated = rotation.rotate(vec);
        Vec3 scaled = rotated.scale(scaling);
        return scaled;
    }
    
    // the returned pos is in a portal-local coordinate where X is axisW, Y is axisH and Z is normal
    public Vec3 worldPosToPortalLocalPos(Vec3 worldPos) {
        Vec3 axisW = McHelper.getAxisWFromOrientation(this.orientation);
        Vec3 axisH = McHelper.getAxisHFromOrientation(this.orientation);
        Vec3 origin = this.fromPos;
        Vec3 offset = worldPos.subtract(origin);
        return new Vec3(
            offset.dot(axisW),
            offset.dot(axisH),
            offset.dot(axisW.cross(axisH))
        );
    }
    
    public Vec3 portalLocalPosToWorldPos(Vec3 portalLocalPos) {
        Vec3 axisW = McHelper.getAxisWFromOrientation(this.orientation);
        Vec3 axisH = McHelper.getAxisHFromOrientation(this.orientation);
        Vec3 origin = this.fromPos;
        return origin
            .add(axisW.scale(portalLocalPos.x))
            .add(axisH.scale(portalLocalPos.y))
            .add(axisW.cross(axisH).scale(portalLocalPos.z));
    }
    
    public Vec3 getNormal() {
        return McHelper.getNormalFromOrientation(this.orientation);
    }
    
    public Vec3 getContentDirection() {
        return rotation.rotate(getNormal().scale(-1));
    }
    
}
