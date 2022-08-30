package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseRouterData.class)
public interface IENoiseRouterData {
    @Invoker("end")
    public static NoiseRouter ip_end(Registry<DensityFunction> registry){
        throw new RuntimeException();
    }
    
    @Invoker("noNewCaves")
    public static NoiseRouter ip_noNewCaves(Registry<DensityFunction> registry, DensityFunction densityFunction){
        throw new RuntimeException();
    }
    
    @Invoker("slideEndLike")
    public static DensityFunction ip_slideEndLike(DensityFunction densityFunction, int i, int j) {
        throw new RuntimeException();
    }
    
    @Invoker("getFunction")
    public static DensityFunction ip_getFunction(Registry<DensityFunction> registry, ResourceKey<DensityFunction> resourceKey) {
        throw new RuntimeException();
    }
    
    @Accessor("BASE_3D_NOISE_END")
    public static ResourceKey<DensityFunction> get_BASE_3D_NOISE_END(){throw new RuntimeException();}
    
    
    
}
