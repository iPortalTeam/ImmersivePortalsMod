package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.IPGlobal;

@Mixin(SectionBufferBuilderPack.class)
public class MixinSectionBufferBuilderPack {
    // The buffer can grow size on demand.
    // There is no need to allocate a large buffer at the beginning.
    // That's not an issue of vanilla,
    // but with ImmPtl, each loaded dimension will have some buffer packs.
    // This will reduce memory usage.
    // The initial size cannot be 0, because it resizes in endVertex(), not before putting data.
    @Redirect(
        method = "method_22706",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderType;bufferSize()I"
        )
    )
    private static int redirectBufferSize(RenderType instance) {
        if (!IPGlobal.saveMemoryInBufferPack) {
            return instance.bufferSize();
        }
        
        return Math.min(128, instance.bufferSize());
    }
}
