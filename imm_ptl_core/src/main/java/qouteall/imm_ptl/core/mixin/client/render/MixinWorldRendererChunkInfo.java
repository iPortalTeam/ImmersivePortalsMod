package qouteall.imm_ptl.core.mixin.client.render;

import qouteall.imm_ptl.core.ducks.IEWorldRendererChunkInfo;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public class MixinWorldRendererChunkInfo implements IEWorldRendererChunkInfo {
    @Shadow
    @Final
    private ChunkBuilder.BuiltChunk chunk;
    
    @Override
    public ChunkBuilder.BuiltChunk getBuiltChunk() {
        return chunk;
    }
}
