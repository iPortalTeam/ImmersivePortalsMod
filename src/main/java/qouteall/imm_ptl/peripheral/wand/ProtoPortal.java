package qouteall.imm_ptl.peripheral.wand;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.q_misc_util.my_util.Circle;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.WithDim;

import java.util.Objects;

/**
 * Will be serialized to JSON
 */
public class ProtoPortal {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static enum Stage {
        PlacingFirstSideLeftBottom,
        PlacingFirstSideRightBottom,
        PlacingFirstSideLeftTop,
        PlacingSecondSideLeftBottom,
        PlacingSecondSideRightBottom,
        PlacingSecondSideLeftTop,
        Completed
    }
    
    @Nullable
    public ProtoPortalSide firstSide;
    
    @Nullable
    public ProtoPortalSide secondSide;
    
    public ProtoPortal() {
    
    }
    
    public ProtoPortal copy() {
        ProtoPortal newPortal = new ProtoPortal();
        newPortal.firstSide = firstSide == null ? null : firstSide.copy();
        newPortal.secondSide = secondSide == null ? null : secondSide.copy();
        return newPortal;
    }
    
    public void reset() {
        firstSide = null;
        secondSide = null;
    }
    
    public boolean isComplete() {
        return firstSide != null && secondSide != null && secondSide.isComplete();
    }
    
    public boolean isValidPlacement() {
        if (firstSide != null) {
            if (!firstSide.isValidPlacement(null)) {
                return false;
            }
            
            if (secondSide != null) {
                Validate.isTrue(firstSide.isComplete());
                double heightDivWidth = firstSide.getHeightDivWidth();
                return secondSide.isValidPlacement(heightDivWidth);
            }
        }
        
        return true;
    }
    
    @Nullable
    public ResourceKey<Level> getCursorConstraintDim() {
        if (firstSide != null && !firstSide.isComplete()) {
            return firstSide.dimension;
        }
        if (secondSide != null && !secondSide.isComplete()) {
            return secondSide.dimension;
        }
        return null;
    }
    
    @Nullable
    public WithDim<Plane> getCursorConstraintPlane() {
        if (firstSide != null) {
            WithDim<Plane> plane = firstSide.getCursorConstraintPlane();
            if (plane != null) {
                return plane;
            }
        }
        
        if (secondSide != null) {
            WithDim<Plane> plane = secondSide.getCursorConstraintPlane();
            if (plane != null) {
                return plane;
            }
        }
        
        return null;
    }
    
    @Nullable
    public WithDim<Circle> getCursorConstraintCircle() {
        if (firstSide != null && secondSide != null) {
            double heightDivWidth = firstSide.getHeightDivWidth();
            
            return secondSide.getCursorConstraintCircle(heightDivWidth);
        }
        
        return null;
    }
    
    public boolean tryPlaceCursor(ResourceKey<Level> dimension, Vec3 pos) {
        if (firstSide == null) {
            firstSide = new ProtoPortalSide(dimension, pos);
            return true;
        }
        
        if (!firstSide.isComplete()) {
            if (dimension != firstSide.dimension) {
                return false;
            }
            
            firstSide.placeCursor(pos);
            return true;
        }
        
        if (secondSide == null) {
            secondSide = new ProtoPortalSide(dimension, pos);
            return true;
        }
        
        if (!secondSide.isComplete()) {
            if (dimension != secondSide.dimension) {
                LOGGER.error("cursor dimension mismatch {} {}", dimension, secondSide.dimension);
                return false;
            }
            
            secondSide.placeCursor(pos);
            return true;
        }
        
        return false;
    }
    
    public void undo() {
        if (secondSide != null) {
            secondSide = secondSide.undo();
            return;
        }
        if (firstSide != null) {
            firstSide = firstSide.undo();
            return;
        }
    }
    
    @Nullable
    public MutableComponent getPromptMessage(
        @Nullable ProtoPortal pendingState
    ) {
        if (firstSide == null) {
            return Component.translatable("imm_ptl.wand.first_side_left_bottom");
        }
        if (firstSide.rightBottom == null) {
            String widthStr = pendingState == null ? "?" :
                String.format("%.3f", Objects.requireNonNull(pendingState.firstSide).getWidth());
    
            return Component.translatable("imm_ptl.wand.first_side_right_bottom", widthStr);
        }
        if (firstSide.leftTop == null) {
            String heightStr = pendingState == null ? "?" :
                String.format("%.3f", Objects.requireNonNull(pendingState.firstSide).getHeight());
            
            return Component.translatable("imm_ptl.wand.first_side_left_up", heightStr);
        }
    
        if (secondSide == null) {
            return Component.translatable("imm_ptl.wand.second_side_left_bottom");
        }
    
        if (secondSide.rightBottom == null) {
            String widthStr = "?";
            String heightStr = "?";
            String scaleStr = "?";
    
            if (pendingState != null) {
                Validate.notNull(pendingState.firstSide);
                Validate.notNull(pendingState.secondSide);
                double width = pendingState.secondSide.getWidth();
                double heightDivWidth = pendingState.firstSide.getHeightDivWidth();
                double height = width * heightDivWidth;
                double scale = width / pendingState.firstSide.getWidth();
                widthStr = String.format("%.3f", width);
                heightStr = String.format("%.3f", height);
                scaleStr = String.format("%.3f", scale);
            }
            
            return Component.translatable("imm_ptl.wand.second_side_right_bottom", widthStr, heightStr, scaleStr);
        }
    
        if (secondSide.leftTop == null) {
            return Component.translatable("imm_ptl.wand.second_side_left_up");
        }
    
        return null;
    }
    
    public Stage getStage() {
        if (firstSide == null) {
            return Stage.PlacingFirstSideLeftBottom;
        }
        if (firstSide.rightBottom == null) {
            return Stage.PlacingFirstSideRightBottom;
        }
        if (firstSide.leftTop == null) {
            return Stage.PlacingFirstSideLeftTop;
        }
        if (secondSide == null) {
            return Stage.PlacingSecondSideLeftBottom;
        }
        if (secondSide.rightBottom == null) {
            return Stage.PlacingSecondSideRightBottom;
        }
        if (secondSide.leftTop == null) {
            return Stage.PlacingSecondSideLeftTop;
        }
        return Stage.Completed;
    }
}
