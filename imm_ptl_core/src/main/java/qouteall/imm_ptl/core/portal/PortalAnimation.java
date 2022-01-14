package qouteall.imm_ptl.core.portal;

import net.minecraft.nbt.NbtCompound;

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
    
}
