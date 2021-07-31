package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.UnpooledDirectByteBuf;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.q_misc_util.my_util.SignalBiArged;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.Validate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glReadPixels;

public class MyRenderHelper {
    
    public static final MinecraftClient client = MinecraftClient.getInstance();
    
    public static final SignalBiArged<ResourceManager, Consumer<Shader>> loadShaderSignal =
        new SignalBiArged<>();
    
    public static void init() {
        
        loadShaderSignal.connect((resourceManager, resultConsumer) -> {
            try {
                DrawFbInAreaShader shader = new DrawFbInAreaShader(
                    getResourceFactory(resourceManager),
                    "portal_draw_fb_in_area",
                    VertexFormats.POSITION_COLOR
                );
                resultConsumer.accept(shader);
                drawFbInAreaShader = shader;
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        
        loadShaderSignal.connect((resourceManager, resultConsumer) -> {
            try {
                Shader shader = new Shader(
                    getResourceFactory(resourceManager),
                    "portal_area",
                    VertexFormats.POSITION_COLOR
                );
                resultConsumer.accept(shader);
                portalAreaShader = shader;
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        
        loadShaderSignal.connect((resourceManager, resultConsumer) -> {
            try {
                Shader shader = new Shader(
                    getResourceFactory(resourceManager),
                    "blit_screen_noblend",
                    VertexFormats.POSITION_TEXTURE_COLOR
                );
                resultConsumer.accept(shader);
                blitScreenNoBlendShader = shader;
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    private static ResourceFactory getResourceFactory(ResourceManager resourceManager) {
        ResourceFactory resourceFactory = new ResourceFactory() {
            @Override
            public Resource getResource(Identifier id) throws IOException {
                Identifier corrected = new Identifier("immersive_portals", id.getPath());
                return resourceManager.getResource(corrected);
            }
        };
        return resourceFactory;
    }
    
    public static class DrawFbInAreaShader extends Shader {
        
        public final GlUniform uniformW;
        public final GlUniform uniformH;
        
        public DrawFbInAreaShader(
            ResourceFactory factory, String name, VertexFormat format
        ) throws IOException {
            super(factory, name, format);
            
            uniformW = getUniform("w");
            uniformH = getUniform("h");
        }
        
        void loadWidthHeight(int w, int h) {
            uniformW.set((float) w);
            uniformH.set((float) h);
        }
    }
    
    public static DrawFbInAreaShader drawFbInAreaShader;
    public static Shader portalAreaShader;
    public static Shader blitScreenNoBlendShader;
    
    public static void drawPortalAreaWithFramebuffer(
        PortalLike portal,
        Framebuffer textureProvider,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix
    ) {
        
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GlStateManager._viewport(0, 0, textureProvider.textureWidth, textureProvider.textureHeight);
        
        DrawFbInAreaShader shader = drawFbInAreaShader;
        shader.addSampler("DiffuseSampler", textureProvider.getColorAttachment());
        shader.loadWidthHeight(textureProvider.textureWidth, textureProvider.textureHeight);
        
        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(modelViewMatrix);
        }
        
        if (shader.projectionMat != null) {
            shader.projectionMat.set(projectionMatrix);
        }
        
        shader.upload();
        
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(
            Vec3d.ZERO,//fog
            portal,
            bufferBuilder,
            CHelper.getCurrentCameraPos(),
            RenderStates.tickDelta
        );
        
        BufferRenderer.postDraw(bufferBuilder);
        
        // wrong name. unbind
        shader.bind();
    }
    
    public static void renderScreenTriangle() {
        renderScreenTriangle(255, 255, 255, 255);
    }
    
    public static void renderScreenTriangle(Vec3d color) {
        renderScreenTriangle(
            (int) (color.x * 255),
            (int) (color.y * 255),
            (int) (color.z * 255),
            255
        );
    }
    
    public static void renderScreenTriangle(int r, int g, int b, int a) {
        Shader shader = GameRenderer.getPositionColorShader();
        Validate.notNull(shader);
        
        Matrix4f identityMatrix = new Matrix4f();
        identityMatrix.loadIdentity();
        
        shader.modelViewMat.set(identityMatrix);
        shader.projectionMat.set(identityMatrix);
        
        shader.upload();
        
        RenderSystem.disableTexture();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        bufferBuilder.vertex(1, -1, 0).color(r, g, b, a)
            .next();
        bufferBuilder.vertex(1, 1, 0).color(r, g, b, a)
            .next();
        bufferBuilder.vertex(-1, 1, 0).color(r, g, b, a)
            .next();
        
        bufferBuilder.vertex(-1, 1, 0).color(r, g, b, a)
            .next();
        bufferBuilder.vertex(-1, -1, 0).color(r, g, b, a)
            .next();
        bufferBuilder.vertex(1, -1, 0).color(r, g, b, a)
            .next();
        
        bufferBuilder.end();
        
        BufferRenderer.postDraw(bufferBuilder);
        
        // wrong name. unbind
        shader.bind();
        
        RenderSystem.enableTexture();
    }
    
    /**
     * {@link Framebuffer#draw(int, int)}
     */
    public static void drawScreenFrameBuffer(
        Framebuffer textureProvider,
        boolean doUseAlphaBlend,
        boolean doEnableModifyAlpha
    ) {
        float right = (float) textureProvider.viewportWidth;
        float up = (float) textureProvider.viewportHeight;
        float left = 0;
        float bottom = 0;
        
        int viewportWidth = textureProvider.viewportWidth;
        int viewportHeight = textureProvider.viewportHeight;
        
        drawFramebufferWithViewport(
            textureProvider, doUseAlphaBlend, doEnableModifyAlpha,
            left, (double) right, bottom, (double) up,
            viewportWidth, viewportHeight
        );
    }
    
    public static void drawFramebuffer(
        Framebuffer textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        float left, double right, float bottom, double up
    ) {
        drawFramebufferWithViewport(
            textureProvider,
            doUseAlphaBlend, doEnableModifyAlpha,
            left, right, bottom, up,
            client.getWindow().getFramebufferWidth(),
            client.getWindow().getFramebufferHeight()
        );
    }
    
    public static void drawFramebufferWithViewport(
        Framebuffer textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        float left, double right, float bottom, double up,
        int viewportWidth, int viewportHeight
    ) {
        CHelper.checkGlError();
        
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(0, 0, viewportWidth, viewportHeight);
        
        if (doUseAlphaBlend) {
            RenderSystem.enableBlend();
        }
        else {
            RenderSystem.disableBlend();
        }
        
        if (doEnableModifyAlpha) {
            GlStateManager._colorMask(true, true, true, true);
        }
        else {
            GlStateManager._colorMask(true, true, true, false);
        }
        
        Shader shader = doUseAlphaBlend ? client.gameRenderer.blitScreenShader : blitScreenNoBlendShader;
        
        shader.addSampler("DiffuseSampler", textureProvider.getColorAttachment());
        
        Matrix4f projectionMatrix = Matrix4f.projectionMatrix(
            (float) viewportWidth, (float) (-viewportHeight), 1000.0F, 3000.0F);
        
        shader.modelViewMat.set(Matrix4f.translate(0.0F, 0.0F, -2000.0F));
        
        shader.projectionMat.set(projectionMatrix);
        
        shader.upload();
        
        float textureXScale = (float) viewportWidth / (float) textureProvider.textureWidth;
        float textureYScale = (float) viewportHeight / (float) textureProvider.textureHeight;
        
        Tessellator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        bufferBuilder.vertex(left, up, 0.0D)
            .texture(0.0F, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(right, up, 0.0D)
            .texture(textureXScale, 0.0F)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(right, bottom, 0.0D)
            .texture(textureXScale, textureYScale)
            .color(255, 255, 255, 255).next();
        bufferBuilder.vertex(left, bottom, 0.0D)
            .texture(0.0F, textureYScale)
            .color(255, 255, 255, 255).next();
        
        bufferBuilder.end();
        BufferRenderer.postDraw(bufferBuilder);
        
        // unbind
        shader.bind();
        
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        
        RenderSystem.enableBlend();
        
        CHelper.checkGlError();
    }
    
    // it will remove the light sections that are marked to be removed
    // if not, light data will cause minor memory leak
    // and wrongly remove the light data when the chunks get reloaded to client
    public static void earlyUpdateLight() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        
        ClientWorldLoader.getClientWorlds().forEach(world -> {
            if (world != MinecraftClient.getInstance().world) {
                int updateNum = world.getChunkManager().getLightingProvider().doLightUpdates(
                    1000, true, true
                );
            }
        });
    }
    
    public static void applyMirrorFaceCulling() {
        glCullFace(GL_FRONT);
    }
    
    public static void recoverFaceCulling() {
        glCullFace(GL_BACK);
    }
    
    public static void clearAlphaTo1(Framebuffer mcFrameBuffer) {
        mcFrameBuffer.beginWrite(true);
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(0, 0, 0, 1.0f);
        RenderSystem.clear(GL_COLOR_BUFFER_BIT, true);
        RenderSystem.colorMask(true, true, true, true);
    }
    
    public static void restoreViewPort() {
        MinecraftClient client = MinecraftClient.getInstance();
        GlStateManager._viewport(
            0,
            0,
            client.getWindow().getFramebufferWidth(),
            client.getWindow().getFramebufferHeight()
        );
    }
    
    public static float transformFogDistance(float value) {
        if (IPGlobal.debugDisableFog) {
            return 233333;
        }
        
        if (PortalRendering.isRendering()) {
            PortalLike renderingPortal = PortalRendering.getRenderingPortal();
            if (PortalRenderer.shouldApplyScaleToModelView(renderingPortal)) {
                double scaling = renderingPortal.getScale();
                float result = (float) (value / scaling);
                if (scaling > 10) {
                    result *= 10;
                }
                return result;
            }
        }
        return value;
    }
    
    private static boolean debugEnabled = false;
    
    public static void debugFramebufferDepth() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        
        int width = client.getFramebuffer().textureWidth;
        int height = client.getFramebuffer().textureHeight;
        
        
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        
        FloatBuffer floatBuffer = directBuffer.asFloatBuffer();
        
        glReadPixels(
            0, 0, width, height,
            GL_DEPTH_COMPONENT, GL_FLOAT, floatBuffer
        );
        
        float[] data = new float[width * height];
        
        floatBuffer.rewind();
        floatBuffer.get(data);
        
        float maxValue = (float) IntStream.range(0, data.length)
            .mapToDouble(i -> data[i]).max().getAsDouble();
        float minValue = (float) IntStream.range(0, data.length)
            .mapToDouble(i -> data[i]).min().getAsDouble();
        
        byte[] grayData = new byte[width * height];
        for (int i = 0; i < data.length; i++) {
            float datum = data[i];
            
            datum = (datum - minValue) / (maxValue - minValue);
            
            grayData[i] = (byte) (datum * 255);
        }
        
        BufferedImage bufferedImage =
            new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        bufferedImage.setData(
            Raster.createRaster(
                bufferedImage.getSampleModel(),
                new DataBufferByte(grayData, grayData.length), new Point()
            )
        );
        
        System.out.println("oops");
    }
}
