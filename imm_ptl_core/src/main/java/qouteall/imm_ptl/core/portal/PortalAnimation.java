package qouteall.imm_ptl.core.portal;

import net.minecraft.nbt.CompoundTag;

public class PortalAnimation {
    public static enum Curve {
        linear, sine, circle
    }
    
    public final Curve curve;
    public final int durationTicks;
    public final boolean inverseScale;
    
    public PortalAnimation(Curve curve, int durationTicks, boolean inverseScale) {
        this.curve = curve;
        this.durationTicks = durationTicks;
        this.inverseScale = inverseScale;
    }
    
    public static final PortalAnimation defaultAnimation =
        new PortalAnimation(Curve.sine, 10, false);
    
    public static PortalAnimation fromNbt(CompoundTag nbt) {
        String c = nbt.getString("curve");
        Curve curve = switch (c) {
            case "linear" -> Curve.linear;
            case "sine" -> Curve.sine;
            case "circle" -> Curve.circle;
            default -> Curve.sine;
        };
        int durationTicks = nbt.getInt("durationTicks");
        boolean inverseScale = nbt.getBoolean("inverseScale");
        
        return new PortalAnimation(curve, durationTicks, inverseScale);
    }
    
    public CompoundTag toNbt() {
        CompoundTag nbtCompound = new CompoundTag();
        nbtCompound.putString("curve", curve.toString());
        nbtCompound.putInt("durationTicks", durationTicks);
        nbtCompound.putBoolean("inverseScale", inverseScale);
        return nbtCompound;
    }
    
    public PortalAnimation updateInverseScale(boolean newValue) {
        return new PortalAnimation(curve, durationTicks, newValue);
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
