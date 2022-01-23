package qouteall.q_misc_util.ducks;

import net.minecraft.core.MappedRegistry;
import net.minecraft.world.level.dimension.LevelStem;

public interface IEGeneratorOptions {
    void setDimOptionRegistry(MappedRegistry<LevelStem> reg);
}
