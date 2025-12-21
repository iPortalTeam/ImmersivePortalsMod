package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = RenderSystem.class, remap = false)
public class MixinRenderSystem_Clipping {
}
