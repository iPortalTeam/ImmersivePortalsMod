package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.exposer.IEGameRenderer;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import com.qouteall.immersive_portals.optifine_compatibility.OFHelper;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;

public class RenderHelper {
    public static DimensionType originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameMode originalGameMode;
    public static float partialTicks = 0;
    
    public static int renderedPortalNum = 0;
    public static int lastRenderedPortalNum = 0;
    
    public static void updateRenderInfo(
        float partialTicks_
    ) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        RenderHelper.originalPlayerDimension = cameraEntity.dimension;
        RenderHelper.originalPlayerPos = cameraEntity.getPos();
        RenderHelper.originalPlayerLastTickPos = Helper.lastTickPosOf(cameraEntity);
        PlayerListEntry entry = CHelper.getClientPlayerListEntry();
        RenderHelper.originalGameMode = entry != null ? entry.getGameMode() : GameMode.CREATIVE;
        partialTicks = partialTicks_;
    
        lastRenderedPortalNum = renderedPortalNum;
        renderedPortalNum = 0;
    }
    
    public static void setupCameraTransformation() {
        ((IEGameRenderer) PortalRenderer.mc.gameRenderer).applyCameraTransformations_(partialTicks);
        Camera camera = PortalRenderer.mc.gameRenderer.getCamera();
        camera.update(
            PortalRenderer.mc.world,
            (Entity) (PortalRenderer.mc.getCameraEntity() == null ? PortalRenderer.mc.player : PortalRenderer.mc.getCameraEntity()),
            PortalRenderer.mc.options.perspective > 0,
            PortalRenderer.mc.options.perspective == 2,
            partialTicks
        );
        
    }
    
    //it will render a box instead of a quad
    public static void drawPortalViewTriangle(Portal portal) {
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(portal.dimensionTo);
        
        Vec3d fogColor = helper.getFogColor();
        
        GlStateManager.disableCull();
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableBlend();
        GlStateManager.disableLighting();
        
        GL11.glDisable(GL_CLIP_PLANE0);
        
        if (OFHelper.getIsUsingShader()) {
            fogColor = Vec3d.ZERO;
        }
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(
            fogColor,
            portal,
            bufferbuilder,
            PortalRenderer.mc.gameRenderer.getCamera().getPos(),
            partialTicks
        );
        
        tessellator.draw();
        
        GlStateManager.enableCull();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
        GlStateManager.enableLighting();
    }
    
    public static void drawFrameBufferUp(
        Portal portal,
        GlFramebuffer textureProvider,
        ShaderManager shaderManager
    ) {
        setupCameraTransformation();
        
        shaderManager.loadContentShaderAndShaderVars(0);
        
        if (OFHelper.getIsUsingShader()) {
            GlStateManager.viewport(
                0,
                0,
                PortalRenderer.mc.getFramebuffer().viewWidth,
                PortalRenderer.mc.getFramebuffer().viewHeight
            );
        }
        
        GlStateManager.enableTexture();
        
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
        
        GlStateManager.bindTexture(textureProvider.colorAttachment);
        GlStateManager.texParameter(3553, 10241, 9729);
        GlStateManager.texParameter(3553, 10240, 9729);
        GlStateManager.texParameter(3553, 10242, 10496);
        GlStateManager.texParameter(3553, 10243, 10496);
        
        drawPortalViewTriangle(portal);
        
        shaderManager.unloadShader();
        
        if (OFHelper.getIsUsingShader()) {
            GlStateManager.viewport(0, 0, Shaders.renderWidth, Shaders.renderHeight);
        }
    }
    
    static void renderScreenTriangle() {
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        
        GlStateManager.disableAlphaTest();
        GlStateManager.disableTexture();
        
        GlStateManager.shadeModel(GL_SMOOTH);
        
        GL20.glUseProgram(0);
        GL11.glDisable(GL_CLIP_PLANE0);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBufferBuilder();
        bufferbuilder.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR);
        
        bufferbuilder.vertex(1, -1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(1, 1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(-1, 1, 0).color(255, 255, 255, 255)
            .next();
        
        bufferbuilder.vertex(-1, 1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(-1, -1, 0).color(255, 255, 255, 255)
            .next();
        bufferbuilder.vertex(1, -1, 0).color(255, 255, 255, 255)
            .next();
        
        tessellator.draw();
        
        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.popMatrix();
        
        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.popMatrix();
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }
    
    public static void copyFromShaderFbTo(GlFramebuffer destFb, int copyComponent) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OFGlobal.getDfb.get());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destFb.fbo);
        
        GL30.glBlitFramebuffer(
            0, 0, Shaders.renderWidth, Shaders.renderHeight,
            0, 0, destFb.viewWidth, destFb.viewHeight,
            copyComponent, GL_NEAREST
        );
        
        OFHelper.bindToShaderFrameBuffer();
    }
    
    /**
     * {@link GlFramebuffer#draw(int, int)}
     */
    public static void myDrawFrameBuffer(
        GlFramebuffer textureProvider,
        boolean doEnableAlphaTest,
        boolean doEnableModifyAlpha
    ) {
        Validate.isTrue(GLX.isUsingFBOs());
        
        if (doEnableModifyAlpha) {
            GlStateManager.colorMask(true, true, true, true);
        }
        else {
            GlStateManager.colorMask(true, true, true, false);
        }
        GlStateManager.disableDepthTest();
        GlStateManager.depthMask(false);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(
            0.0D,
            (double) textureProvider.viewWidth,
            (double) textureProvider.viewHeight,
            0.0D,
            1000.0D,
            3000.0D
        );
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
        GlStateManager.viewport(0, 0, textureProvider.viewWidth, textureProvider.viewHeight);
        GlStateManager.enableTexture();
        GlStateManager.disableLighting();
        if (doEnableAlphaTest) {
            GlStateManager.enableAlphaTest();
        }
        else {
            GlStateManager.disableAlphaTest();
        }
        
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        textureProvider.beginRead();
        float float_1 = (float) textureProvider.viewWidth;
        float float_2 = (float) textureProvider.viewHeight;
        float float_3 = (float) textureProvider.viewWidth / (float) textureProvider.texWidth;
        float float_4 = (float) textureProvider.viewHeight / (float) textureProvider.texHeight;
        Tessellator tessellator_1 = Tessellator.getInstance();
        BufferBuilder bufferBuilder_1 = tessellator_1.getBufferBuilder();
        bufferBuilder_1.begin(7, VertexFormats.POSITION_UV_COLOR);
        bufferBuilder_1.vertex(0.0D, (double) float_2, 0.0D).texture(0.0D, 0.0D).color(
            255,
            255,
            255,
            255
        ).next();
        bufferBuilder_1.vertex((double) float_1, (double) float_2, 0.0D).texture(
            (double) float_3,
            0.0D
        ).color(255, 255, 255, 255).next();
        bufferBuilder_1.vertex((double) float_1, 0.0D, 0.0D).texture(
            (double) float_3,
            (double) float_4
        ).color(255, 255, 255, 255).next();
        bufferBuilder_1.vertex(0.0D, 0.0D, 0.0D).texture(0.0D, (double) float_4).color(
            255,
            255,
            255,
            255
        ).next();
        tessellator_1.draw();
        textureProvider.endRead();
        GlStateManager.depthMask(true);
        GlStateManager.colorMask(true, true, true, true);
    }
}
