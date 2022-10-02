package qouteall.imm_ptl.core.ducks;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

public interface IEWorldRenderer {
    EntityRenderDispatcher ip_getEntityRenderDispatcher();
    
    ViewArea ip_getBuiltChunkStorage();
    
    ChunkRenderDispatcher getChunkBuilder();
    
    void ip_myRenderEntity(
        Entity entity,
        double cameraX,
        double cameraY,
        double cameraZ,
        float tickDelta,
        PoseStack matrixStack,
        MultiBufferSource vertexConsumerProvider
    );
    
    PostChain portal_getTransparencyShader();
    
    void portal_setTransparencyShader(PostChain arg);
    
    RenderBuffers ip_getRenderBuffers();
    
    void ip_setRenderBuffers(RenderBuffers arg);
    
    Frustum portal_getFrustum();
    
    void portal_setFrustum(Frustum arg);
    
    void portal_fullyDispose();
    
    void portal_setChunkInfoList(ObjectArrayList<LevelRenderer.RenderChunkInfo> arg);
    
    ObjectArrayList<LevelRenderer.RenderChunkInfo> portal_getChunkInfoList();
}
