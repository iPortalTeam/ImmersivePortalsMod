package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;

// Cloud rendering internals moved to CloudRenderer in 1.21.11.
// Keep a placeholder mixin until the optimization is re-implemented.
@Mixin(CloudRenderer.class)
public class MixinLevelRenderer_Clouds {
}
