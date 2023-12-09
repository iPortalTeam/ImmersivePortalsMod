package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.mixin.client.render.optimization.MixinSectionBufferBuilderPack;

@Mixin(SectionRenderDispatcher.class)
public class MixinSectionRenderDispatcher {
    @Shadow
    @Final
    private SectionBufferBuilderPool bufferPool;
    
    /**
     * When loading multiple client dimensions at the same time,
     * there will be multiple {@link SectionRenderDispatcher} instances.
     * They cannot share one {@link SectionBufferBuilderPool} instance because
     * that type is not thread-safe.
     * In {@link MixinSectionBufferBuilderPack} it reduces the initial size of the buffer
     * to reduce memory overhead.
     * This is not enabled in Sodium as Sodium does not use this.
     */
    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderBuffers;sectionBufferPool()Lnet/minecraft/client/renderer/SectionBufferBuilderPool;"
        )
    )
    private SectionBufferBuilderPool redirectSectionBufferBuilderPool(RenderBuffers instance) {
        if (ClientWorldLoader.getIsCreatingClientWorld()
            && !SodiumInterface.invoker.isSodiumPresent()
        ) {
            int processors = Runtime.getRuntime().availableProcessors();
            int bufferCount = Minecraft.getInstance().is64Bit() ?
                processors : Math.min(processors, 4);
            return SectionBufferBuilderPool.allocate(bufferCount);
        }
        
        return instance.sectionBufferPool();
    }
}
