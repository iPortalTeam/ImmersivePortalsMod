package com.qouteall.immersive_portals.mixin_client;

import com.qouteall.immersive_portals.exposer.IEWorldRendererChunkInfo;
import net.minecraft.client.render.chunk.ChunkRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer$ChunkInfo")
public class MixinWorldRendererChunkInfo implements IEWorldRendererChunkInfo {
    
    @Shadow
    @Final
    private ChunkRenderer renderer;
    
    @Override
    public ChunkRenderer getChunkRenderer() {
        return renderer;
    }
}
