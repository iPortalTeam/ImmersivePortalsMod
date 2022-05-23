package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class OverlayRendering {
    private static final Random random = new Random();
    
    public static boolean test = false;
    
    public static class PortalOverlayRenderLayer extends RenderType {
        
        public PortalOverlayRenderLayer() {
            super(
                "imm_ptl_portal_overlay",
                RenderType.translucentMovingBlock().format(),
                RenderType.translucentMovingBlock().mode(),
                RenderType.translucentMovingBlock().bufferSize(),
                RenderType.translucentMovingBlock().affectsCrumbling(),
                true,
                () -> {
                    RenderType.translucentMovingBlock().setupRenderState();
//                    RenderSystem.enableBlend();
//                    RenderSystem.color4f(0,0,1,1);
                },
                () -> {
                    RenderType.translucentMovingBlock().clearRenderState();
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
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
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
    
    public static List<BakedQuad> getQuads(BakedModel model, BlockState blockState, Vec3 portalNormal) {
        Direction facing = Direction.getNearest(portalNormal.x, portalNormal.y, portalNormal.z);
        
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
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        BlockState blockState = portal.overlayBlockState;
        
        Vec3 cameraPos = CHelper.getCurrentCameraPos();
        
        if (blockState == null) {
            return;
        }
        
        BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderer();
        
        BlockPortalShape blockPortalShape = portal.blockPortalShape;
        if (blockPortalShape == null) {
            return;
        }
        
        matrixStack.pushPose();
        
        Vec3 offset = portal.getNormal().scale(portal.overlayOffset);
        
        Vec3 pos = portal.position();
        
        matrixStack.translate(
             offset.x,
             offset.y,
             offset.z
        );
        
        BakedModel model = blockRenderManager.getBlockModel(blockState);
        RenderType renderLayer = Sheets.translucentCullBlockSheet();
        VertexConsumer buffer = vertexConsumerProvider.getBuffer(renderLayer);
        
        List<BakedQuad> quads = getQuads(model, blockState, portal.getNormal());
        
        random.setSeed(0);
        
        for (BlockPos blockPos : blockPortalShape.area) {
            matrixStack.pushPose();
            matrixStack.translate(
                blockPos.getX() - pos.x, blockPos.getY() - pos.y, blockPos.getZ() - pos.z
            );
            
            for (BakedQuad quad : quads) {
                renderQuad(
                    buffer,
                    matrixStack.last(),
                    quad,
                    0.6f,
                    1.0f, 1.0f, 1.0f,
                    14680304,//packed light value
                    OverlayTexture.NO_OVERLAY,
                    true,
                    ((float) portal.overlayOpacity)
                );
            }
            
            matrixStack.popPose();
        }
        
        matrixStack.popPose();
        
    }
    
    /**
     * vanilla copy
     * vanilla block model rendering does not support customizing alpha
     * and glColor() also doesn't work
     * {@link net.minecraft.client.render.VertexConsumer#quad(net.minecraft.client.util.math.MatrixStack.Entry, net.minecraft.client.render.model.BakedQuad, float[], float, float, float, int[], int, boolean)}
     */
    public static void renderQuad(
        VertexConsumer vertexConsumer,
        PoseStack.Pose matrixEntry, BakedQuad quad,
        float brightness, float red, float green, float blue,
        int lights, int overlay, boolean useQuadColorData,
        float alpha
    ) {
        int[] is = quad.getVertices();
        Vec3i vec3i = quad.getDirection().getNormal();
        Vector3f vector3f = new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());
        Matrix4f matrix4f = matrixEntry.pose();
        vector3f.transform(matrixEntry.normal());

        int j = is.length / 8;
        MemoryStack memoryStack = MemoryStack.stackPush();
        Throwable var17 = null;

        try {
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
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
                vertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), r, s, t, alpha, v, w, overlay, u, vector3f.x(), vector3f.y(), vector3f.z());
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
