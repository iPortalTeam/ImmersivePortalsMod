package qouteall.imm_ptl.core.portal.animation;

import javax.annotation.Nullable;

public record AnimationResult(
    @Nullable DeltaUnilateralPortalState delta,
    boolean isFinished
) {
}
