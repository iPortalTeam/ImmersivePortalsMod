package qouteall.imm_ptl.core.mixin.client.render.optimization;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SectionRenderDispatcher.RenderSection.CompileTask.class)
public interface IEChunkCompileTask {
    @Accessor("isCancelled")
    AtomicBoolean getIsCancelled();
}
