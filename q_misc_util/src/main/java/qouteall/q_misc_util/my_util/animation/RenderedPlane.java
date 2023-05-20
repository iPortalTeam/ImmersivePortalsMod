package qouteall.q_misc_util.my_util.animation;

import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;

import javax.annotation.Nullable;

public record RenderedPlane(
    @Nullable WithDim<Plane> plane,
    double scale
) {
    public static final RenderedPlane NONE = new RenderedPlane(null, 0);
}
