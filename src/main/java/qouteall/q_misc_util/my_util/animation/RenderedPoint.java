package qouteall.q_misc_util.my_util.animation;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.WithDim;

public record RenderedPoint(@Nullable WithDim<Vec3> pos, double scale) {
    public static final RenderedPoint EMPTY = new RenderedPoint(null, 0);
}
