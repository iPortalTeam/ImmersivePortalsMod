package qouteall.imm_ptl.core.mixin.client.render;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import qouteall.imm_ptl.core.ducks.IEBuiltChunk;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class MixinRenderChunk implements IEBuiltChunk {
    
    private long portal_mark;
    
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
    
}
