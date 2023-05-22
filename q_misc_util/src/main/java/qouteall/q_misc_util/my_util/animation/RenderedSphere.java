package qouteall.q_misc_util.my_util.animation;

import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Sphere;
import qouteall.q_misc_util.my_util.WithDim;

import javax.annotation.Nullable;

public record RenderedSphere(
    @Nullable WithDim<Sphere> sphere,
    DQuaternion orientation,
    double scale
) {
    public static final RenderedSphere NONE = new RenderedSphere(
        null, DQuaternion.identity, 0
    );
}
