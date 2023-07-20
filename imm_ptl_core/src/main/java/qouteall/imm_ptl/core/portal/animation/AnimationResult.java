package qouteall.imm_ptl.core.portal.animation;

import org.jetbrains.annotations.Nullable;

public record AnimationResult(
    @Nullable DeltaUnilateralPortalState delta,
    boolean isFinished
) {
}
