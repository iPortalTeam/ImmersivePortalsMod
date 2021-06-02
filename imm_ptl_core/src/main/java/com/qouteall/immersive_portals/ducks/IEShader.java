package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.gl.GlUniform;

import javax.annotation.Nullable;

public interface IEShader {
    @Nullable
    GlUniform ip_getClippingEquationUniform();
}
