package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinBuiltChunk implements IEBuiltChunk {
    
    private long portal_mark;
    private WorldRenderer.ChunkInfo portal_dummyChunkInfo;
    
    @Shadow
    protected abstract void clear();
    
    @Shadow
    @Final
    @Mutable
    public int index;
    
    @Override
    public void portal_fullyReset() {
        clear();
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
    public WorldRenderer.ChunkInfo portal_getDummyChunkInfo() {
        if (portal_dummyChunkInfo == null) {
            portal_dummyChunkInfo = new WorldRenderer.ChunkInfo(
                ((ChunkBuilder.BuiltChunk) (Object) this),
                null, 0
            );
        }
        return portal_dummyChunkInfo;
    }
}
