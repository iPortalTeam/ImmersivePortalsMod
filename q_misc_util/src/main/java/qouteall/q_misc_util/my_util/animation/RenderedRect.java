package qouteall.q_misc_util.my_util.animation;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import qouteall.q_misc_util.my_util.DQuaternion;

import java.util.Arrays;
import java.util.Comparator;

public record RenderedRect(
    ResourceKey<Level> dimension,
    Vec3 center,
    DQuaternion orientation,
    double width,
    double height
) {
    /**
     * The rect has 8 invariants:
     * - Identity
     * - Rotate 90
     * - Rotate 180 (same as flip X and Y)
     * - Rotate 270 (same as rotate -90)
     * - Flip X
     * - Flip X then Rotate 90
     * - Flip X then Rotate 180
     * - Flip X then Rotate 270
     * Get the invariant whiches orientation is the closet to the target orientation.
     */
    public RenderedRect turnToClosestTo(
        DQuaternion targetOrientation
    ) {
        RenderedRect[] candidates = new RenderedRect[]{
            this,
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(0, 0, 1), 90
                )), height, width
            ),
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(0, 0, 1), 180
                )), width, height
            ),
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(0, 0, 1), 270
                )), height, width
            ),
            
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(1, 0, 0), 180
                )), width, height
            ),
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(0, 0, 1), 90
                )).hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(1, 0, 0), 180
                )), height, width
            ),
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(0, 0, 1), 180
                )).hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(1, 0, 0), 180
                )), width, height
            ),
            new RenderedRect(
                dimension, center,
                orientation.hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(0, 0, 1), 270
                )).hamiltonProduct(DQuaternion.rotationByDegrees(
                    new Vec3(1, 0, 0), 180
                )), height, width
            )
        };
        
        return Arrays.stream(candidates)
            .min(Comparator.comparingDouble(
                r -> DQuaternion.distance(r.orientation, targetOrientation)
            )).orElseThrow();
    }
}
