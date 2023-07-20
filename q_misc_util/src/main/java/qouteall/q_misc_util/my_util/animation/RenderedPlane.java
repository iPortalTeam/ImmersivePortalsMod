package qouteall.q_misc_util.my_util.animation;

import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;

public record RenderedPlane(
    @Nullable WithDim<Plane> plane,
    double scale
) {
    public static final RenderedPlane NONE = new RenderedPlane(null, 0);
}
