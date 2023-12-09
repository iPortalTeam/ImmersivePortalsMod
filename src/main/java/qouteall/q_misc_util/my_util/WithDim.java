package qouteall.q_misc_util.my_util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record WithDim<T>(
    ResourceKey<Level> dimension,
    T value
) {
}
