package qouteall.imm_ptl.core.mixin.client.render;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.render.FrontClipping;

@Mixin(RenderType.class)
public class MixinRenderType {
    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;bindDefaultUniforms(Lcom/mojang/blaze3d/systems/RenderPass;)V"
        )
    )
    private void ip_bindDefaultUniforms(RenderPass pass) {
        RenderSystem.bindDefaultUniforms(pass);
        FrontClipping.bindClippingUniform(pass);
    }
}
