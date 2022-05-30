package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.coderbot.iris.Iris;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.CoreWorldRenderingPipeline;
import net.coderbot.iris.pipeline.newshader.NewWorldRenderingPipeline;
import net.coderbot.iris.shadows.ShadowRenderTargets;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.compat.IPPortingLibCompat;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.FrontClipping;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.RendererUsingStencil;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_DEPTH_FUNC;
import static org.lwjgl.opengl.GL11.GL_EQUAL;
import static org.lwjgl.opengl.GL11.GL_INCR;
import static org.lwjgl.opengl.GL11.GL_KEEP;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_REPLACE;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;

// Iris now use the vanilla framebuffer's depth texture and support stencil
// So a better portal rendering method for forward-shading shaders is possible
public class ExperimentalIrisPortalRenderer extends PortalRenderer {
    public static final ExperimentalIrisPortalRenderer instance = new ExperimentalIrisPortalRenderer();
    
    public static void init() {
        IPGlobal.clientCleanupSignal.connect(() -> {
        });
    }
    
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
        // TODO switch to a separate buffer source for Iris
        client.renderBuffers().bufferSource().endBatch();
        
        doPortalRendering(matrixStack);
        
        // Resume Iris world rendering
        ((IEIrisNewWorldRenderingPipeline) (Object) Iris.getPipelineManager().getPipeline().get())
            .ip_setIsRenderingWorld(true);
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
        myFinishRendering();
    }
    
    protected void restoreDepthOfPortalViewArea(PortalLike portal, PoseStack matrixStack) {
        client.getMainRenderTarget().bindWrite(false);
        
        setStencilStateForWorldRendering();
        
        int originalDepthFunc = GL11.glGetInteger(GL_DEPTH_FUNC);
        
        GL11.glDepthFunc(GL_ALWAYS);
        
        FrontClipping.disableClipping();
        
        ViewAreaRenderer.renderPortalArea(
            portal, Vec3.ZERO,
            matrixStack.last().pose(),
            RenderSystem.getProjectionMatrix(),
            false,
            false,
            true
        );
        
        GL11.glDepthFunc(originalDepthFunc);
    }
    
    @Override
    public void invokeWorldRendering(WorldRenderInfo worldRenderInfo) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipeline().get();
    
        ShadowMapSwapper.Storage shadowMapCache = null;
        
        if (pipeline instanceof NewWorldRenderingPipeline newWorldRenderingPipeline) {
            ShadowRenderTargets shadowRenderTargets = ((IEIrisNewWorldRenderingPipeline) newWorldRenderingPipeline).ip_getShadowRenderTargets();
            
            if (shadowRenderTargets != null) {
                ShadowMapSwapper shadowMapSwapper = ((IEIrisShadowRenderTargets) shadowRenderTargets).getShadowMapSwapper();
                
                shadowMapCache = shadowMapSwapper.acquireStorage();
                
                if (shadowMapCache != null) {
                    shadowMapCache.copyFromIrisShadowRenderTargets();
                }
            }
        }
        
        SystemTimeUniforms.COUNTER.beginFrame(); // is it necessary?
        super.invokeWorldRendering(worldRenderInfo);
        SystemTimeUniforms.COUNTER.beginFrame(); // make Iris to update the uniforms
        
        if (pipeline instanceof NewWorldRenderingPipeline newWorldRenderingPipeline) {
            // this is important to hand rendering
            newWorldRenderingPipeline.isBeforeTranslucent = true;
        }
        
        // Avoid Iris from force-disabling depth mask
        ((IEIrisNewWorldRenderingPipeline) (Object) pipeline)
            .ip_setIsRenderingWorld(false);
        
        if (shadowMapCache != null) {
            shadowMapCache.copyToIrisShadowRenderTargets();
            shadowMapCache.restitute();
        }
    }
    
    protected void doPortalRendering(PoseStack matrixStack) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        
        client.getProfiler().popPush("render_portal_total");
        renderPortals(matrixStack);
    }
    
    private void myFinishRendering() {
        GL11.glStencilFunc(GL_ALWAYS, 2333, 0xFF);
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        
        GL11.glDisable(GL_STENCIL_TEST);
        GlStateManager._enableDepthTest();
    }
    
    protected void renderPortals(PoseStack matrixStack) {
        // The main depth buffer that Iris use should not contain the depth of portal itself,
        //  otherwise it can't read the correct depth, things may not render normally.
        // However, portals can occlude portals. So we need another framebuffer to hold the
        //  world depth + portal depth
        
        List<PortalLike> portalsToRender = getPortalsToRender(matrixStack);
        List<PortalLike> reallyRenderedPortals = new ArrayList<>();
        
        for (PortalLike portal : portalsToRender) {
            boolean reallyRendered = doRenderPortal(portal, matrixStack);
            
            if (reallyRendered) {
                reallyRenderedPortals.add(portal);
            }
        }
        
        setStencilStateForWorldRendering();
        
        // draw the portal areas again to increase stencil
        // to limit the area of Iris deferred composite rendering
        for (PortalLike reallyRenderedPortal : reallyRenderedPortals) {
            renderPortalViewAreaToStencil(reallyRenderedPortal, matrixStack);
        }
        
        setStencilStateForWorldRendering();
    }
    
    // return true if it really rendered the portal
    private boolean doRenderPortal(
        PortalLike portal,
        PoseStack matrixStack
    ) {
        if (RendererUsingStencil.shouldSkipRenderingInsideFuseViewPortal(portal)) {
            return false;
        }
        
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        
        client.getProfiler().push("render_view_area");
        
        boolean anySamplePassed = PortalRenderInfo.renderAndDecideVisibility(portal, () -> {
            renderPortalViewAreaToStencil(portal, matrixStack);
        });
        
        client.getProfiler().pop();
        
        if (!anySamplePassed) {
            setStencilStateForWorldRendering();
            return false;
        }
        
        PortalRendering.pushPortalLayer(portal);
        
        int thisPortalStencilValue = outerPortalStencilValue + 1;
        
        if (!portal.isFuseView()) {
            client.getProfiler().push("clear_depth_of_view_area");
            clearDepthOfThePortalViewArea(portal);
            client.getProfiler().pop();
        }
        
        setStencilStateForWorldRendering();
        
        renderPortalContent(portal);
        
        if (!portal.isFuseView()) {
            restoreDepthOfPortalViewArea(portal, matrixStack);
        }
        
        clampStencilValue(outerPortalStencilValue);
        
        PortalRendering.popPortalLayer();
        
        return true;
    }
    
    public void onAfterIrisDeferredCompositeRendering() {
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        clampStencilValue(outerPortalStencilValue);
        
        setStencilStateForWorldRendering();
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
        //nothing
    }
    
    private void renderPortalViewAreaToStencil(
        PortalLike portal, PoseStack matrixStack
    ) {
        int outerPortalStencilValue = PortalRendering.getPortalLayer();
        
        //is the mask here different from the mask of glStencilMask?
        GL11.glStencilFunc(GL_EQUAL, outerPortalStencilValue, 0xFF);
        
        //if stencil and depth test pass, the data in stencil buffer will increase by 1
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR);
        //NOTE about GL_INCR:
        //if multiple triangles occupy the same pixel and passed stencil and depth tests,
        //its stencil value will still increase by one
        
        GL11.glStencilMask(0xFF);
        
        // update it before pushing
        FrontClipping.updateInnerClipping(matrixStack);
        
        ViewAreaRenderer.renderPortalArea(
            portal, Vec3.ZERO,
            matrixStack.last().pose(),
            RenderSystem.getProjectionMatrix(),
            true,
            false, // don't modify color
            true
        );
    }
    
    private void clearDepthOfThePortalViewArea(
        PortalLike portal
    ) {
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        
        setStencilStateForWorldRendering();
        
        //do not manipulate color buffer
        GL11.glColorMask(false, false, false, false);
        
        //save the state
        int originalDepthFunc = GL11.glGetInteger(GL_DEPTH_FUNC);
        
        //always passes depth test
        GL11.glDepthFunc(GL_ALWAYS);
        
        //the pixel's depth will be 1, which is the furthest
        GL11.glDepthRange(1, 1);
        
        MyRenderHelper.renderScreenTriangle();
        
        //retrieve the state
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthFunc(originalDepthFunc);
        GL11.glDepthRange(0, 1);
    }
    
    public static void clampStencilValue(
        int maximumValue
    ) {
        GlStateManager._depthMask(true);
        
        //NOTE GL_GREATER means ref > stencil
        //GL_LESS means ref < stencil
        
        //pass if the stencil value is greater than the maximum value
        GL11.glStencilFunc(GL_LESS, maximumValue, 0xFF);
        
        //if stencil test passed, encode the stencil value
        GL11.glStencilOp(GL_KEEP, GL_REPLACE, GL_REPLACE);
        
        //do not manipulate the depth buffer
        GL11.glDepthMask(false);
        
        //do not manipulate the color buffer
        GL11.glColorMask(false, false, false, false);
        
        GlStateManager._disableDepthTest();
        
        MyRenderHelper.renderScreenTriangle();
        
        GL11.glDepthMask(true);
        
        GL11.glColorMask(true, true, true, true);
        
        GlStateManager._enableDepthTest();
    }
    
    private void setStencilStateForWorldRendering() {
        int thisPortalStencilValue = PortalRendering.getPortalLayer();
        
        //draw content in the mask
        GL11.glStencilFunc(GL_EQUAL, thisPortalStencilValue, 0xFF);
        
        //do not manipulate stencil buffer now
        GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
    }
}
