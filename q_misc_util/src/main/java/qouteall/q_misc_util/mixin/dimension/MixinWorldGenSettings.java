package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import qouteall.q_misc_util.ducks.IEGeneratorOptions;

@Mixin(WorldGenSettings.class)
public class MixinWorldGenSettings implements IEGeneratorOptions {
    
//    @Shadow
//    @Final
//    @Mutable
//    private Registry<LevelStem> dimensions;
//
//    @Override
//    public void setDimOptionRegistry(MappedRegistry<LevelStem> reg) {
//        dimensions = reg;
//    }
    
}
