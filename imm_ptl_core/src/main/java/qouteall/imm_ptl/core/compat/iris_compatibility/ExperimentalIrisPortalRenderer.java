package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.RendererUsingStencil;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

public class ExperimentalIrisPortalRenderer extends RendererUsingStencil {
    public static final ExperimentalIrisPortalRenderer instance = new ExperimentalIrisPortalRenderer();
    
    @Override
    public boolean replaceFrameBufferClearing() {
        boolean skipClearing = PortalRendering.isRendering();
        if (skipClearing) {
            // TODO check whether clearing is necessary as sky may override it
        }
        return skipClearing;
    }
    
    @Override
    public void onBeforeTranslucentRendering(PoseStack matrixStack) {
//        doPortalRendering(matrixStack);
    }
    
    @Override
    public void onBeginIrisTranslucentRendering(PoseStack matrixStack) {
        // Iris's buffers are deferred, changing a render layer won't cause it to draw
        // actually I want to flush only solid layers
        client.renderBuffers().bufferSource().endBatch();
        
        doPortalRendering(matrixStack);
    }
    
    @Override
    public void onAfterTranslucentRendering(PoseStack matrixStack) {
    
    }
    
    @Override
    public void onHandRenderingEnded(PoseStack matrixStack) {
        //nothing
    }
    
    @Override
    public void prepareRendering() {
        if (!IPPortingLibCompat.getIsStencilEnabled(client.getMainRenderTarget())) {
            IPPortingLibCompat.setIsStencilEnabled(client.getMainRenderTarget(), true);
        }
        
        client.getMainRenderTarget().bindWrite(false);
        
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        
        GlStateManager._enableDepthTest();
        GL11.glEnable(GL_STENCIL_TEST);
        
    }
    
    @Override
    public void finishRendering() {
        //nothing
    }
    
    @Override
    protected void restoreDepthOfPortalViewArea(PortalLike portal, PoseStack matrixStack) {
        // don't restore depth for now
    }
    
    @Override
    public void invokeWorldRendering(WorldRenderInfo worldRenderInfo) {
        SystemTimeUniforms.COUNTER.beginFrame(); // is it necessary?
        super.invokeWorldRendering(worldRenderInfo);
        SystemTimeUniforms.COUNTER.beginFrame(); // make Iris to update the uniforms
        
        Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> {
            if (pipeline instanceof NewWorldRenderingPipeline newWorldRenderingPipeline) {
                // this is important to hand rendering
                newWorldRenderingPipeline.isBeforeTranslucent = true;
            }
        });
    }
}
