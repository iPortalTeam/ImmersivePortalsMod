package qouteall.imm_ptl.core.mixin.common;

import net.minecraft.util.MetricsData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEMetricsData;

@Mixin(MetricsData.class)
public class MixinMetricsData implements IEMetricsData {
    @Shadow
    @Final
    private long[] samples;
    
    @Override
    public long[] getSamplesNonClientOnly() {
        return samples;
    }
}
