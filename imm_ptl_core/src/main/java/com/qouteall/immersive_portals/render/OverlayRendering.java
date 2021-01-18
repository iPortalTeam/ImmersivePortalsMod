package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.portal.PortalLike;
import com.qouteall.immersive_portals.portal.nether_portal.BlockPortalShape;
import com.qouteall.immersive_portals.portal.nether_portal.BreakablePortalEntity;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class OverlayRendering {
    private static final Random random = new Random();
    
    public static boolean test = false;
    
    private static final VertexConsumerProvider.Immediate vertexConsumerProvider =
        VertexConsumerProvider.immediate(new BufferBuilder(256));
    
    public static class PortalOverlayRenderLayer extends RenderLayer {
        
        public PortalOverlayRenderLayer() {
            super(
                "imm_ptl_portal_overlay",
                RenderLayer.getTranslucentMovingBlock().getVertexFormat(),
                RenderLayer.getTranslucentMovingBlock().getDrawMode(),
                RenderLayer.getTranslucentMovingBlock().getExpectedBufferSize(),
                RenderLayer.getTranslucentMovingBlock().hasCrumbling(),
                true,
                () -> {
                    RenderLayer.getTranslucentMovingBlock().startDrawing();
//                    RenderSystem.enableBlend();
//                    RenderSystem.color4f(0,0,1,1);
                },
                () -> {
                    RenderLayer.getTranslucentMovingBlock().endDrawing();
                }
            );
        }
    }
    
    public static final PortalOverlayRenderLayer portalOverlayRenderLayer = new PortalOverlayRenderLayer();
    
    public static boolean shouldRenderOverlay(PortalLike portal) {
        if (portal instanceof BreakablePortalEntity) {
            if (((BreakablePortalEntity) portal).overlayBlockState != null) {
                return true;
            }
        }
        return false;
    }
    
    public static void onPortalRendered(
        PortalLike portal,
        MatrixStack matrixStack
    ) {
        if (portal instanceof BreakablePortalEntity) {
            renderBreakablePortalOverlay(
                ((BreakablePortalEntity) portal),
                RenderStates.tickDelta,
                matrixStack,
                vertexConsumerProvider
            );
        }
    }
    
    public static List<BakedQuad> getQuads(BakedModel model, BlockState blockState, Vec3d portalNormal) {
        Direction facing = Direction.getFacing(portalNormal.x, portalNormal.y, portalNormal.z);
        
        List<BakedQuad> result = new ArrayList<>();
        
        result.addAll(model.getQuads(blockState, facing, random));
        
        result.addAll(model.getQuads(blockState, null, random));
        
        if (result.isEmpty()) {
            for (Direction direction : Direction.values()) {
                result.addAll(model.getQuads(blockState, direction, random));
            }
        }
        
        return result;
    }
    
    /**
     * {@link net.minecraft.client.render.entity.FallingBlockEntityRenderer}
     */
    private static void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        MatrixStack matrixStack,
        VertexConsumerProvider.Immediate vertexConsumerProvider
    ) {
        BlockState blockState = portal.overlayBlockState;
        
        Vec3d cameraPos = CHelper.getCurrentCameraPos();
        
        if (blockState == null) {
            return;
        }
        
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        
        BlockPortalShape blockPortalShape = portal.blockPortalShape;
        if (blockPortalShape == null) {
            return;
        }
        
        matrixStack.push();
        
        Vec3d offset = portal.getNormal().multiply(portal.overlayOffset);
        
        Vec3d pos = portal.getPos();
        
        matrixStack.translate(
            pos.x - cameraPos.x + offset.x,
            pos.y - cameraPos.y + offset.y,
            pos.z - cameraPos.z + offset.z
        );
        
        BakedModel model = blockRenderManager.getModel(blockState);
        PortalOverlayRenderLayer renderLayer = OverlayRendering.portalOverlayRenderLayer;
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(renderLayer);
        
        List<BakedQuad> quads = getQuads(model, blockState, portal.getNormal());
        
        random.setSeed(0);
        
        for (BlockPos blockPos : blockPortalShape.area) {
            matrixStack.push();
            matrixStack.translate(
                blockPos.getX() - pos.x, blockPos.getY() - pos.y, blockPos.getZ() - pos.z
            );
            
            for (BakedQuad quad : quads) {
                renderQuad(
                    buffer,
                    matrixStack.peek(),
                    quad,
                    0.6f,
                    1.0f, 1.0f, 1.0f,
                    14680304,//packed light value
                    OverlayTexture.DEFAULT_UV,
                    true,
                    ((float) portal.overlayOpacity)
                );
            }
            
            matrixStack.pop();
        }
        
        matrixStack.pop();
        
        vertexConsumerProvider.draw(renderLayer);
        
    }
    
    /**
     * vanilla copy
     * vanilla block model rendering does not support customizing alpha
     * and glColor() also doesn't work
     * {@link net.minecraft.client.render.VertexConsumer#quad(net.minecraft.client.util.math.MatrixStack.Entry, net.minecraft.client.render.model.BakedQuad, float[], float, float, float, int[], int, boolean)}
     */
    public static void renderQuad(
        VertexConsumer vertexConsumer,
        MatrixStack.Entry matrixEntry, BakedQuad quad,
        float brightness, float red, float green, float blue,
        int lights, int overlay, boolean useQuadColorData,
        float alpha
    ) {
        int[] is = quad.getVertexData();
        Vec3i vec3i = quad.getFace().getVector();
        Vector3f vector3f = new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());
        Matrix4f matrix4f = matrixEntry.getModel();
        vector3f.transform(matrixEntry.getNormal());
        
        int j = is.length / 8;
        MemoryStack memoryStack = MemoryStack.stackPush();
        Throwable var17 = null;
        
        try {
            ByteBuffer byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            
            for (int k = 0; k < j; ++k) {
                intBuffer.clear();
                intBuffer.put(is, k * 8, 8);
                float f = byteBuffer.getFloat(0);
                float g = byteBuffer.getFloat(4);
                float h = byteBuffer.getFloat(8);
                float r;
                float s;
                float t;
                float v;
                float w;
                if (useQuadColorData) {
                    float l = (float) (byteBuffer.get(12) & 255) / 255.0F;
                    v = (float) (byteBuffer.get(13) & 255) / 255.0F;
                    w = (float) (byteBuffer.get(14) & 255) / 255.0F;
                    r = l * brightness * red;
                    s = v * brightness * green;
                    t = w * brightness * blue;
                }
                else {
                    r = brightness * red;
                    s = brightness * green;
                    t = brightness * blue;
                }
                
                int u = lights;
                v = byteBuffer.getFloat(16);
                w = byteBuffer.getFloat(20);
                Vector4f vector4f = new Vector4f(f, g, h, 1.0F);
                vector4f.transform(matrix4f);
                vertexConsumer.vertex(vector4f.getX(), vector4f.getY(), vector4f.getZ(), r, s, t, alpha, v, w, overlay, u, vector3f.getX(), vector3f.getY(), vector3f.getZ());
            }
        }
        catch (Throwable var38) {
            var17 = var38;
            throw var38;
        }
        finally {
            if (memoryStack != null) {
                if (var17 != null) {
                    try {
                        memoryStack.close();
                    }
                    catch (Throwable var37) {
                        var17.addSuppressed(var37);
                    }
                }
                else {
                    memoryStack.close();
                }
            }
            
        }
    }
}
