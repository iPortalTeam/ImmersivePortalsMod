package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nullable;
import java.util.List;

public class NormalAnimation implements PortalAnimationDriver {
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:normal"),
            NormalAnimation::deserialize
        );
    }
    
    public static class Phase {
        public long durationTicks;
        public PortalState targetState;
        public TimingFunction timingFunction;
        public boolean inverseScale;
        
        public static Phase fromTag(CompoundTag compoundTag) {
            Phase phase = new Phase();
            phase.durationTicks = compoundTag.getLong("durationTicks");
            phase.targetState = PortalState.fromTag(compoundTag.getCompound("targetState"));
            phase.timingFunction = TimingFunction.fromString(compoundTag.getString("timingFunction"));
            phase.inverseScale = compoundTag.getBoolean("inverseScale");
            return phase;
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("durationTicks", durationTicks);
            tag.put("targetState", targetState.toTag());
            tag.putString("timingFunction", timingFunction.toString());
            tag.putBoolean("inverseScale", inverseScale);
            return tag;
        }
    }
    
    public PortalState initialState;
    public List<Phase> phases;
    public boolean doRectifyCluster;
    public long startingGameTime;
    public int loopCount;
    
    // cache field, not serialized
    @Nullable
    private Long ticksPerRound;
    
    private static NormalAnimation deserialize(CompoundTag compoundTag) {
        NormalAnimation animation = new NormalAnimation();
        animation.initialState = PortalState.fromTag(compoundTag.getCompound("initialState"));
        animation.phases = Helper.listTagToList(
            Helper.getCompoundList(compoundTag, "phases"),
            Phase::fromTag
        );
        animation.doRectifyCluster = compoundTag.getBoolean("doRectifyCluster");
        animation.startingGameTime = compoundTag.getLong("startingGameTime");
        animation.loopCount = compoundTag.getInt("loopCount");
        
        if (animation.phases.isEmpty() || animation.loopCount < 0) {
            throw new RuntimeException("invalid NormalAnimation");
        }
        
        return animation;
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("type", "imm_ptl:normal");
        tag.put("initialState", initialState.toTag());
        tag.put("phases", Helper.listToListTag(phases, Phase::toTag));
        tag.putBoolean("doRectifyCluster", doRectifyCluster);
        tag.putLong("startingGameTime", startingGameTime);
        tag.putInt("loopCount", loopCount);
        
        return tag;
    }
    
    private long getTicksPerRound() {
        if (ticksPerRound == null) {
            ticksPerRound = phases.stream().mapToLong(p -> p.durationTicks).sum();
        }
        return ticksPerRound;
    }
    
    private long getTotalDuration() {
        if (loopCount > 10000) {
            return Long.MAX_VALUE;
        }
        
        return getTicksPerRound() * loopCount;
    }
    
    @Override
    public boolean update(Portal portal, long tickTime, float partialTicks) {
        double passedTicks = ((double) (tickTime - startingGameTime)) + partialTicks;
        long totalDuration = getTotalDuration();
        
        boolean ends = false;
        if (passedTicks >= totalDuration) {
            portal.setPortalState(getEndingState());
            return true;
        }
        
        // modulo works for double!
        double passedTicksInRound = passedTicks % ((double) getTicksPerRound());
        
        long traversedTicks = 0;
        PortalState phaseStartingState = initialState;
        for (Phase phase : phases) {
            if (passedTicksInRound <= traversedTicks + phase.durationTicks) {
                double phaseProgress = (passedTicksInRound - traversedTicks) / (double) phase.durationTicks;
                phaseProgress = phase.timingFunction.mapProgress(phaseProgress);
                PortalState currentState = PortalState.interpolate(
                    phaseStartingState, phase.targetState, phaseProgress, phase.inverseScale
                );
                portal.setPortalState(currentState);
                break;
            }
            traversedTicks += phase.durationTicks;
            phaseStartingState = phase.targetState;
        }
        
        return false;
    }
    
    private PortalState getEndingState() {
        Phase lastPhase = phases.get(phases.size() - 1);
        return lastPhase.targetState;
    }
    
    @Override
    public boolean shouldRectifyCluster() {
        return doRectifyCluster;
    }
    
    @Override
    public void serverSideForceStop(Portal portal, long tickTime) {
        portal.setPortalState(getEndingState());
    }
    
    public static NormalAnimation createOnePhaseAnimation(
        PortalState fromState, PortalState toState,
        long startingGameTime, long durationTicks,
        boolean inverseScale, boolean doRectifyCluster,
        TimingFunction timingFunction
    ) {
        NormalAnimation animation = new NormalAnimation();
        animation.initialState = fromState;
        animation.doRectifyCluster = doRectifyCluster;
        
        Phase phase = new Phase();
        phase.durationTicks = durationTicks;
        phase.targetState = toState;
        phase.inverseScale = inverseScale;
        phase.timingFunction = timingFunction;
        animation.phases = List.of(phase);
        
        animation.startingGameTime = startingGameTime;
        animation.loopCount = 1;
        
        return animation;
    }
}
