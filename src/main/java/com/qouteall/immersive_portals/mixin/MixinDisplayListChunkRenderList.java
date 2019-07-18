package com.qouteall.immersive_portals.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.Globals;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.render.chunk.DisplayListChunkRenderer;
import net.minecraft.client.render.chunk.DisplayListChunkRendererList;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Iterator;

@Mixin(DisplayListChunkRendererList.class)
public class MixinDisplayListChunkRenderList extends ChunkRendererList {
    
    /**
     * @author
     */
    @Overwrite
    public void render(BlockRenderLayer blockRenderLayer_1) {
        if (this.isCameraPositionSet) {
            Iterator var2 = this.chunkRenderers.iterator();
            
            while (var2.hasNext()) {
                ChunkRenderer chunkRenderer_1 = (ChunkRenderer) var2.next();
                DisplayListChunkRenderer displayListChunkRenderer_1 = (DisplayListChunkRenderer) chunkRenderer_1;
                GlStateManager.pushMatrix();
                this.translateToOrigin(chunkRenderer_1);
                
                Globals.shaderManager.loadShaderIfRenderingPortal(
                    new Vec3d(chunkRenderer_1.getOrigin())
                );
                
                GlStateManager.callList(displayListChunkRenderer_1.method_3639(
                    blockRenderLayer_1,
                    displayListChunkRenderer_1.getData()
                ));
                
                Globals.shaderManager.unloadShader();
                
                GlStateManager.popMatrix();
            }
            
            GlStateManager.clearCurrentColor();
            this.chunkRenderers.clear();
        }
    }
    
}
