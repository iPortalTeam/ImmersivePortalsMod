package qouteall.imm_ptl.core.optifine_compatibility.mixin_optifine;

import net.optifine.shaders.ShadersRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(value = ShadersRender.class, remap = false)
public class MixinShadersRender {

}
