package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_FRONT;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glReadPixels;

public class MyRenderHelper {
    
    public static final Minecraft client = Minecraft.getInstance();
    
    private static final Identifier SHADER_PORTAL_AREA =
        Identifier.fromNamespaceAndPath("immersive_portals", "core/portal_area");
    private static final Identifier SHADER_PORTAL_DRAW_FB =
        Identifier.fromNamespaceAndPath("immersive_portals", "core/portal_draw_fb_in_area");
    private static final Identifier SHADER_BLIT_SCREEN_NO_BLEND =
        Identifier.fromNamespaceAndPath("immersive_portals", "core/blit_screen_noblend");
    private static final Identifier SHADER_POSITION_COLOR =
        Identifier.fromNamespaceAndPath("minecraft", "core/position_color");
    
    private static RenderPipeline portalAreaPipelineCull;
    private static RenderPipeline portalAreaPipelineNoCull;
    private static RenderPipeline portalDrawFbPipeline;
    private static RenderPipeline blitScreenNoBlendPipeline;
    private static RenderPipeline blitScreenBlendPipeline;
    private static RenderPipeline positionColorPipeline;
    
    private static final PerspectiveProjectionMatrixBuffer PROJECTION_BUFFER =
        new PerspectiveProjectionMatrixBuffer("imm_ptl_projection");
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1, 1, 1, 1);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    
    private static GpuBuffer portalParamsBuffer;
    private static GpuBufferSlice portalParamsSlice;
    
    public static void init() {
        portalAreaPipelineCull = buildPortalAreaPipeline(true);
        portalAreaPipelineNoCull = buildPortalAreaPipeline(false);
        portalDrawFbPipeline = buildPortalDrawFbPipeline();
        blitScreenNoBlendPipeline = buildBlitScreenPipeline(false);
        blitScreenBlendPipeline = buildBlitScreenPipeline(true);
        positionColorPipeline = buildPositionColorPipeline();
    }
    
    private static RenderPipeline buildPortalAreaPipeline(boolean cull) {
        return RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                "immersive_portals",
                "portal_area_" + (cull ? "cull" : "nocull")
            ))
            .withVertexShader(SHADER_PORTAL_AREA)
            .withFragmentShader(SHADER_PORTAL_AREA)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(cull)
            .withColorWrite(true, true)
            .withDepthWrite(true)
            .withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("IPortalClipping", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .build();
    }
    
    private static RenderPipeline buildPortalDrawFbPipeline() {
        return RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("immersive_portals", "portal_draw_fb_in_area"))
            .withVertexShader(SHADER_PORTAL_DRAW_FB)
            .withFragmentShader(SHADER_PORTAL_DRAW_FB)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withColorWrite(true, true)
            .withDepthWrite(true)
            .withSampler("DiffuseSampler")
            .withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("IPortalClipping", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("IPortalParams", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .build();
    }
    
    private static RenderPipeline buildBlitScreenPipeline(boolean useAlphaBlend) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(
                "immersive_portals",
                useAlphaBlend ? "blit_screen_blend" : "blit_screen_noblend"
            ))
            .withVertexShader(SHADER_BLIT_SCREEN_NO_BLEND)
            .withFragmentShader(SHADER_BLIT_SCREEN_NO_BLEND)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withColorWrite(true, true)
            .withDepthWrite(false)
            .withSampler("DiffuseSampler");
        
        if (useAlphaBlend) {
            BlendFunction blend = new BlendFunction(
                SourceFactor.ONE,
                DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ZERO,
                DestFactor.ONE
            );
            builder = builder.withBlend(blend);
        }
        else {
            builder = builder.withoutBlend();
        }
        
        return builder.build();
    }
    
    private static RenderPipeline buildPositionColorPipeline() {
        return RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("immersive_portals", "position_color"))
            .withVertexShader(SHADER_POSITION_COLOR)
            .withFragmentShader(SHADER_POSITION_COLOR)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withCull(false)
            .withColorWrite(true, true)
            .withDepthWrite(true)
            .withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .build();
    }
    
    public static void drawPortalAreaWithFramebuffer(
        Portal portal,
        RenderTarget textureProvider,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix
    ) {
        
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        GlStateManager._viewport(0, 0, textureProvider.width, textureProvider.height);
        
        MeshData mesh = ViewAreaRenderer.buildPortalViewAreaTrianglesBuffer(
            Vec3.ZERO,//fog
            portal,
            CHelper.getCurrentCameraPos(),
            RenderStates.getPartialTick()
        );
        
        RenderTarget outputTarget = client.getMainRenderTarget();
        try (RenderPass pass = beginRenderPass(outputTarget)) {
            pass.setPipeline(portalDrawFbPipeline);
            bindCommonUniforms(pass, modelViewMatrix, projectionMatrix);
            FrontClipping.bindClippingUniform(pass);
            bindPortalParams(pass, textureProvider.width, textureProvider.height);
            bindDiffuseSampler(pass, textureProvider);
            drawMesh(pass, mesh);
        }
    }
    
    public static void drawPortalAreaMesh(
        MeshData mesh,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        boolean doFaceCulling
    ) {
        RenderTarget outputTarget = client.getMainRenderTarget();
        RenderPipeline pipeline = doFaceCulling ? portalAreaPipelineCull : portalAreaPipelineNoCull;
        
        try (RenderPass pass = beginRenderPass(outputTarget)) {
            pass.setPipeline(pipeline);
            bindCommonUniforms(pass, modelViewMatrix, projectionMatrix);
            FrontClipping.bindClippingUniform(pass);
            drawMesh(pass, mesh);
        }
    }
    
    public static void renderScreenTriangle() {
        renderScreenTriangle(255, 255, 255, 255);
    }
    
    public static void renderScreenTriangle(Vec3 color) {
        renderScreenTriangle(
            (int) (color.x * 255),
            (int) (color.y * 255),
            (int) (color.z * 255),
            255
        );
    }
    
    public static void testOneTriangle(int r, int g, int b, int a) {
        MeshData mesh = buildScreenTriangleMesh(r, g, b, a);
        drawPositionColorMesh(mesh, new Matrix4f().identity(), new Matrix4f().identity(), client.getMainRenderTarget());
    }
    
    /**
     * {@link RenderTarget#blitToScreen()}
     */
    @IPVanillaCopy
    public static void renderScreenTriangle(int r, int g, int b, int a) {
        MeshData mesh = buildScreenTriangleMesh(r, g, b, a);
        drawPositionColorMesh(mesh, new Matrix4f().identity(), new Matrix4f().identity(), client.getMainRenderTarget());
    }
    
    private static MeshData buildScreenTriangleMesh(int r, int g, int b, int a) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator
            .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        
        bufferBuilder.addVertex(1, -1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(1, 1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1, 1, 0).setColor(r, g, b, a);
        
        bufferBuilder.addVertex(-1, 1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(-1, -1, 0).setColor(r, g, b, a);
        bufferBuilder.addVertex(1, -1, 0).setColor(r, g, b, a);
        
        return bufferBuilder.build();
    }
    
    /**
     * {@link RenderTarget#blitToScreen()}
     */
    public static void drawScreenFrameBuffer(
        RenderTarget textureProvider,
        boolean doUseAlphaBlend,
        boolean doEnableModifyAlpha
    ) {
        int x = 0;
        int y = 0;
        
        int viewportWidth = textureProvider.width;
        int viewportHeight = textureProvider.height;
        
        drawFramebufferWithCoordinatesAndDimensions(
            textureProvider, doUseAlphaBlend, doEnableModifyAlpha,
            x, y, viewportWidth, viewportHeight
        );
    }
    
    public static void drawFramebuffer(
        RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        float xMin, float xMax, float yMin, float yMax
    ) {
        drawFramebufferWithCoordinatesAndDimensions(
            textureProvider,
            doUseAlphaBlend, doEnableModifyAlpha,
            0, 0,
            client.getWindow().getWidth(),
            client.getWindow().getHeight()
        );
    }
    
    public static void drawFramebufferWithViewport(
        RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        float left, float right, float bottom, float up,
        int viewportWidth, int viewportHeight
    ) {
        drawFramebufferWithCoordinatesAndDimensions(
            textureProvider,
            doUseAlphaBlend, doEnableModifyAlpha,
            0, 0,
            viewportWidth, viewportHeight
        );
    }
    
    public static void drawFramebufferWithBounds(
        RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        int xMin, int xMax, int yMin, int yMax
    ) {
        drawFramebufferWithCoordinatesAndDimensions(
            textureProvider,
            doUseAlphaBlend, doEnableModifyAlpha,
            xMin, yMin,
            Mth.abs(xMax - xMin),
            Mth.abs(yMax - yMin)
        );
    }
    
    /**
     * {@link RenderTarget#blitToScreen()}
     */
    @IPVanillaCopy
    public static void drawFramebufferWithCoordinatesAndDimensions(
        RenderTarget textureProvider, boolean doUseAlphaBlend, boolean doEnableModifyAlpha,
        int x, int y, int viewportWidth, int viewportHeight
    ) {
        CHelper.checkGlError();
        
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._viewport(
            x, textureProvider.height - viewportHeight - y,
            viewportWidth, viewportHeight
        );
        
        if (doUseAlphaBlend) {
            GlStateManager._enableBlend();
        }
        else {
            GlStateManager._disableBlend();
        }
        
        if (doEnableModifyAlpha) {
            GlStateManager._colorMask(true, true, true, true);
        }
        else {
            GlStateManager._colorMask(true, true, true, false);
        }
        
        RenderPipeline pipeline = doUseAlphaBlend ? blitScreenBlendPipeline : blitScreenNoBlendPipeline;
        MeshData mesh = buildBlitMesh();
        
        RenderTarget outputTarget = client.getMainRenderTarget();
        try (RenderPass pass = beginRenderPass(outputTarget)) {
            pass.setPipeline(pipeline);
            bindDiffuseSampler(pass, textureProvider);
            drawMesh(pass, mesh);
        }
        
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        
        CHelper.checkGlError();
    }
    
    private static MeshData buildBlitMesh() {
        BufferBuilder bufferBuilder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        bufferBuilder.addVertex(0.0f, 0.0f, 0.0f).setUv(0.0f, 0.0f);
        bufferBuilder.addVertex(1.0f, 0.0f, 0.0f).setUv(1.0f, 0.0f);
        bufferBuilder.addVertex(1.0f, 1.0f, 0.0f).setUv(1.0f, 1.0f);
        bufferBuilder.addVertex(0.0f, 1.0f, 0.0f).setUv(0.0f, 1.0f);
        
        return bufferBuilder.buildOrThrow();
    }
    
    // it will remove the light sections that are marked to be removed
    // if not, light data will cause minor memory leak
    // and wrongly remove the light data when the chunks get reloaded to client
    // this should not run before world rendering or the smooth lighting may become abnormal in section edge
    public static void lateUpdateLight() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        
        ClientWorldLoader.getClientWorlds().forEach(world -> {
            if (!RenderStates.isDimensionRendered(world.dimension())) {
                world.getChunkSource().getLightEngine().runLightUpdates();
            }
        });
    }
    
    /**
     * If we don't do this
     * the future created in {@link SectionRenderDispatcher#uploadSectionLayer}
     * may never complete
     */
    public static void earlyRemoteUpload() {
        if (!ClientWorldLoader.getIsInitialized()) {
            return;
        }
        
        ClientWorldLoader.WORLD_RENDERER_MAP.forEach((dim, worldRenderer) -> {
            if (client.level.dimension() != dim) {
                worldRenderer.getSectionRenderDispatcher().uploadAllPendingUploads();
            }
        });
    }
    
    public static void applyMirrorFaceCulling() {
        glCullFace(GL_FRONT);
    }
    
    public static void recoverFaceCulling() {
        glCullFace(GL_BACK);
    }
    
    public static void clearAlphaTo1(RenderTarget target) {
        GlStateManager._colorMask(false, false, false, true);
        MeshData mesh = buildScreenTriangleMesh(0, 0, 0, 255);
        drawPositionColorMesh(mesh, new Matrix4f().identity(), new Matrix4f().identity(), target);
        GlStateManager._colorMask(true, true, true, true);
    }
    
    public static void restoreViewPort() {
        Minecraft client = Minecraft.getInstance();
        GlStateManager._viewport(
            0,
            0,
            client.getWindow().getWidth(),
            client.getWindow().getHeight()
        );
    }
    
    public static float transformFogDistance(float value) {
        if (!WorldRenderInfo.isFogEnabled()) {
            return value * 23333;
        }
        
        // just disable fog for fuse-view portals for now
        if (PortalRendering.isRendering()) {
            Portal renderingPortal = PortalRendering.getRenderingPortal();
            
            if (renderingPortal.isFuseView()) {
                return value * 23333;
            }
        }
        
        // as non-fuse-view portals does not apply scale transformation to modelview,
        // there is no need to transform fog distance (both with and without sodium)
        
        return value;
    }
    
    private static boolean debugEnabled = false;
    
    public static void debugFramebufferDepth() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        
        int width = client.getMainRenderTarget().width;
        int height = client.getMainRenderTarget().height;
        
        
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
    
    public static void debugFramebufferColorRed() {
        if (!debugEnabled) {
            return;
        }
        debugEnabled = false;
        
        int width = client.getMainRenderTarget().width;
        int height = client.getMainRenderTarget().height;
        
        
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        
        FloatBuffer floatBuffer = directBuffer.asFloatBuffer();
        
        glReadPixels(
            0, 0, width, height,
            GL_RED, GL_FLOAT, floatBuffer
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
    
    public static void clearRenderTarget(RenderTarget target, float r, float g, float b, float a, boolean clearDepth) {
        int color = ARGB.colorFromFloat(a, r, g, b);
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTexture colorTexture = target.getColorTexture();
        if (clearDepth && target.getDepthTexture() != null) {
            encoder.clearColorAndDepthTextures(colorTexture, color, target.getDepthTexture(), 1.0);
        }
        else {
            encoder.clearColorTexture(colorTexture, color);
        }
    }
    
    private static RenderPass beginRenderPass(RenderTarget target) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuTextureView colorView = target.getColorTextureView();
        if (colorView == null) {
            throw new IllegalStateException("RenderTarget has no color texture view");
        }
        GpuTextureView depthView = target.getDepthTextureView();
        if (depthView != null) {
            return encoder.createRenderPass(
                () -> "imm_ptl_render_pass",
                colorView,
                OptionalInt.empty(),
                depthView,
                OptionalDouble.empty()
            );
        }
        return encoder.createRenderPass(
            () -> "imm_ptl_render_pass",
            colorView,
            OptionalInt.empty()
        );
    }
    
    private static void bindCommonUniforms(RenderPass pass, Matrix4f modelView, Matrix4f projection) {
        GpuBufferSlice projectionSlice = PROJECTION_BUFFER.getBuffer(projection);
        GpuBufferSlice transformSlice = RenderSystem.getDynamicUniforms().writeTransform(
            modelView,
            COLOR_MODULATOR,
            MODEL_OFFSET,
            new Matrix4f().identity()
        );
        pass.setUniform("Projection", projectionSlice);
        pass.setUniform("DynamicTransforms", transformSlice);
    }
    
    private static void bindDiffuseSampler(RenderPass pass, RenderTarget textureProvider) {
        GpuTextureView colorView = textureProvider.getColorTextureView();
        if (colorView == null) {
            return;
        }
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        pass.bindTexture("DiffuseSampler", colorView, sampler);
    }
    
    private static void bindPortalParams(RenderPass pass, int width, int height) {
        ensurePortalParamsBuffer();
        ByteBuffer buffer = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        Std140Builder.intoBuffer(buffer).putVec4(width, height, 0.0f, 0.0f);
        buffer.flip();
        
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(portalParamsSlice, buffer);
        pass.setUniform("IPortalParams", portalParamsSlice);
    }
    
    private static void ensurePortalParamsBuffer() {
        if (portalParamsBuffer != null && !portalParamsBuffer.isClosed()) {
            return;
        }
        
        int alignment = RenderSystem.getDevice().getUniformOffsetAlignment();
        int size = 16;
        int alignedSize = ((size + alignment - 1) / alignment) * alignment;
        
        portalParamsBuffer = RenderSystem.getDevice().createBuffer(
            () -> "imm_ptl_portal_params",
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
            alignedSize
        );
        portalParamsSlice = portalParamsBuffer.slice(0, size);
    }
    
    private static void drawPositionColorMesh(
        MeshData mesh, Matrix4f modelView, Matrix4f projection, RenderTarget target
    ) {
        try (RenderPass pass = beginRenderPass(target)) {
            pass.setPipeline(positionColorPipeline);
            bindCommonUniforms(pass, modelView, projection);
            drawMesh(pass, mesh);
        }
    }
    
    private static void drawMesh(RenderPass pass, MeshData mesh) {
        if (mesh == null) {
            return;
        }
        
        GpuBuffer vertexBuffer = null;
        GpuBuffer indexBuffer = null;
        boolean shouldCloseIndex = false;
        try {
            vertexBuffer = mesh.drawState().format().uploadImmediateVertexBuffer(mesh.vertexBuffer());
            
            if (mesh.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer auto = RenderSystem.getSequentialBuffer(mesh.drawState().mode());
                indexBuffer = auto.getBuffer(mesh.drawState().indexCount());
                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer, auto.type());
                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
            else {
                indexBuffer = mesh.drawState().format().uploadImmediateIndexBuffer(mesh.indexBuffer());
                shouldCloseIndex = true;
                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer, mesh.drawState().indexType());
                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
        }
        finally {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }
            if (indexBuffer != null && shouldCloseIndex) {
                indexBuffer.close();
            }
            mesh.close();
        }
    }
}
