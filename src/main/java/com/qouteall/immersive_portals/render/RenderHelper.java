package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.immersive_portals.*;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.portal.Mirror;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlFramebuffer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Untracker;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.*;

public class RenderHelper {
    //switching context is really bug-prone
    public static DimensionType originalPlayerDimension;
    public static Vec3d originalPlayerPos;
    public static Vec3d originalPlayerLastTickPos;
    public static GameMode originalGameMode;
    public static float partialTicks = 0;
    
    private static Set<DimensionType> renderedDimensions = new HashSet<>();
    public static List<List<WeakReference<Portal>>> lastPortalRenderInfos = new ArrayList<>();
    private static List<List<WeakReference<Portal>>> portalRenderInfos = new ArrayList<>();
    
    public static Vec3d lastCameraPos = Vec3d.ZERO;
    public static Vec3d cameraPosDelta = Vec3d.ZERO;
    
    public static void updatePreRenderInfo(
        float partialTicks_
    ) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        
        if (cameraEntity == null) {
            return;
        }
        
        RenderHelper.originalPlayerDimension = cameraEntity.dimension;
        RenderHelper.originalPlayerPos = cameraEntity.getPos();
        RenderHelper.originalPlayerLastTickPos = McHelper.lastTickPosOf(cameraEntity);
        PlayerListEntry entry = CHelper.getClientPlayerListEntry();
        RenderHelper.originalGameMode = entry != null ? entry.getGameMode() : GameMode.CREATIVE;
        partialTicks = partialTicks_;
        
        renderedDimensions.clear();
        lastPortalRenderInfos = portalRenderInfos;
        portalRenderInfos = new ArrayList<>();
    }
    
    public static void onTotalRenderEnd() {
        MinecraftClient mc = MinecraftClient.getInstance();
        CGlobal.clientWorldLoader
            .getDimensionRenderHelper(mc.world.dimension.getType())
            .switchToMe();
    
        if (getRenderedPortalNum() != 0) {
            //recover chunk renderer dispatcher
            ((IEWorldRenderer) mc.worldRenderer).getChunkRenderDispatcher().updateCameraPosition(
                mc.cameraEntity.x,
                mc.cameraEntity.z
            );
        }
        
        Vec3d currCameraPos = mc.gameRenderer.getCamera().getPos();
        cameraPosDelta = currCameraPos.subtract(lastCameraPos);
        if (cameraPosDelta.lengthSquared() > 1) {
            cameraPosDelta = Vec3d.ZERO;
        }
        lastCameraPos = currCameraPos;
    }
    
    public static int getRenderedPortalNum() {
        return portalRenderInfos.size();
    }
    
    public static boolean isDimensionRendered(DimensionType dimensionType) {
        return renderedDimensions.contains(dimensionType);
    }
    
    public static void onBeginPortalWorldRendering(Stack<Portal> portalLayers) {
        List<WeakReference<Portal>> currRenderInfo = portalLayers.stream().map(
            (Function<Portal, WeakReference<Portal>>) WeakReference::new
        ).collect(Collectors.toList());
        portalRenderInfos.add(currRenderInfo);
        renderedDimensions.add(portalLayers.peek().dimensionTo);
    }
    
    public static void setupCameraTransformation() {
        MinecraftClient mc = MinecraftClient.getInstance();
        //rendering post processing shader may change view port
        if (OFInterface.isShaders.getAsBoolean()) {
            //Shaders.setViewport(0, 0, mc.window.getFramebufferWidth(), mc.window.getFramebufferHeight());
        }
        else {
            GlStateManager.viewport(
                0,
                0,
                mc.window.getFramebufferWidth(),
                mc.window.getFramebufferHeight()
            );
        }
        
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
    
    public static void drawFrameBufferUp(
        Portal portal,
        GlFramebuffer textureProvider,
        ShaderManager shaderManager
    ) {
        Helper.checkGlError();
        
        setupCameraTransformation();
        
        shaderManager.loadContentShaderAndShaderVars(0);
    
        if (OFInterface.isShaders.getAsBoolean()) {
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
    
        ViewAreaRenderer.drawPortalViewTriangle(portal);
        
        shaderManager.unloadShader();
    
        OFInterface.resetViewport.run();
    
        Helper.checkGlError();
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
    
    /**
     * {@link GlFramebuffer#draw(int, int)}
     */
    public static void myDrawFrameBuffer(
        GlFramebuffer textureProvider,
        boolean doEnableAlphaTest,
        boolean doEnableModifyAlpha
    ) {
        Helper.checkGlError();
        
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
    
        Helper.checkGlError();
    }
    
    //If I don't do so JVM will crash
    private static final FloatBuffer matrixBuffer = (FloatBuffer) GLX.make(MemoryUtil.memAllocFloat(
        16), (p_209238_0_) -> {
        Untracker.untrack(MemoryUtil.memAddress(p_209238_0_));
    });
    
    public static void multMatrix(float[] arr) {
        matrixBuffer.put(arr);
        matrixBuffer.rewind();
        GlStateManager.multMatrix(matrixBuffer);
    }
    
    public static boolean isRenderingMirror() {
        return CGlobal.renderer.isRendering() &&
            CGlobal.renderer.getRenderingPortal() instanceof Mirror;
    }
    
    public static void setupTransformationForMirror(Camera camera) {
        if (CGlobal.renderer.isRendering()) {
            Portal renderingPortal = CGlobal.renderer.getRenderingPortal();
            if (renderingPortal instanceof Mirror) {
                Mirror mirror = (Mirror) renderingPortal;
                Vec3d relativePos = mirror.getPos().subtract(camera.getPos());
                
                GlStateManager.translated(relativePos.x, relativePos.y, relativePos.z);
                
                float[] arr = getMirrorTransformation(mirror.getNormal());
                multMatrix(arr);
                
                GlStateManager.translated(-relativePos.x, -relativePos.y, -relativePos.z);
    
                //GlStateManager.cullFace(GlStateManager.FaceSides.FRONT);
            }
            else {
                //GlStateManager.cullFace(GlStateManager.FaceSides.BACK);
            }
        }
        else {
            //GlStateManager.cullFace(GlStateManager.FaceSides.BACK);
        }
    }
    
    
    //https://en.wikipedia.org/wiki/Householder_transformation
    private static float[] getMirrorTransformation(
        Vec3d mirrorNormal
    ) {
        Vec3d normal = mirrorNormal.normalize();
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
        return new float[]{
            1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
            0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
            0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
            0, 0, 0, 1
        };
    }
}
