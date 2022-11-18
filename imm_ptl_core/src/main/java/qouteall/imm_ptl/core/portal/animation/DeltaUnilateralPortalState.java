package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Vec2d;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public record DeltaUnilateralPortalState(
    @Nullable Vec3 offset,
    @Nullable DQuaternion rotation,
    @Nullable Vec2d sizeScaling
) {
    public static final DeltaUnilateralPortalState identity =
        new DeltaUnilateralPortalState(null, null, null);
    
    public DeltaUnilateralPortalState getInverse() {
        return new DeltaUnilateralPortalState(
            offset == null ? null : offset.scale(-1),
            rotation == null ? null : rotation.getConjugated(),
            sizeScaling == null ? null : new Vec2d(1.0 / sizeScaling.x(), 1.0 / sizeScaling.y())
        );
    }
    
    public DeltaUnilateralPortalState combine(DeltaUnilateralPortalState then) {
        return new DeltaUnilateralPortalState(
            Helper.combineNullable(
                offset, then.offset, Vec3::add
            ),
            Helper.combineNullable(
                rotation, then.rotation, DQuaternion::hamiltonProduct
            ),
            Helper.combineNullable(
                sizeScaling, then.sizeScaling, (a, b) -> new Vec2d(a.x() * b.x(), a.y() * b.y())
            )
        );
    }
    
    public static DeltaUnilateralPortalState fromTag(CompoundTag tag) {
        return new DeltaUnilateralPortalState(
            Helper.getVec3dOptional(tag, "offset"),
            tag.contains("rotation") ? DQuaternion.fromTag(tag.getCompound("rotation")) : null,
            tag.contains("sizeScalingX") ? new Vec2d(
                tag.getDouble("sizeScalingX"),
                tag.getDouble("sizeScalingY")
            ) : null
        );
    }
    
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (offset != null) {
            Helper.putVec3d(tag, "offset", offset);
        }
        if (rotation != null) {
            tag.put("rotation", rotation.toTag());
        }
        if (sizeScaling != null) {
            tag.putDouble("sizeScalingX", sizeScaling.x());
            tag.putDouble("sizeScalingY", sizeScaling.y());
        }
        return tag;
    }
    
    public DeltaUnilateralPortalState getPartial(double progress) {
        return new DeltaUnilateralPortalState(
            offset == null ? null : offset.scale(progress),
            rotation == null ? null : DQuaternion.interpolate(DQuaternion.identity, rotation, progress),
            sizeScaling == null ? null : new Vec2d(
                Mth.lerp(progress, 1, sizeScaling.x()),
                Mth.lerp(progress, 1, sizeScaling.y())
            )
        );
    }
    
    public static DeltaUnilateralPortalState interpolate(
        DeltaUnilateralPortalState a, DeltaUnilateralPortalState b,
        double progress
    ) {
        return a.getPartial(1 - progress).combine(b.getPartial(progress));
    }
    
    public DeltaUnilateralPortalState getFlipped() {
        return new DeltaUnilateralPortalState(
            offset,
            rotation == null ? null : rotation.hamiltonProduct(UnilateralPortalState.flipAxisH),
            sizeScaling
        );
    }
    
    public void apply(UnilateralPortalState.Builder builder) {
        if (offset != null) {
            builder.offset(offset);
        }
        if (rotation != null) {
            builder.rotate(rotation);
        }
        if (sizeScaling != null) {
            builder.scaleWidth(sizeScaling.x());
            builder.scaleHeight(sizeScaling.y());
        }
    }
    
    public DeltaUnilateralPortalState purgeFPError() {
        Vec3 offset = this.offset;
        if (offset != null && offset.lengthSqr() < 0.0001) {
            offset = null;
        }
        
        DQuaternion rotation = this.rotation;
        if (rotation != null && DQuaternion.isClose(rotation, DQuaternion.identity, 0.0001)) {
            rotation = null;
        }
        
        Vec2d sizeScaling = this.sizeScaling;
        if (sizeScaling != null
            && Math.abs(sizeScaling.x() - 1) < 0.00001
            && Math.abs(sizeScaling.y() - 1) < 0.00001
        ) {
            sizeScaling = null;
        }
        
        return new DeltaUnilateralPortalState(offset, rotation, sizeScaling);
    }
    
    public boolean isIdentity() {
        return offset == null && rotation == null && sizeScaling == null;
    }
    
    @Override
    public String toString() {
        String str = Stream.of(
            offset == null ? null : "offset=(%.3f,%.3f,%.3f)".formatted(
                offset.x(), offset.y(), offset.z()
            ),
            rotation == null ? null : "rotation=(%.3f,%.3f,%.3f,%.3f)".formatted(
                rotation.x, rotation.y, rotation.z, rotation.w
            ),
            sizeScaling == null ? null : "sizeScaling=(%.3f,%.3f)".formatted(
                sizeScaling.x(), sizeScaling.y()
            )
        ).filter(Objects::nonNull).reduce((a, b) -> a + ", " + b).orElse("");
        return "Delta(" + str + ')';
    }
    
    public static class Builder {
        @Nullable
        private Vec3 offset;
        @Nullable
        private DQuaternion rotation;
        @Nullable
        private Vec2d sizeScaling;
        
        public Builder offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }
        
        public Builder rotate(DQuaternion rotation) {
            this.rotation = rotation;
            return this;
        }
        
        public Builder scaleSize(double scale) {
            this.sizeScaling = new Vec2d(scale, scale);
            return this;
        }
        
        public Builder scaleSize(Vec2d scale) {
            this.sizeScaling = scale;
            return this;
        }
        
        public DeltaUnilateralPortalState build() {
            return new DeltaUnilateralPortalState(offset, rotation, sizeScaling);
        }
    }
}
