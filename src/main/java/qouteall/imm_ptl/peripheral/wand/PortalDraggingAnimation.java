package qouteall.imm_ptl.peripheral.wand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.portal.animation.AnimationContext;
import qouteall.imm_ptl.core.portal.animation.AnimationResult;
import qouteall.imm_ptl.core.portal.animation.DeltaUnilateralPortalState;
import qouteall.imm_ptl.core.portal.animation.PortalAnimationDriver;
import qouteall.imm_ptl.core.portal.animation.TimingFunction;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.q_misc_util.Helper;

@Deprecated
public class PortalDraggingAnimation implements PortalAnimationDriver {
    
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:dragging"),
            PortalDraggingAnimation::deserialize
        );
    }
    
    private final Info info;
    
    public PortalDraggingAnimation(Info info) {
        this.info = info;
    }
    
    public static record Info(
        PortalWandInteraction.DraggingInfo draggingInfo,
        UnilateralPortalState beforeState,
        Vec3 lastCursorPos,
        Vec3 currCursorPos,
        long startGameTime,
        long endGameTime
    ) {
    
    }
    
    private static PortalDraggingAnimation deserialize(CompoundTag compoundTag) {
        String json = compoundTag.getString("json");
        Info info = IPGlobal.gson.fromJson(json, Info.class);
        return new PortalDraggingAnimation(info);
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "imm_ptl:dragging");
        tag.putString("json", IPGlobal.gson.toJson(info));
        return tag;
    }
    
    @NotNull
    @Override
    public AnimationResult getAnimationResult(long tickTime, float partialTicks, AnimationContext context) {
        double passedTicks = ((double) (tickTime - 1 - info.startGameTime())) + partialTicks;
        double duration = info.endGameTime() - info.startGameTime();
        
        DeltaUnilateralPortalState result = null;

        double progress = Mth.clamp(passedTicks / duration, 0, 1);
        progress = TimingFunction.circle.mapProgress(progress);
        
        Vec3 cursorPos = info.lastCursorPos().lerp(info.currCursorPos(), progress);
        
        UnilateralPortalState resultState = PortalWandInteraction.applyDrag(
            info.beforeState, cursorPos, info.draggingInfo
        );
        
        if (resultState != null) {
            if (!resultState.orientation().isValid()) {
                Helper.err("invalid orientation in dragging animation");
            }
            
            result = DeltaUnilateralPortalState.fromDiff(
                info.beforeState, resultState
            );
        }
        
        return new AnimationResult(
            result, passedTicks >= duration
        );
    }
    
    @Nullable
    @Override
    public DeltaUnilateralPortalState getEndingResult(long tickTime, AnimationContext context) {
        UnilateralPortalState resultState = PortalWandInteraction.applyDrag(
            info.beforeState, info.currCursorPos(), info.draggingInfo
        );
        
        if (resultState != null) {
            if (!resultState.orientation().isValid()) {
                Helper.err("invalid orientation in dragging animation");
            }
            
            return DeltaUnilateralPortalState.fromDiff(
                info.beforeState, resultState
            );
        }
        else {
            return null;
        }
    }
}
