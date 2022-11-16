package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import qouteall.q_misc_util.Helper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public interface PortalAnimationDriver {
    static final Map<ResourceLocation, Function<CompoundTag, PortalAnimationDriver>> deserializerRegistry =
        new HashMap<>();
    
    public static void registerDeserializer(ResourceLocation key, Function<CompoundTag, PortalAnimationDriver> deserializer) {
        PortalAnimationDriver.deserializerRegistry.put(
            key,
            deserializer
        );
    }
    
    @Nullable
    public static PortalAnimationDriver fromTag(CompoundTag tag) {
        String type = tag.getString("type");
        Function<CompoundTag, PortalAnimationDriver> deserializer = deserializerRegistry.get(
            new ResourceLocation(type)
        );
        if (deserializer == null) {
            Helper.err("Unknown animation type " + type);
            return null;
        }
    
        try {
            return deserializer.apply(tag);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    CompoundTag toTag();
    
    /**
     * Invoked on both client side and server side.
     * On server side it's invoked during ticking.
     * On client side it's invoked both on ticking and before rendering.
     *
     * @param tickTime     Tick time.
     * @param partialTicks Partial ticks. The real time is tickTime - 1 + partialTicks.
     * @param context      The context of the animation.
     * @return The animation result
     */
    @Nonnull
    AnimationResult getAnimationResult(
        long tickTime,
        float partialTicks,
        AnimationContext context
    );
    
    /**
     * Get the ending state of the animation.
     * This is used when creating a new animation when existing animation is running.
     *
     * @param tickTime World game time.
     * @param context
     */
    @Nullable
    default DeltaUnilateralPortalState getEndingResult(long tickTime, AnimationContext context) {
        return null;
    }
    
    /**
     * @return A flipped version of this animation for the flipped portal.
     */
    default PortalAnimationDriver getFlippedVersion() {
        return this;
    }
}
