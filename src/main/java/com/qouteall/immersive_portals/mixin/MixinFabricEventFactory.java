package com.qouteall.immersive_portals.mixin;

import net.fabricmc.fabric.api.event.EventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//fix fabric bug temporarily
@Mixin(EventFactory.class)
public class MixinFabricEventFactory {
    @Shadow
    private static boolean profilingEnabled;
    
    static {
        profilingEnabled = false;
    }
}
