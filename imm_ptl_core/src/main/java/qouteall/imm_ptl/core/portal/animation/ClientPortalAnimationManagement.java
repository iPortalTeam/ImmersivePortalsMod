package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.Helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ClientPortalAnimationManagement {
    private static final Map<Portal, RunningAnimation> defaultAnimatedPortals = new HashMap<>();
    private static final HashSet<Portal> customAnimatedPortals = new HashSet<>();
    
    public static void init() {
        IPGlobal.preGameRenderSignal.connect(ClientPortalAnimationManagement::onPreGameRender);
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
        RunningAnimation runningAnimation = new RunningAnimation(
            fromState,
            toState,
            currTime,
            currTime + Helper.secondToNano(animation.durationTicks / 20.0),
            animation.timingFunction,
            animation.inverseScale
        );
        defaultAnimatedPortals.put(portal, runningAnimation);
    }
    
    public static void markRequiresCustomAnimationUpdate(Portal portal) {
        customAnimatedPortals.add(portal);
    }
    
    private static void onPreGameRender() {
        long currTime = System.nanoTime();
        
        defaultAnimatedPortals.entrySet().removeIf(entry -> {
            Portal portal = entry.getKey();
            RunningAnimation animation = entry.getValue();
            
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
        
        customAnimatedPortals.removeIf(portal -> {
            if (portal.isRemoved()) {
                return true;
            }
            PortalAnimationDriver animationDriver = portal.getAnimationDriver();
            if (animationDriver == null) {
                return true;
            }
            
            boolean finished = animationDriver.update(
                portal, portal.level.getGameTime(), RenderStates.tickDelta
            );
            if (animationDriver.shouldRectifyCluster()) {
                portal.rectifyClusterPortals();
            }
    
            if (finished) {
                portal.setAnimationDriver(null);
            }
            
            return finished;
        });
    }
    
    private static void cleanup() {
        defaultAnimatedPortals.clear();
        customAnimatedPortals.clear();
    }
    
    public static class RunningAnimation {
        public PortalState fromState;
        public PortalState toState;
        public long startTimeNano;
        public long toTimeNano;
        public TimingFunction timingFunction;
        public boolean inverseScale;
        
        public RunningAnimation(
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
