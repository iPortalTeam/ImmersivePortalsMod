package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;

public class DefaultPortalAnimation {
    
    public TimingFunction timingFunction;
    public int durationTicks;
    public boolean inverseScale;
    
    public DefaultPortalAnimation(TimingFunction timingFunction, int durationTicks, boolean inverseScale) {
        this.timingFunction = timingFunction;
        this.durationTicks = durationTicks;
        this.inverseScale = inverseScale;
    }
    
    public static DefaultPortalAnimation createDefault() {
        return new DefaultPortalAnimation(TimingFunction.sine, 10, false);
    }
    
    public static DefaultPortalAnimation fromNbt(CompoundTag nbt) {
        String c = nbt.getString("curve");
        TimingFunction timingFunction = TimingFunction.fromString(c);
        int durationTicks = nbt.getInt("durationTicks");
        boolean inverseScale = nbt.getBoolean("inverseScale");
        
        return new DefaultPortalAnimation(timingFunction, durationTicks, inverseScale);
    }
    
    public CompoundTag toNbt() {
        CompoundTag nbtCompound = new CompoundTag();
        nbtCompound.putString("curve", timingFunction.toString());
        nbtCompound.putInt("durationTicks", durationTicks);
        nbtCompound.putBoolean("inverseScale", inverseScale);
        return nbtCompound;
    }
    
    public DefaultPortalAnimation copy() {
        return new DefaultPortalAnimation(timingFunction, durationTicks, inverseScale);
    }
    
}
