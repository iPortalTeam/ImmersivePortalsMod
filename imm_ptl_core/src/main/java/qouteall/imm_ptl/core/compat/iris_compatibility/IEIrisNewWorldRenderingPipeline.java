package qouteall.imm_ptl.core.compat.iris_compatibility;

import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.shadows.ShadowRenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public interface IEIrisNewWorldRenderingPipeline {
    void ip_setIsRenderingWorld(boolean cond);
    
    ShadowRenderTargets ip_getShadowRenderTargets();
}
