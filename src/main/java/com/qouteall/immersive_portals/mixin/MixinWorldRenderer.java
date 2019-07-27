package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEWorldRenderer;
import com.qouteall.immersive_portals.render.MyViewFrustum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.ChunkRenderDispatcher;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRendererFactory;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements IEWorldRenderer {
    @Shadow
    private ClientWorld world;
    
    @Shadow
    private ChunkRendererFactory chunkRendererFactory;
    
    @Shadow
    private ChunkRenderDispatcher chunkRenderDispatcher;
    
    @Shadow
    private ChunkRendererList chunkRendererList;
    
    @Shadow
    private List chunkInfos;
    
    @Inject(
        method = "Lnet/minecraft/client/render/WorldRenderer;reload()V",
        at = @At("TAIL")
    )
    private void onReloaded(CallbackInfo ci) {
        if (chunkRenderDispatcher != null) {
            if (!(chunkRenderDispatcher instanceof MyViewFrustum)) {
                chunkRenderDispatcher = new MyViewFrustum(
                    this.world,
                    MinecraftClient.getInstance().options.viewDistance,
                    (WorldRenderer) (Object) this,
                    this.chunkRendererFactory,
                    chunkRenderDispatcher
                );
            }
        }
    }
    
    @Override
    public ChunkRenderDispatcher getChunkRenderDispatcher() {
        return chunkRenderDispatcher;
    }
    
    @Override
    public ChunkRendererList getChunkRenderList() {
        return chunkRendererList;
    }
    
    @Override
    public List getChunkInfos() {
        return chunkInfos;
    }
    
    @Override
    public void setChunkInfos(List list) {
        chunkInfos = list;
    }
}
