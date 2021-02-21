package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.optifine.Config", remap = false)
public interface IEOFConfig {
    @Invoker("isVbo")
    public static boolean ip_isVbo() {
        throw new RuntimeException();
    }
    
    @Invoker("isRenderRegions")
    public static boolean ip_isRenderRegions() {
        throw new RuntimeException();
    }
}
