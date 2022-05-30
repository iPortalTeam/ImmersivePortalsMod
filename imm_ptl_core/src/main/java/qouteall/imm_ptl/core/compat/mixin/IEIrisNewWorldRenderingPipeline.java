package qouteall.imm_ptl.core.compat.mixin;

import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = NewWorldRenderingPipeline.class, remap = false)
public interface IEIrisNewWorldRenderingPipeline {
    @Accessor("isRenderingWorld")
    void ip_setIsRenderingWorld(boolean cond);
}
