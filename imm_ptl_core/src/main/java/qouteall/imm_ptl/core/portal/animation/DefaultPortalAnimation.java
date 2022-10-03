package qouteall.imm_ptl.core.portal.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import org.apache.commons.lang3.Validate;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalState;
import qouteall.q_misc_util.Helper;

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
    
    @Environment(EnvType.CLIENT)
    public void startClientDefaultAnimation(Portal portal, PortalState animationStartState) {
        PortalState newState = portal.getPortalState();
        
        if (newState == null) {
            Helper.err("portal animation state abnormal");
            return;
        }
        
        if (newState.fromWorld != animationStartState.fromWorld ||
            newState.toWorld != animationStartState.toWorld
        ) {
            return;
        }
        
        ClientPortalAnimationManagement.addDefaultAnimation(portal, animationStartState, newState, this);
        
        // multiple animations may start at the same tick. correct the current state
        portal.setPortalState(animationStartState);
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
