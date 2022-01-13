package qouteall.imm_ptl.core.portal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.Helper;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class PortalAnimation {
    public static enum Curve {
        linear, sine, circle
    }
    
    public final Curve curve;
    public final int durationTicks;
    
    public PortalAnimation(Curve curve, int durationTicks) {
        this.curve = curve;
        this.durationTicks = durationTicks;
    }
    
    public static final PortalAnimation defaultAnimation = new PortalAnimation(Curve.sine, 10);
    
    public static PortalAnimation fromNbt(NbtCompound nbt) {
        String c = nbt.getString("curve");
        Curve curve = switch (c) {
            case "linear" -> Curve.linear;
            case "sine" -> Curve.sine;
            case "circle" -> Curve.circle;
            default -> Curve.sine;
        };
        int durationTicks = nbt.getInt("durationTicks");
        
        return new PortalAnimation(curve, durationTicks);
    }
    
    public NbtCompound toNbt() {
        NbtCompound nbtCompound = new NbtCompound();
        nbtCompound.putString("curve", curve.toString());
        nbtCompound.putInt("durationTicks", durationTicks);
        return nbtCompound;
    }
    
    public static double mapProgress(double progress, Curve curve) {
        switch (curve) {
            
            case linear -> {return progress;}
            case sine -> {return Math.sin(progress * (Math.PI / 2));}
            case circle -> {return Math.sqrt(1 - (1 - progress) * (1 - progress));}
        }
        throw new RuntimeException();
    }
    
    public static class RunningAnimation {
        public PortalState fromState;
        public PortalState toState;
        public long startTimeNano;
        public long toTimeNano;
        public Curve curve;
        
        public RunningAnimation(PortalState fromState, PortalState toState, long startTimeNano, long toTimeNano, Curve curve) {
            this.fromState = fromState;
            this.toState = toState;
            this.startTimeNano = startTimeNano;
            this.toTimeNano = toTimeNano;
            this.curve = curve;
        }
    }
    
    private static final Map<Portal, RunningAnimation> animatedPortals = new HashMap<>();
    
    public static void init() {
        IPGlobal.preGameRenderSignal.connect(PortalAnimation::onPreGameRender);
        IPGlobal.clientCleanupSignal.connect(PortalAnimation::cleanup);
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
            animation.curve
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
            
            double progress = (currTime - animation.startTimeNano)
                / ((double) (animation.toTimeNano - animation.startTimeNano));
            
            if (progress < 0) {
                Helper.err("invalid portal animation");
                return true;
            }
            
            progress = mapProgress(progress, animation.curve);
            
            portal.setPortalState(PortalState.interpolate(
                animation.fromState, animation.toState, progress
            ));
            
            return false;
        });
    }
    
    private static void cleanup() {
        animatedPortals.clear();
    }
    
}
