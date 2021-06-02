package com.qouteall.immersive_portals.mixin.client.render;

import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
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
    
    @Inject(
        method = "needsImportantRebuild",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onNeedsImportantRebuild(CallbackInfoReturnable<Boolean> cir) {
        if (PortalRendering.isRendering()) {
            cir.setReturnValue(false);
        }
    }
    
    @Shadow
    protected abstract void clear();
    
    @Shadow
    @Final
    @Mutable
    public int index;
    
    @Override
    public void fullyReset() {
        clear();
    }
    
    @Override
    public long getMark() {
        return portal_mark;
    }
    
    @Override
    public void setMark(long arg) {
        portal_mark = arg;
    }
    
    @Override
    public void setIndex(int arg) {
        index = arg;
    }
}
