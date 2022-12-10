package qouteall.q_misc_util.mixin.dimension;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
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
