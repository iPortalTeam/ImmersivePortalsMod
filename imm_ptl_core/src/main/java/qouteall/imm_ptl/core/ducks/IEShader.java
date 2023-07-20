package qouteall.imm_ptl.core.ducks;

import com.mojang.blaze3d.shaders.Uniform;

import org.jetbrains.annotations.Nullable;

public interface IEShader {
    @Nullable
    Uniform ip_getClippingEquationUniform();
}
