package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class PortalAnimationManagement {
    private static final Map<Portal, RunningAnimation> animatedPortals = new HashMap<>();
    
    public static void init() {
        IPGlobal.preGameRenderSignal.connect(PortalAnimationManagement::onPreGameRender);
        IPGlobal.clientCleanupSignal.connect(PortalAnimationManagement::cleanup);
        ClientWorldLoader.clientDimensionDynamicRemoveSignal.connect(dim -> cleanup());
    }
    
    public static void addAnimation(
        Portal portal,
        PortalState fromState,
        PortalState toState,
        PortalAnimation animation
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
            animation.curve,
            animation.inverseScale
        );
        animatedPortals.put(portal, runningAnimation);
    }
    
    private static void onPreGameRender() {
        long currTime = System.nanoTime();
        
        animatedPortals.entrySet().removeIf(entry -> {
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
    }
    
    private static void cleanup() {
        animatedPortals.clear();
    }
    
    public static class RunningAnimation {
        public PortalState fromState;
        public PortalState toState;
        public long startTimeNano;
        public long toTimeNano;
        public PortalAnimation.Curve curve;
        public boolean inverseScale;
        
        public RunningAnimation(
            PortalState fromState, PortalState toState, long startTimeNano, long toTimeNano,
            PortalAnimation.Curve curve,
            boolean inverseScale
        ) {
            this.fromState = fromState;
            this.toState = toState;
            this.startTimeNano = startTimeNano;
            this.toTimeNano = toTimeNano;
            this.curve = curve;
            this.inverseScale = inverseScale;
        }
        
        public PortalState getCurrentState(long currTime) {
            
            double progress = (currTime - this.startTimeNano)
                / ((double) (this.toTimeNano - this.startTimeNano));
            
            if (progress < 0) {
                Helper.err("invalid portal animation");
                progress = 0;
            }
            
            progress = PortalAnimation.mapProgress(progress, this.curve);
            
            PortalState currState = PortalState.interpolate(
                this.fromState, this.toState, progress, inverseScale
            );
            
            return currState;
        }
    }
}
