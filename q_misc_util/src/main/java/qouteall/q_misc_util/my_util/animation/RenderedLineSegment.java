package qouteall.q_misc_util.my_util.animation;

import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.my_util.LineSegment;
import qouteall.q_misc_util.my_util.WithDim;

public record RenderedLineSegment(@Nullable WithDim<LineSegment> lineSegment, double scale) {
    public static final RenderedLineSegment EMPTY = new RenderedLineSegment(null, 0);
}
