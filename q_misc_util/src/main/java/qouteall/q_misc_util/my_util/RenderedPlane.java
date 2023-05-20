package qouteall.q_misc_util.my_util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public record RenderedPlane(
    @Nullable WithDim<Plane> plane,
    double scale
) {
    public static final RenderedPlane NONE = new RenderedPlane(null, 0);
}
