package qouteall.imm_ptl.core.ducks;

import com.mojang.blaze3d.shaders.Uniform;

import javax.annotation.Nullable;

public interface IEShader {
    @Nullable
    Uniform ip_getClippingEquationUniform();
}
