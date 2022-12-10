package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.compat.iris_compatibility.IrisInterface;
import qouteall.imm_ptl.core.compat.sodium_compatibility.SodiumInterface;
import qouteall.imm_ptl.core.miscellaneous.IPVanillaCopy;
import qouteall.imm_ptl.core.portal.PortalLike;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.BreakablePortalEntity;
import qouteall.imm_ptl.core.render.context_management.RenderStates;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class OverlayRendering {
    private static final RandomSource random = RandomSource.create();
    
    
    public static boolean shouldRenderOverlay(PortalLike portal) {
        if (portal instanceof BreakablePortalEntity breakablePortalEntity) {
            if (breakablePortalEntity.getActualOverlay() != null) {
                return breakablePortalEntity.isInFrontOfPortal(CHelper.getCurrentCameraPos());
            }
        }
        return false;
    }
    
    private static boolean shaderOverlayWarned = false;
    
    public static void onRenderPortalEntity(
        PortalLike portal,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        if (IrisInterface.invoker.isShaders()) {
            if (!shaderOverlayWarned) {
                shaderOverlayWarned = true;
                CHelper.printChat("[Immersive Portals] Portal overlay cannot be rendered with shaders");
            }
            
            return;
        }
        
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
     * {@link net.minecraft.client.renderer.entity.FallingBlockRenderer}
     */
    private static void renderBreakablePortalOverlay(
        BreakablePortalEntity portal,
        float tickDelta,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    ) {
        BreakablePortalEntity.OverlayInfo overlay = portal.getActualOverlay();
        BlockState blockState = overlay.blockState();
        
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
        
        Vec3 offset = portal.getNormal().scale(overlay.offset());
        
        Vec3 pos = portal.position();
        
        matrixStack.translate(offset.x, offset.y, offset.z);
        
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
            
            if (overlay.rotation() != null) {
                matrixStack.mulPose(overlay.rotation().toMcQuaternion());
            }
            
            for (BakedQuad quad : quads) {
                SodiumInterface.invoker.markSpriteActive(quad.getSprite());
                renderQuad(
                    buffer,
                    matrixStack.last(),
                    quad,
                    new float[]{1.0F, 1.0F, 1.0F, 1.0F},
                    1.0f, 1.0f, 1.0f,
                    new int[]{14680304, 14680304, 14680304, 14680304},//packed light value
                    OverlayTexture.NO_OVERLAY,
                    true,
                    ((float) overlay.opacity())
                );
            }
            
            matrixStack.popPose();
        }
        
        matrixStack.popPose();
        
    }
    
    /**
     * {@link VertexConsumer#putBulkData(PoseStack.Pose, BakedQuad, float, float, float, int, int)}
     * it hardcoded alpha to 1. change it to my customized alpha
     */
    @IPVanillaCopy
    public static void renderQuad(
        VertexConsumer vertexConsumer,
        PoseStack.Pose poseEntry, BakedQuad quad,
        float[] colorMuls, float red, float green, float blue,
        int[] combinedLights, int combinedOverlay, boolean mulColor,
        float alpha
    ) {
        float[] fs = new float[]{colorMuls[0], colorMuls[1], colorMuls[2], colorMuls[3]};
        int[] is = new int[]{combinedLights[0], combinedLights[1], combinedLights[2], combinedLights[3]};
        int[] js = quad.getVertices();
        Vec3i vec3i = quad.getDirection().getNormal();
        Vector3f vector3f = new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ());
        Matrix4f matrix4f = poseEntry.pose();
        poseEntry.normal().transform(vector3f);
        int i = 8;
        int j = js.length / 8;
        
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            
            for (int k = 0; k < j; ++k) {
                intBuffer.clear();
                intBuffer.put(js, k * 8, 8);
                float f = byteBuffer.getFloat(0);
                float g = byteBuffer.getFloat(4);
                float h = byteBuffer.getFloat(8);
                float o;
                float p;
                float q;
                if (mulColor) {
                    float l = (float) (byteBuffer.get(12) & 255) / 255.0F;
                    float m = (float) (byteBuffer.get(13) & 255) / 255.0F;
                    float n = (float) (byteBuffer.get(14) & 255) / 255.0F;
                    o = l * fs[k] * red;
                    p = m * fs[k] * green;
                    q = n * fs[k] * blue;
                }
                else {
                    o = fs[k] * red;
                    p = fs[k] * green;
                    q = fs[k] * blue;
                }
                
                int r = is[k];
                float m = byteBuffer.getFloat(16);
                float n = byteBuffer.getFloat(20);
                Vector4f vector4f = new Vector4f(f, g, h, 1.0F);
                matrix4f.transform(vector4f);
                vertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), o, p, q, alpha, m, n, combinedOverlay, r, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }
}
