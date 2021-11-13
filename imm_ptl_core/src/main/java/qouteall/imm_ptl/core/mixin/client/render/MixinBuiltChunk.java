package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
