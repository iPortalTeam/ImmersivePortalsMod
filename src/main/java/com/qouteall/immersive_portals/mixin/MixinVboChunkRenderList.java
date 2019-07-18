package com.qouteall.immersive_portals.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.Globals;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.gl.GlBuffer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.chunk.ChunkRendererList;
import net.minecraft.client.render.chunk.VboChunkRendererList;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(VboChunkRendererList.class)
public abstract class MixinVboChunkRenderList extends ChunkRendererList {
    
    /**
     * @author qouteall
     */
    @Overwrite
    public void render(BlockRenderLayer blockRenderLayer_1) {
        if (this.isCameraPositionSet) {
            Iterator var2 = this.chunkRenderers.iterator();
            
            while (var2.hasNext()) {
                ChunkRenderer chunkRenderer_1 = (ChunkRenderer) var2.next();
                GlBuffer glBuffer_1 = chunkRenderer_1.getGlBuffer(blockRenderLayer_1.ordinal());
                GlStateManager.pushMatrix();
                this.translateToOrigin(chunkRenderer_1);
                glBuffer_1.bind();
                this.method_1356();
                
                Globals.shaderManager.loadShaderIfRenderingPortal(
                    new Vec3d(chunkRenderer_1.getOrigin())
                );
                
                glBuffer_1.draw(7);
                
                Globals.shaderManager.unloadShader();
                
                GlStateManager.popMatrix();
            }
            
            GlBuffer.unbind();
            GlStateManager.clearCurrentColor();
            this.chunkRenderers.clear();
        }
    }
    
    @Shadow
    public abstract void method_1356();
}
