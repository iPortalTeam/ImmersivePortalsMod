package qouteall.q_misc_util.ducks;

import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;

public interface IEGeneratorOptions {
    void setDimOptionRegistry(SimpleRegistry<DimensionOptions> reg);
}
