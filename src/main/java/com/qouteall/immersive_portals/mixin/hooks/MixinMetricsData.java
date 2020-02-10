package com.qouteall.immersive_portals.mixin.hooks;

import com.qouteall.immersive_portals.ducks.IEMetricsData;
import net.minecraft.util.MetricsData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
