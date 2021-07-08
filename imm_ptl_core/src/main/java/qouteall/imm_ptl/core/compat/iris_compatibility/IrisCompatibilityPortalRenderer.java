package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.systems.RenderSystem;
import net.coderbot.iris.uniforms.SystemTimeUniforms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ducks.IEFrameBuffer;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.PortalRenderInfo;
import qouteall.imm_ptl.core.render.MyGameRenderer;
import qouteall.imm_ptl.core.render.MyRenderHelper;
import qouteall.imm_ptl.core.render.PortalRenderer;
import qouteall.imm_ptl.core.render.SecondaryFrameBuffer;
import qouteall.imm_ptl.core.render.ViewAreaRenderer;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import static org.lwjgl.opengl.GL11.GL_NEAREST;

public class IrisCompatibilityPortalRenderer extends PortalRenderer {
    
    public static final IrisCompatibilityPortalRenderer instance = new IrisCompatibilityPortalRenderer();
    
    private SecondaryFrameBuffer deferredBuffer = new SecondaryFrameBuffer();
    private MatrixStack modelView = new MatrixStack();
    
    @Override
    public boolean replaceFrameBufferClearing() {
        return false;
    }
    
    @Override
    public void onBeforeTranslucentRendering(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        modelView = new MatrixStack();
        modelView.push();
        modelView.peek().getModel().multiply(matrixStack.peek().getModel());
        modelView.peek().getNormal().multiply(matrixStack.peek().getNormal());
    }
    
    @Override
    public void onAfterTranslucentRendering(MatrixStack matrixStack) {
    
    }
    
    @Override
    public void finishRendering() {
    
    }
    
    @Override
    public void prepareRendering() {
        deferredBuffer.prepare();
        
        deferredBuffer.fb.setClearColor(1, 0, 0, 0);
        deferredBuffer.fb.clear(MinecraftClient.IS_SYSTEM_MAC);
        
        Framebuffer mcFb = MinecraftClient.getInstance().getFramebuffer();
        ((IEFrameBuffer) mcFb).setIsStencilBufferEnabledAndReload(false);
    }
    
    @Override
    protected void doRenderPortal(PortalLike portal, MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            // this renderer only supports one-layer portal
            return;
        }
        
        if (!testShouldRenderPortal(portal, matrixStack)) {
            return;
        }
        
        client.getFramebuffer().beginWrite(true);
        
        PortalRendering.pushPortalLayer(portal);
        
        renderPortalContent(portal);
        
        PortalRendering.popPortalLayer();
        
        client.gameRenderer.loadProjectionMatrix(RenderStates.projectionMatrix);
        
        CHelper.enableDepthClamp();
        
        // draw portal content to the deferred buffer
        deferredBuffer.fb.beginWrite(true);
        MyRenderHelper.drawPortalAreaWithFramebuffer(
            portal,
            client.getFramebuffer(),
            matrixStack.peek().getModel(),
            RenderStates.projectionMatrix
        );
        CHelper.disableDepthClamp();
        
        RenderSystem.colorMask(true, true, true, true);
        
        client.getFramebuffer().beginWrite(true);
    }
    
    @Override
    public void invokeWorldRendering(
        WorldRenderInfo worldRenderInfo
    ) {
        SystemTimeUniforms.COUNTER.beginFrame();
        MyGameRenderer.renderWorldNew(
            worldRenderInfo,
            Runnable::run
        );
        SystemTimeUniforms.COUNTER.beginFrame();
    }
    
    @Override
    public void renderPortalInEntityRenderer(Portal portal) {
    
    }
    
    private boolean testShouldRenderPortal(PortalLike portal, MatrixStack matrixStack) {
        
        //reset projection matrix
        client.gameRenderer.loadProjectionMatrix(RenderStates.projectionMatrix);
        
        deferredBuffer.fb.beginWrite(true);
        
        return PortalRenderInfo.renderAndDecideVisibility(portal, () -> {
            
            ViewAreaRenderer.renderPortalArea(
                portal, Vec3d.ZERO,
                matrixStack.peek().getModel(),
                RenderStates.projectionMatrix,
                true, false, false
            );
        });
    }
    
    @Override
    public void onBeforeHandRendering(MatrixStack matrixStack) {
        if (PortalRendering.isRendering()) {
            return;
        }
        
        CHelper.checkGlError();
        
        // save the main framebuffer to deferredBuffer
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, client.getFramebuffer().fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, deferredBuffer.fb.fbo);
        GL30.glBlitFramebuffer(
            0, 0, deferredBuffer.fb.textureWidth, deferredBuffer.fb.textureHeight,
            0, 0, deferredBuffer.fb.textureWidth, deferredBuffer.fb.textureHeight,
            GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
            GL_NEAREST
        );
        
        CHelper.checkGlError();
        
        renderPortals(modelView);
        modelView.pop();
        
        Framebuffer mainFrameBuffer = client.getFramebuffer();
        mainFrameBuffer.beginWrite(true);
        
        MyRenderHelper.drawScreenFrameBuffer(
            deferredBuffer.fb,
            false,
            false
        );
    }
    
    @Override
    public void onHandRenderingEnded(MatrixStack matrixStack) {
    
    }
}
