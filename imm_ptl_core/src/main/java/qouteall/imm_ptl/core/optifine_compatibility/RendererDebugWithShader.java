package qouteall.imm_ptl.core.optifine_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL13;

public class RendererDebugWithShader extends PortalRenderer {
    SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
        renderPortals(matrixStack);
    }
    
    @Override
    public void prepareRendering() {
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
        
        OFGlobal.bindToShaderFrameBuffer.run();
        
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    protected void doRenderPortal(PortalLike portal, MatrixStack matrixStack) {
        if (RenderStates.getRenderedPortalNum() >= 1) {
            return;
        }
    
        PortalRendering.pushPortalLayer(portal);
        
        renderPortalContent(portal);
        //it will bind the gbuffer of rendered dimension
    
        PortalRendering.popPortalLayer();
        
        deferredBuffer.fb.beginWrite(true);
        
        GlStateManager._activeTexture(GL13.GL_TEXTURE0);
        client.getFramebuffer().draw(
            deferredBuffer.fb.viewportWidth,
            deferredBuffer.fb.viewportHeight
        );

        OFGlobal.bindToShaderFrameBuffer.run();
    }
    
    @Override
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            runnable -> {
                OFGlobal.shaderContextManager.switchContextAndRun(()->{
                    OFGlobal.bindToShaderFrameBuffer.run();
                    runnable.run();
                });
            }
        );
    }
    
    @Override
    public void onHandRenderingEnded(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        if (RenderStates.getRenderedPortalNum() == 0) {
            return;
        }
    
        throw new RuntimeException();
//        GlStateManager.enableAlphaTest();
//        Framebuffer mainFrameBuffer = client.getFramebuffer();
//        mainFrameBuffer.beginWrite(true);
//
//        deferredBuffer.fb.draw(mainFrameBuffer.viewportWidth, mainFrameBuffer.viewportHeight);
    }
}
