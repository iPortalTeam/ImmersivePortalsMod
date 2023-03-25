package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientPortalAnimationManagement {
    private static final Map<Portal, RunningDefaultAnimation> defaultAnimatedPortals = new HashMap<>();
    private static final HashSet<Portal> customAnimatedPortals = new HashSet<>();
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(ClientPortalAnimationManagement::cleanup);
        ClientWorldLoader.clientDimensionDynamicRemoveSignal.connect(dim -> cleanup());
    }
    
    public static void addDefaultAnimation(
        Portal portal,
        PortalState fromState,
        PortalState toState,
        DefaultPortalAnimation animation
    ) {
        if (animation.durationTicks <= 0) {
            return;
        }
        
        long currTime = System.nanoTime();
        RunningDefaultAnimation runningDefaultAnimation = new RunningDefaultAnimation(
            fromState,
            toState,
            currTime,
            currTime + Helper.secondToNano(animation.durationTicks / 20.0),
            animation.timingFunction,
            animation.inverseScale
        );
        defaultAnimatedPortals.put(portal, runningDefaultAnimation);
    }
    
    public static void markRequiresCustomAnimationUpdate(Portal portal) {
        customAnimatedPortals.add(portal);
        defaultAnimatedPortals.remove(portal);
    }
    
    public static void tick() {
        // update the portal state to the end of the tick
        updateCustomAnimations(true);
        
        // update the portal state to the immediate state for teleportation
        updateCustomAnimations(false);
    }
    
    public static void update() {
        long currTime = System.nanoTime();
        
        defaultAnimatedPortals.entrySet().removeIf(entry -> {
            Portal portal = entry.getKey();
            RunningDefaultAnimation animation = entry.getValue();
            
            if (portal.isRemoved()) {
                return true;
            }
            
            if (currTime > animation.toTimeNano) {
                portal.setPortalState(animation.toState);
                // animation finished
                return true;
            }
            
            PortalState currentState = animation.getCurrentState(currTime);
            
            if (currentState.fromWorld != portal.getOriginDim()) {
                // stop animation
                return true;
            }
            
            if (currentState.toWorld != portal.getDestDim()) {
                // stop animation
                return true;
            }
            
            portal.setPortalState(currentState);
            
            return false;
        });
        
        updateCustomAnimations(false);
    }
    
    private static void updateCustomAnimations(boolean isTicking) {
        long stableTickTime = StableClientTimer.getStableTickTime();
        float stablePartialTicks = StableClientTimer.getStablePartialTicks();
        
        // if isTicking, update the portal as if it's one tick later
        // due to the presence of StableClientTimer, the partialTick in ticking may be non 0
        long usedTickTime = isTicking ? stableTickTime + 1 : stableTickTime;
        float usedPartialTicks = stablePartialTicks;
        
        customAnimatedPortals.removeIf(portal -> {
            if (portal.isRemoved()) {
                return true;
            }
            
            if (!portal.animation.hasAnimationDriver()) {
                return true;
            }
            
            portal.animation.updateAnimationDriver(
                portal,
                portal.animation,
                usedTickTime,
                portal.animation.isPaused() ? 0 : usedPartialTicks,
                isTicking,
                !isTicking
                // in client, the animation is updated in two cases: ticking and before rendering a frame
                // if it's ticking, the animation should be updated as the end of the tick which is ahead of the current time
                // to make the animation stop smoothly, don't remove the animation during ticking
            );
            
            // remove the entry if animation finishes or is paused
            return !portal.animation.hasAnimationDriver();
        });
    }
    
    private static void cleanup() {
        defaultAnimatedPortals.clear();
        customAnimatedPortals.clear();
    }
    
    public static void foreachCustomAnimatedPortals(Consumer<Portal> consumer) {
        customAnimatedPortals.forEach(consumer);
    }
    
    public static class RunningDefaultAnimation {
        public PortalState fromState;
        public PortalState toState;
        public long startTimeNano;
        public long toTimeNano;
        public TimingFunction timingFunction;
        public boolean inverseScale;
        
        public RunningDefaultAnimation(
            PortalState fromState, PortalState toState, long startTimeNano, long toTimeNano,
            TimingFunction timingFunction,
            boolean inverseScale
        ) {
            this.fromState = fromState;
            this.toState = toState;
            this.startTimeNano = startTimeNano;
            this.toTimeNano = toTimeNano;
            this.timingFunction = timingFunction;
            this.inverseScale = inverseScale;
        }
        
        public PortalState getCurrentState(long currTime) {
            
            double progress = (currTime - this.startTimeNano)
                / ((double) (this.toTimeNano - this.startTimeNano));
            
            if (progress < 0) {
                Helper.err("invalid portal animation");
                progress = 0;
            }
            
            progress = this.timingFunction.mapProgress(progress);
            
            PortalState currState = PortalState.interpolate(
                this.fromState, this.toState, progress, inverseScale
            );
            
            return currState;
        }
    }
}
