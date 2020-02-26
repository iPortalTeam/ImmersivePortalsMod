package com.qouteall.immersive_portals.mixin;

import net.minecraft.util.profiler.ProfilerSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.time.Duration;

@Mixin(ProfilerSystem.class)
public class MixinProfilerSystem {
    @Mutable
    @Shadow
    @Final
    private static long TIMEOUT_NANOSECONDS;
    
    static {
        TIMEOUT_NANOSECONDS = Duration.ofMillis(60).toNanos();
    }
}
