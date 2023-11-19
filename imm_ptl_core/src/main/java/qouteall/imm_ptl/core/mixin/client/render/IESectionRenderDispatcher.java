package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionRenderDispatcher.class)
public interface IESectionRenderDispatcher {
    @Accessor("fixedBuffers")
    SectionBufferBuilderPack ip_getFixedBuffers();
    
    @Mutable
    @Accessor("fixedBuffers")
    void ip_setFixedBuffers(SectionBufferBuilderPack arg);
}
