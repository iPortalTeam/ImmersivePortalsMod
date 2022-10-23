package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.portal.Portal;
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
     * Note: no need to call `rectifyPortalCluster()` here.
     * @param portal
     * @param tickTime
     * @param partialTicks
     * @return whether the animation finishes
     */
    boolean update(Portal portal, long tickTime, float partialTicks);
    
    default boolean shouldRectifyCluster() {
        return true;
    }
    
    default void serverSideForceStop(Portal portal, long tickTime) {}
}
