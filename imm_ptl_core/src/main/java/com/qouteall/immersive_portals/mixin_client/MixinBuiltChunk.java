package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinBuiltChunk implements IEBuiltChunk {
    
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
    
    @Override
    public void fullyReset() {
        clear();
    }
}
