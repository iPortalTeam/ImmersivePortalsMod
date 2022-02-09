package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class MixinRenderChunk implements IEBuiltChunk {
    
    private long portal_mark;
    private LevelRenderer.RenderChunkInfo portal_dummyChunkInfo;
    
    @Shadow
    protected abstract void reset();
    
    @Shadow
    @Final
    @Mutable
    public int index;
    
    @Override
    public void portal_fullyReset() {
        reset();
    }
    
    @Override
    public long portal_getMark() {
        return portal_mark;
    }
    
    @Override
    public void portal_setMark(long arg) {
        portal_mark = arg;
    }
    
    @Override
    public void portal_setIndex(int arg) {
        index = arg;
    }
    
    @Override
    public LevelRenderer.RenderChunkInfo portal_getDummyChunkInfo() {
        if (portal_dummyChunkInfo == null) {
            portal_dummyChunkInfo = new LevelRenderer.RenderChunkInfo(
                ((ChunkRenderDispatcher.RenderChunk) (Object) this),
                null, 0
            );
        }
        return portal_dummyChunkInfo;
    }
}
