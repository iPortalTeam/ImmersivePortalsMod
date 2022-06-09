package qouteall.imm_ptl.peripheral.mixin.common.alternate_dimension;

import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseRouterData.class)
public interface IENoiseRouterData {
    @Invoker("end")
    public static NoiseRouter ip_end(Registry<DensityFunction> registry){
        throw new RuntimeException();
    }
}
