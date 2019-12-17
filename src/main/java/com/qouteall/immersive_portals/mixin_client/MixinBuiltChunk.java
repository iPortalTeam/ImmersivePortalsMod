package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinBuiltChunk implements IEBuiltChunk {
    
    @Shadow
    protected abstract void clear();
    
    @Override
    public void fullyReset() {
        clear();
    }
}
