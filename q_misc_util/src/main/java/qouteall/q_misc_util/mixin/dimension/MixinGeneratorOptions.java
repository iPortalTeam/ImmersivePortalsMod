package qouteall.q_misc_util.mixin.dimension;

import qouteall.imm_ptl.core.ducks.IEGeneratorOptions;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GeneratorOptions.class)
public class MixinGeneratorOptions implements IEGeneratorOptions {
    
    @Shadow
    @Final
    @Mutable
    private SimpleRegistry<DimensionOptions> options;
    
    @Override
    public void setDimOptionRegistry(SimpleRegistry<DimensionOptions> reg) {
        options = reg;
    }
    
}
