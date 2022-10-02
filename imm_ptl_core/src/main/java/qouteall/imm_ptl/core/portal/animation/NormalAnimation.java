package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;

public class NormalAnimation implements PortalAnimationDriver {
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:normal"),
            NormalAnimation::deserialize
        );
    }
    
    public PortalState startingState;
    public PortalState endingState;
    public long startGameTime;
    public long endGameTime;
    public boolean doRectifyCluster;
    public boolean inverseScale;
    public TimingFunction timingFunction;
    
    private static NormalAnimation deserialize(CompoundTag compoundTag) {
        NormalAnimation normalAnimation = new NormalAnimation();
        normalAnimation.startingState = PortalState.fromTag(compoundTag.getCompound("startingState"));
        normalAnimation.endingState = PortalState.fromTag(compoundTag.getCompound("endingState"));
        normalAnimation.startGameTime = compoundTag.getLong("startGameTime");
        normalAnimation.endGameTime = compoundTag.getLong("endGameTime");
        normalAnimation.doRectifyCluster = compoundTag.getBoolean("doRectifyCluster");
        normalAnimation.inverseScale = compoundTag.getBoolean("inverseScale");
        normalAnimation.timingFunction = TimingFunction.fromString(compoundTag.getString("timingFunction"));
        return normalAnimation;
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("type", "imm_ptl:normal");
        tag.put("startingState", startingState.toTag());
        tag.put("endingState", endingState.toTag());
        tag.putLong("startGameTime", startGameTime);
        tag.putLong("endGameTime", endGameTime);
        tag.putBoolean("doRectifyCluster", doRectifyCluster);
        tag.putBoolean("inverseScale", inverseScale);
        tag.putString("timingFunction", timingFunction.toString());
        
        return tag;
    }
    
    @Override
    public boolean update(Portal portal, long tickTime, float tickDelta) {
        double passedTicks = ((double) (tickTime - startGameTime)) + tickDelta;
        
        boolean ends = false;
        if (passedTicks > (endGameTime - startGameTime)) {
            ends = true;
            passedTicks = endGameTime - startGameTime;
        }
        
        double progress = passedTicks / (endGameTime - startGameTime);
        progress = timingFunction.mapProgress(progress);
        PortalState currentState = PortalState.interpolate(startingState, endingState, progress, inverseScale);
        portal.setPortalState(currentState);
        
        return ends;
    }
    
    @Override
    public boolean shouldRectifyCluster() {
        return doRectifyCluster;
    }
    
    @Override
    public void serverSideForceStop(Portal portal, long tickTime) {
        portal.setPortalState(endingState);
    }
}
