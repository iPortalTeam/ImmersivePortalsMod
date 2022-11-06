package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import qouteall.q_misc_util.Helper;

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
     * Note: no need to call `rectifyPortalCluster()` here.
     * @param stateBuilder Used for changing the portal state.
     * @param tickTime Tick time.
     * @param partialTicks Partial ticks. The real time is tickTime - 1 + partialTicks.
     * @return whether the animation finishes
     */
    boolean update(
        UnilateralPortalState.Builder stateBuilder,
        long tickTime,
        float partialTicks
    );
    
    /**
     * Get the ending state of the animation.
     * This is used when creating a new animation when existing animation is running.
     * @param stateBuilder Used for outputting the portal state.
     * @param tickTime World game time.
     */
    default void obtainEndingState(UnilateralPortalState.Builder stateBuilder, long tickTime) {}
}
