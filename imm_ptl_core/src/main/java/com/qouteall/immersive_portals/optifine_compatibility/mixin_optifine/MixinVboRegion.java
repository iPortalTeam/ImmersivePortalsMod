package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.optifine.render.VboRegion")
public interface MixinVboRegion {
    @Invoker("deleteGlBuffers")
    void ip_deleteGlBuffers();
}
