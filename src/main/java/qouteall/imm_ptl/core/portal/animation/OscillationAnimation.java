package qouteall.imm_ptl.core.portal.animation;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.q_misc_util.Helper;

@Deprecated
public record OscillationAnimation(
    // can be non-normalized
    Vec3 vec,
    // in Hz, cycle count per second
    double frequency,
    long startGameTime,
    long cycleCount
) implements PortalAnimationDriver {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // it's currently not called
    public static void init() {
        registerDeserializer(
            new ResourceLocation("imm_ptl:oscillation"),
            OscillationAnimation::fromTag
        );
    }
    
    public static OscillationAnimation fromTag(CompoundTag tag) {
        Vec3 vec = Helper.vec3FromListTag(tag.get("vec"));
        if (vec == null) {
            LOGGER.error("Invalid oscillation animation {}", tag);
            return null;
        }
        
        double frequency = tag.getDouble("frequency");
        long startGameTime = tag.getLong("startGameTime");
        long cycleCount = tag.getLong("cycleCount");
        
        return new OscillationAnimation(
            vec, frequency, startGameTime, cycleCount
        );
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", "imm_ptl:oscillation");
        tag.put("vec", Helper.vec3ToListTag(vec));
        tag.putDouble("frequency", frequency);
        tag.putLong("startGameTime", startGameTime);
        tag.putLong("cycleCount", cycleCount);
        return tag;
    }
    
    @Override
    public @NotNull AnimationResult getAnimationResult(
        long tickTime, float partialTicks, AnimationContext context
    ) {
        double deltaTicks = (double) (tickTime - startGameTime) + partialTicks;
        double deltaSeconds = deltaTicks / 20.0;
        double deltaCycles = deltaSeconds * frequency;
        
        if (deltaCycles > cycleCount) {
            return new AnimationResult(
                null, true
            );
        }
        
        double v = Math.sin(deltaCycles * Math.PI * 2);
        
        throw new NotImplementedException();
    }
    
    @Override
    public DeltaUnilateralPortalState getEndingResult(long tickTime, AnimationContext context) {
        return DeltaUnilateralPortalState.identity;
    }
}
