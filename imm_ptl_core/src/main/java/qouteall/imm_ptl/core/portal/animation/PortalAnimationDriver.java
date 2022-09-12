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
    public static final Map<ResourceLocation, Function<CompoundTag, PortalAnimationDriver>> deserializerRegistry =
        new HashMap<>();
    
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
        
        return deserializer.apply(tag);
    }
    
    CompoundTag toTag();
    
    /**
     * @param portal
     * @param tickTime
     * @param tickDelta
     * @return whether the animation finishes
     * Note: don't need to call `rectifyPortalCluster()` here
     */
    boolean update(Portal portal, long tickTime, float tickDelta);
    
    
}
